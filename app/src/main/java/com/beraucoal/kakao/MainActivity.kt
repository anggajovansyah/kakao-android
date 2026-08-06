package com.beraucoal.kakao

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // splash native (ikon daun di latar hijau) tampil sekilas sebelum Compose ambil alih
        super.onCreate(savedInstanceState)

        try {
            // Memaksa penggunaan LEGACY renderer untuk menghindari bug RPC timeout pada maps_core di emulator
            com.google.android.gms.maps.MapsInitializer.initialize(
                applicationContext,
                com.google.android.gms.maps.MapsInitializer.Renderer.LEGACY
            ) { renderer ->
                android.util.Log.d("KakaoMaps", "Maps SDK initialized with renderer: $renderer")
            }
        } catch (e: Exception) {
            android.util.Log.e("KakaoMaps", "Failed to initialize Maps SDK", e)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { /* hasil izin ditangani per-screen (mis. cek ulang di KebunMappingScreen) */ }

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
