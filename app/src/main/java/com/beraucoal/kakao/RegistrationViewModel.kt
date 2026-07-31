package com.beraucoal.kakao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beraucoal.kakao.data.KebunLocation
import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.PetaniRegistration
import com.beraucoal.kakao.data.VerificationStatus
import com.beraucoal.kakao.network.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
}

class RegistrationViewModel(
    private val repository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _registration = MutableStateFlow(PetaniRegistration())
    val registration: StateFlow<PetaniRegistration> = _registration.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var otpRequestId: String? = null

    fun onKtpScanned(ktp: KtpData) {
        _registration.update { it.copy(ktpData = ktp) }
    }

    fun updateKtpField(update: (KtpData) -> KtpData) {
        _registration.update { it.copy(ktpData = update(it.ktpData)) }
    }

    fun onWhatsappNumberChanged(nomor: String) {
        _registration.update { it.copy(nomorWhatsapp = nomor) }
    }

    /** Submit ke NocoBase lalu poll status verifikasi admin. */
    fun submitForVerification(onDone: () -> Unit) {
        val state = _registration.value
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val submitResult = repository.submitPetani(state.ktpData, state.nomorWhatsapp)
            submitResult.onSuccess { petaniId ->
                _registration.update { it.copy(petaniId = petaniId) }
                val statusResult = repository.pollVerificationStatus(petaniId)
                statusResult.onSuccess { status ->
                    _registration.update { it.copy(verificationStatus = status) }
                    _uiState.value = UiState.Idle
                    if (status == VerificationStatus.DISETUJUI) onDone()
                }.onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Gagal cek status verifikasi")
                }
            }.onFailure { e ->
                _uiState.value = UiState.Error(e.message ?: "Gagal mengirim data petani")
            }
        }
    }

    fun sendOtp(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.sendOtp(_registration.value.nomorWhatsapp)
                .onSuccess { requestId ->
                    otpRequestId = requestId
                    _uiState.value = UiState.Idle
                    onDone()
                }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Gagal kirim OTP") }
        }
    }

    fun verifyOtp(kode: String, onValid: () -> Unit) {
        val requestId = otpRequestId ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.verifyOtp(requestId, kode)
                .onSuccess { valid ->
                    _uiState.value = UiState.Idle
                    if (valid) {
                        _registration.update { it.copy(otpVerified = true) }
                        onValid()
                    } else {
                        _uiState.value = UiState.Error("Kode OTP salah, coba lagi")
                    }
                }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Gagal verifikasi OTP") }
        }
    }

    fun submitKebun(lat: Double, lng: Double, luasHektar: Double?, onDone: () -> Unit) {
        val petaniId = _registration.value.petaniId ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.submitKebun(petaniId, lat, lng, luasHektar)
                .onSuccess {
                    _registration.update {
                        it.copy(kebunLocation = KebunLocation(lat, lng, luasHektar))
                    }
                    _uiState.value = UiState.Idle
                    onDone()
                }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Gagal simpan kebun") }
        }
    }

    fun recheckVerificationStatus(onDone: () -> Unit) {
        val petaniId = _registration.value.petaniId ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.pollVerificationStatus(petaniId)
                .onSuccess { status ->
                    _registration.update { it.copy(verificationStatus = status) }
                    _uiState.value = UiState.Idle
                    if (status == VerificationStatus.DISETUJUI) onDone()
                }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Gagal cek status verifikasi") }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
