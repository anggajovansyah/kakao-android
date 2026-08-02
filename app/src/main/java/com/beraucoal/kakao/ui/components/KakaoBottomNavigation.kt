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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.Routes
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoElevation
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val mainBottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Beranda", Icons.Filled.Home),
    BottomNavItem(Routes.SATELLITE_MAP, "Peta Satelit", Icons.Filled.Map),
    BottomNavItem(Routes.PROFILE, "Profil", Icons.Filled.Person)
)

/**
 * Minimalist Floating Bottom Navigation Bar (iOS & Samsung OneUI Slider Style).
 * Menampilkan kapsul melayang dengan animasi pill indikator geser yang halus saat berpindah antar menu utama.
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
    val barHorizontalPadding = 24.dp
    val containerPaddingHorizontal = 8.dp
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
            .padding(horizontal = barHorizontalPadding, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = KakaoColors.Surface.copy(alpha = 0.95f),
            shadowElevation = KakaoElevation.High,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = containerPaddingHorizontal, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // ── Animated Pill Slider Indicator ──
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(itemWidth)
                        .height(48.dp)
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
                                .height(48.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = item.title,
                                        color = contentColor,
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
