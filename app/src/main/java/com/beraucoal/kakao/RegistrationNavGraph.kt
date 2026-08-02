package com.beraucoal.kakao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
}

@Composable
fun RegistrationNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: RegistrationViewModel = viewModel()
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

    val showBottomNav = currentRoute in listOf(Routes.DASHBOARD, Routes.SATELLITE_MAP, Routes.PROFILE)

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
        Box(modifier = Modifier.padding(innerPadding)) {
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
                    SatelliteMapScreen()
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
            }
        }
    }
}
