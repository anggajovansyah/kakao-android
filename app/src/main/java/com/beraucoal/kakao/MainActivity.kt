package com.beraucoal.kakao

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // ── Diagnostik Google Play Services & Maps SDK ──
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode == ConnectionResult.SUCCESS) {
            Log.i("KakaoMaps", "Google Play Services: AVAILABLE & UP-TO-DATE")
        } else {
            Log.e("KakaoMaps", "Google Play Services: NOT AVAILABLE (code=$resultCode, msg=${googleApiAvailability.getErrorString(resultCode)})")
            // Tampilkan dialog system jika Google Play Services perlu update
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            }
        }

        // Log informasi package name & SHA-1 untuk keperluan debugging API key restriction
        Log.i("KakaoMaps", "Package Name: $packageName")
        try {
            val info = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            if (signingInfo != null) {
                val signatures = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                signatures?.forEach { sig ->
                    val md = java.security.MessageDigest.getInstance("SHA-1")
                    val digest = md.digest(sig.toByteArray())
                    val sha1 = digest.joinToString(":") { "%02X".format(it) }
                    Log.i("KakaoMaps", "APK SHA-1 Fingerprint: $sha1")
                    Log.i("KakaoMaps", "Pastikan SHA-1 di atas sudah terdaftar di Google Cloud Console -> Credentials -> API Key -> Android restrictions")
                }
            }
        } catch (e: Exception) {
            Log.e("KakaoMaps", "Gagal membaca signing info", e)
        }

        // Cek apakah API_KEY sudah terpasang di manifest
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
            if (apiKey.isNullOrBlank() || apiKey == "YOUR_MAPS_API_KEY_HERE") {
                Log.e("KakaoMaps", "API KEY BELUM DISET! Peta tidak akan tampil. Atur MAPS_API_KEY di file .env")
            } else {
                Log.i("KakaoMaps", "Maps API Key terdeteksi: ${apiKey.take(10)}...${apiKey.takeLast(4)} (length=${apiKey.length})")
            }
        } catch (e: Exception) {
            Log.e("KakaoMaps", "Gagal membaca meta-data API key", e)
        }

        // Inisialisasi Maps SDK dengan renderer LATEST (menggantikan LEGACY yang bisa menyebabkan blank pada beberapa device)
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(
                applicationContext,
                com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
            ) { renderer ->
                Log.i("KakaoMaps", "Maps SDK berhasil diinisialisasi dengan renderer: $renderer")
            }
        } catch (e: Exception) {
            Log.e("KakaoMaps", "Gagal inisialisasi Maps SDK", e)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { /* hasil izin ditangani per-screen */ }

                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    RegistrationNavGraph()
                }
            }
        }
    }
}
