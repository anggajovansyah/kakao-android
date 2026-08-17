package com.beraucoal.kakao.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.Routes
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoElevation
import com.beraucoal.kakao.ui.theme.PlusJakartaSansFontFamily

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * 5 tab bottom navigation sesuai reference 2.0:
 * Beranda — Foto — Kirim — Riwayat — Akun
 *
 * Catatan: route CAMERA dan QUEUE belum ada implementasinya,
 * untuk saat ini diarahkan ke SATELLITE_MAP sebagai placeholder.
 * Route RIWAYAT juga diarahkan ke DASHBOARD. Akan diperbarui
 * saat layar-layar tersebut diimplementasikan.
 */
val mainBottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Beranda", Icons.Filled.Home),
    BottomNavItem(Routes.SATELLITE_MAP, "Foto", Icons.Filled.CameraAlt),
    BottomNavItem(Routes.UPLOAD_QUEUE, "Kirim", Icons.Filled.CloudUpload),
    BottomNavItem(Routes.RIWAYAT, "Riwayat", Icons.Filled.History),
    BottomNavItem(Routes.PROFILE, "Akun", Icons.Filled.Person)
)

/**
 * Bottom Navigation Bar sesuai reference 2.0 prototipe.
 * Menggunakan gaya iOS/Samsung OneUI dengan pill indicator geser yang halus.
 * Lima tab: Beranda, Foto, Kirim, Riwayat, Akun.
 */
@Composable
fun KakaoBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = mainBottomNavItems
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val barHorizontalPadding = 16.dp
    val containerPaddingHorizontal = 4.dp
    val availableWidth = screenWidth - (barHorizontalPadding * 2) - (containerPaddingHorizontal * 2)
    val itemWidth = availableWidth / items.size

    val pillOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PillOffsetAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = barHorizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = KakaoColors.Surface,
            shadowElevation = KakaoElevation.High,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = containerPaddingHorizontal, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // ── Animated Pill Slider Indicator ──
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(itemWidth)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(KakaoColors.PrimaryContainer)
                )

                // ── Tab Items Row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val contentColor = if (isSelected) KakaoColors.PrimaryDark else KakaoColors.TextMuted

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(itemWidth)
                                .height(44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (currentRoute != item.route) {
                                        onNavigate(item.route)
                                    }
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
