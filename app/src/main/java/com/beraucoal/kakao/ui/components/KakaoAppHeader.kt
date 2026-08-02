package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.beraucoal.kakao.R
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoSizes

/**
 * Header identitas kolaborasi ITSB × Berau Coal.
 * Diposisikan di kiri (ITSB) dan kanan (Berau Coal) dengan ukuran logo yang jauh lebih besar
 * dan whitespace yang nyaman agar terlihat prominen & profesional di setiap halaman.
 */
@Composable
fun KakaoAppHeader(modifier: Modifier = Modifier) {
    Surface(
        color = KakaoColors.Surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.itsb_logo),
                contentDescription = "Logo ITSB",
                modifier = Modifier.height(KakaoSizes.HeaderLogoHeight)
            )

            Image(
                painter = painterResource(id = R.drawable.berau_logo),
                contentDescription = "Logo Berau Coal",
                modifier = Modifier.height(KakaoSizes.HeaderLogoHeight)
            )
        }
    }
}
