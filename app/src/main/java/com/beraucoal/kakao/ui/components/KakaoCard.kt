package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.ui.theme.*

/**
 * Card kontainer standar Kakao — white, rounded modern (24dp), dengan whitespace luas di dalam.
 */
@Composable
fun KakaoContentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(KakaoRadius.Card),
        colors = CardDefaults.cardColors(containerColor = KakaoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = KakaoElevation.Low),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            content = content
        )
    }
}

/**
 * Banner peringatan kuning — rounded, icon warning, teks deskriptif.
 */
@Composable
fun KakaoWarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(KakaoRadius.Medium),
        color = KakaoColors.WarningContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = KakaoColors.Warning,
                modifier = Modifier.size(KakaoSizes.IconMedium)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                color = KakaoColors.WarningText
            )
        }
    }
}

/**
 * Banner status sukses — hijau lembut, icon check, teks.
 */
@Composable
fun KakaoSuccessBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(KakaoRadius.Large),
        color = KakaoColors.SuccessContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning, // will be replaced by CheckCircle in screens
                contentDescription = null,
                tint = KakaoColors.Primary
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = message,
                fontWeight = FontWeight.Bold,
                color = KakaoColors.PrimaryDark
            )
        }
    }
}
