package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SidebarGradientTop

val ShellGoldAccent = Color(0xFFF6C324)
val ShellGoldInk = Color(0xFF122019)

data class FloatingBarTab(
    val destId: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

fun buildShellBottomTabs(strings: AppStrings): List<FloatingBarTab> = listOf(
    FloatingBarTab(7, strings.navSettingsTab, Icons.Filled.Settings, Icons.Outlined.Settings, "bottom_tab_settings"),
    FloatingBarTab(17, strings.navDashboardTab, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "bottom_tab_dashboard"),
    FloatingBarTab(0, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "bottom_tab_home"),
    FloatingBarTab(3, strings.navAccountsTab, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "bottom_tab_accounts"),
    FloatingBarTab(9, strings.navMyMosqueTab, Icons.Filled.Mosque, Icons.Outlined.Mosque, "bottom_tab_my_mosque")
)

@Composable
fun CurvedEmeraldTopBar(
    title: String,
    subtitle: String?,
    isHomeMode: Boolean,
    showBackArrow: Boolean,
    isSignedIn: Boolean,
    hasNoticesBadge: Boolean,
    onMenuOrBackClick: () -> Unit,
    onTitleClick: (() -> Unit)? = null,
    onNoticesClick: () -> Unit,
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SidebarGradientTop,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderSquareIconButton(
                icon = if (showBackArrow) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (showBackArrow) "Back" else "Menu",
                onClick = onMenuOrBackClick,
                testTag = if (showBackArrow) "topbar_back_button" else "drawer_menu_button"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isHomeMode && onTitleClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!showBackArrow) {
                HeaderSquareIconButton(
                    icon = if (isSignedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                    contentDescription = if (isSignedIn) "Sign Out" else "Sign In",
                    showDot = isSignedIn,
                    dotColor = ShellGoldAccent,
                    onClick = onAuthClick,
                    testTag = if (isSignedIn) "sign_out_button" else "topbar_sign_in_button"
                )
                HeaderSquareIconButton(
                    icon = if (hasNoticesBadge) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Notices",
                    showDot = hasNoticesBadge,
                    dotColor = Color(0xFFE85252),
                    onClick = onNoticesClick,
                    testTag = "topbar_notices_button"
                )
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun HeaderSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    showDot: Boolean = false,
    dotColor: Color = Color.Red
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun FloatingEmeraldBottomBar(
    tabs: List<FloatingBarTab>,
    selectedDest: Int,
    onSelectDest: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = SidebarGradientTop,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = selectedDest == tab.destId
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) ShellGoldAccent else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectDest(tab.destId) }
                            .testTag(tab.testTag)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (selected) ShellGoldInk else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) ShellGoldInk else Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
