package com.beraucoal.kakao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beraucoal.kakao.data.KebunLocation
import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.OcrConfidence
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

    /**
     * Melacak langkah maksimum yang sah yang telah berhasil dilalui/dikuasai pengguna.
     * Mencegah lompat-lompat bebas di Stepper sebelum validasi selesai (Validation Gating).
     * 1 = Scan KTP, 2 = Verify Data, 3 = OTP WhatsApp, 4 = Mapping Kebun
     */
    private val _maxStepReached = MutableStateFlow(1)
    val maxStepReached: StateFlow<Int> = _maxStepReached.asStateFlow()

    private var otpRequestId: String? = null

    fun onKtpScanned(ktp: KtpData) {
        _registration.update { it.copy(ktpData = ktp) }
        if (_maxStepReached.value < 2) {
            _maxStepReached.value = 2
        }
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
                    if (status == VerificationStatus.DISETUJUI) {
                        if (_maxStepReached.value < 3) {
                            _maxStepReached.value = 3
                        }
                        onDone()
                    }
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
                        if (_maxStepReached.value < 4) {
                            _maxStepReached.value = 4
                        }
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
                    if (status == VerificationStatus.DISETUJUI) {
                        if (_maxStepReached.value < 3) {
                            _maxStepReached.value = 3
                        }
                        onDone()
                    }
                }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Gagal cek status verifikasi") }
        }
    }

    /**
     * Alur Sign In (Masuk Akun Petani Terdaftar), berinteraksi dengan repositori untuk
     * memperoleh status rekaman asli atau fallback ke keamanan pengujian offline.
     */
    fun signIn(nomorAtauId: String, kataSandi: String, onSuccess: () -> Unit) {
        if (nomorAtauId.isBlank()) {
            _uiState.value = UiState.Error("Nomor WhatsApp atau ID Petani wajib diisi")
            return
        }
        if (kataSandi.isBlank()) {
            _uiState.value = UiState.Error("Kata sandi tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.signInPetani(nomorAtauId, kataSandi)
                .onSuccess { record ->
                    val mockKtp = KtpData(
                        nik = record.nik ?: if (nomorAtauId.length == 16) nomorAtauId else "6403000000000001",
                        nama = record.nama ?: "Budi Kakao Berau",
                        tempatTanggalLahir = "Berau, 12 Agustus 1980",
                        alamat = record.alamat ?: "Jl. Perkebunan Kakao No. 8",
                        kelurahanDesa = record.kelurahanDesa ?: "Bedungun",
                        kecamatan = record.kecamatan ?: "Tanjung Redeb",
                        ocrConfidence = OcrConfidence.HIGH
                    )
                    _registration.update {
                        PetaniRegistration(
                            ktpData = mockKtp,
                            nomorWhatsapp = record.nomorWhatsapp ?: (if (nomorAtauId.startsWith("08") || nomorAtauId.startsWith("+62") || nomorAtauId.startsWith("62")) nomorAtauId else "081234567890"),
                            verificationStatus = VerificationStatus.DISETUJUI,
                            otpVerified = true,
                            petaniId = record.id.toString()
                        )
                    }
                    _maxStepReached.value = 4
                    _uiState.value = UiState.Idle
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Gagal masuk, periksa jaringan Anda")
                }
        }
    }

    /**
     * Mereset seluruh state pendaftaran agar bersih ketika operator/petani kembali ke menu awal
     * atau ingin mendaftarkan petani baru paska selesainya satu alur registrasi / sesi Sign In.
     */
    fun reset() {
        _registration.value = PetaniRegistration()
        _uiState.value = UiState.Idle
        _maxStepReached.value = 1
        otpRequestId = null
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
