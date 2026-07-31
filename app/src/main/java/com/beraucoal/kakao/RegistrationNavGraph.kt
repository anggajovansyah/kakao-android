package com.beraucoal.kakao

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beraucoal.kakao.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val SCAN_KTP = "scan_ktp"
    const val VERIFY_DATA = "verify_data"
    const val OTP = "otp"
    const val KEBUN_MAPPING = "kebun_mapping"
    const val DONE = "done"
}

@Composable
fun RegistrationNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: RegistrationViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.SCAN_KTP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
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
            RegistrationDoneScreen(viewModel = viewModel)
        }
    }
}
