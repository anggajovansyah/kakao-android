package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoRadius
import com.beraucoal.kakao.ui.theme.KakaoSizes
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily

/**
 * Tombol aksi utama — hijau, rounded, Poppins font.
 * Otomatis menampilkan loading spinner saat [isLoading] = true.
 */
@Composable
fun KakaoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = text,
    icon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(KakaoRadius.Button),
        colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
        modifier = modifier
            .fillMaxWidth()
            .height(KakaoSizes.ButtonHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = loadingText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        } else {
            icon?.invoke()
            if (icon != null) Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Tombol sekunder — outline hijau, rounded, Poppins font.
 */
@Composable
fun KakaoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(KakaoRadius.Input),
        modifier = modifier.fillMaxWidth()
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = KakaoColors.Primary,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}
