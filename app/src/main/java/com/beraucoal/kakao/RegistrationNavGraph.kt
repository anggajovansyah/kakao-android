package com.beraucoal.kakao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beraucoal.kakao.data.PlantationRepository
import com.beraucoal.kakao.data.BagianPohon
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beraucoal.kakao.ui.components.KakaoBottomNavigation
import com.beraucoal.kakao.ui.components.RegistrationStepperHeader
import com.beraucoal.kakao.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val SCAN_KTP = "scan_ktp"
    const val VERIFY_DATA = "verify_data"
    const val OTP = "otp"
    const val KEBUN_MAPPING = "kebun_mapping"
    const val DONE = "done"
    const val DASHBOARD = "dashboard"
    const val SATELLITE_MAP = "satellite_map"
    const val PROFILE = "profile"
    const val SELF_MAPPING = "self_mapping"

    // ── Reference 2.0: layar-layar baru ──
    const val PIN = "pin"
    const val WAITING_VERIFICATION = "waiting_verification"
    const val UPLOAD_QUEUE = "upload_queue"
    const val RIWAYAT = "riwayat"
    const val TREE_REPORT = "tree_report/{pohonId}"
    const val CAMERA_CAPTURE = "camera_capture/{bagian}/{existingCount}"
}

@Composable
fun RegistrationNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: RegistrationViewModel = viewModel()
    val sharedPlantationRepository = remember { PlantationRepository() }
    val maxStepReached by viewModel.maxStepReached.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val stepNumber = when (currentRoute) {
        Routes.SCAN_KTP -> 1
        Routes.VERIFY_DATA -> 2
        Routes.OTP -> 3
        Routes.KEBUN_MAPPING -> 4
        else -> 0
    }

    val showBottomNav = currentRoute in listOf(
        Routes.DASHBOARD, Routes.SATELLITE_MAP, Routes.PROFILE,
        Routes.UPLOAD_QUEUE, Routes.RIWAYAT
    )

    Scaffold(
        topBar = {
            if (stepNumber in 1..4) {
                RegistrationStepperHeader(
                    currentStep = stepNumber,
                    totalSteps = 4,
                    maxReachedStep = maxStepReached,
                    showBackButton = true,
                    onBackClicked = { navController.popBackStack() },
                    showForwardButton = stepNumber in 1..3 && stepNumber < maxStepReached,
                    onForwardClicked = {
                        if (stepNumber < maxStepReached) {
                            val nextRoute = when (stepNumber) {
                                1 -> Routes.VERIFY_DATA
                                2 -> Routes.OTP
                                3 -> Routes.KEBUN_MAPPING
                                else -> null
                            }
                            if (nextRoute != null && currentRoute != nextRoute) {
                                navController.navigate(nextRoute)
                            }
                        }
                    },
                    onStepClicked = { targetStep ->
                        if (targetStep <= maxStepReached && targetStep != stepNumber) {
                            val targetRoute = when (targetStep) {
                                1 -> Routes.SCAN_KTP
                                2 -> Routes.VERIFY_DATA
                                3 -> Routes.OTP
                                4 -> Routes.KEBUN_MAPPING
                                else -> null
                            }
                            if (targetRoute != null && targetRoute != currentRoute) {
                                navController.navigate(targetRoute)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomNav) {
                KakaoBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val bottomPadding = if (currentRoute == Routes.SATELLITE_MAP) 0.dp else innerPadding.calculateBottomPadding()
        Box(
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = bottomPadding
            )
        ) {
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        viewModel = viewModel,
                        onSignUp = {
                            viewModel.reset()
                            navController.navigate(Routes.SCAN_KTP)
                        },
                        onSignInSuccess = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.SCAN_KTP) {
                    ScanKtpScreen(
                        onKtpProcessed = { ktp ->
                            viewModel.onKtpScanned(ktp)
                            navController.navigate(Routes.VERIFY_DATA)
                        }
                    )
                }
                composable(Routes.VERIFY_DATA) {
                    VerifyDataScreen(
                        viewModel = viewModel,
                        onApproved = { navController.navigate(Routes.OTP) }
                    )
                }
                composable(Routes.OTP) {
                    OtpScreen(
                        viewModel = viewModel,
                        onVerified = { navController.navigate(Routes.KEBUN_MAPPING) }
                    )
                }
                composable(Routes.KEBUN_MAPPING) {
                    KebunMappingScreen(
                        viewModel = viewModel,
                        onSubmitted = { navController.navigate(Routes.DONE) }
                    )
                }
                composable(Routes.DONE) {
                    RegistrationDoneScreen(
                        viewModel = viewModel,
                        onGoToDashboard = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        },
                        onReturnToMenu = {
                            viewModel.reset()
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onLogout = {
                            viewModel.reset()
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.SATELLITE_MAP) {
                    SatelliteMapScreen(
                        plantationRepository = sharedPlantationRepository,
                        onNavigateToSelfMapping = { navController.navigate(Routes.SELF_MAPPING) },
                        onReportKondisi = { pohon -> 
                            navController.navigate("tree_report/${pohon.id}")
                        }
                    )
                }
                composable(Routes.SELF_MAPPING) {
                    SelfMappingScreen(
                        repository = sharedPlantationRepository,
                        onFinished = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onLogout = {
                            viewModel.reset()
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Reference 2.0: Layar-layar baru ──

                composable(Routes.PIN) {
                    PinScreen(
                        onPinCreated = { pin ->
                            // PIN tersimpan, lanjut ke beranda
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.WAITING_VERIFICATION) {
                    WaitingVerificationScreen(
                        onCheckStatus = {
                            // Simulasi: langsung disetujui, arahkan ke masuk
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.WAITING_VERIFICATION) { inclusive = true }
                            }
                        },
                        onGoBack = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.UPLOAD_QUEUE) {
                    UploadQueueScreen(
                        repository = sharedPlantationRepository,
                        onSync = { sharedPlantationRepository.syncUploadQueue() }
                    )
                }

                composable(Routes.RIWAYAT) {
                    RiwayatScreen(
                        repository = sharedPlantationRepository
                    )
                }

                composable(Routes.TREE_REPORT) { backStackEntry ->
                    val pohonId = backStackEntry.arguments?.getString("pohonId")
                    val pohon = pohonId?.let { sharedPlantationRepository.getPohonById(it) }
                    if (pohon != null) {
                        val savedStateHandle = backStackEntry.savedStateHandle

                        // Ambil daftar foto baru (JSON string list) per bagian
                        val newDaunPhotos = savedStateHandle.get<String>("captured_photos_DAUN")
                        val newBatangPhotos = savedStateHandle.get<String>("captured_photos_BATANG")
                        val newBuahPhotos = savedStateHandle.get<String>("captured_photos_BUAH")

                        TreeReportScreen(
                            pohon = pohon,
                            repository = sharedPlantationRepository,
                            newDaunPhotos = newDaunPhotos,
                            newBatangPhotos = newBatangPhotos,
                            newBuahPhotos = newBuahPhotos,
                            onClearPhotoState = { bagian ->
                                savedStateHandle.remove<String>("captured_photos_${bagian.name}")
                            },
                            onNavigateToCamera = { bagian, existingCount ->
                                navController.navigate("camera_capture/${bagian.name}/$existingCount")
                            },
                            onSaved = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }

                composable("camera_capture/{bagian}/{existingCount}") { backStackEntry ->
                    val bagianStr = backStackEntry.arguments?.getString("bagian") ?: "DAUN"
                    val existingCount = backStackEntry.arguments?.getString("existingCount")?.toIntOrNull() ?: 0
                    val bagian = com.beraucoal.kakao.data.BagianPohon.valueOf(bagianStr)
                    ReportCameraScreen(
                        bagian = bagian,
                        existingPhotoCount = existingCount,
                        maxPhotos = 5,
                        onPhotosCaptured = { uriList ->
                            // Kirim list URI sebagai JSON string sederhana
                            val jsonList = uriList.joinToString(separator = "|||")
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "captured_photos_$bagianStr", jsonList
                            )
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
