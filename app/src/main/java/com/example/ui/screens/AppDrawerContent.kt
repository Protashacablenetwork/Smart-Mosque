package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SidebarGradientTop

data class NavDestination(
    val id: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

data class NavGroup(
    val title: String,
    val items: List<NavDestination>
)

fun buildNavGroups(
    strings: AppStrings,
    isSignedIn: Boolean,
    isStaffReader: Boolean,
    isAdmin: Boolean
): List<NavGroup> {
    val primaryItems = listOf(
        NavDestination(0, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavDestination(17, strings.navDashboardTab, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
        NavDestination(3, strings.navAccountsTab, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_finance"),
        NavDestination(9, strings.navMyMosqueTab, Icons.Filled.Mosque, Icons.Outlined.Mosque, "nav_my_family")
    )

    val societyItems = if (isStaffReader) {
        listOf(
            NavDestination(1, strings.navFamilies, Icons.Filled.FamilyRestroom, Icons.Outlined.FamilyRestroom, "nav_families"),
            NavDestination(2, strings.navMembers, Icons.Filled.Groups, Icons.Outlined.Groups, "nav_members"),
            NavDestination(10, strings.navDuesSetup, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_dues_setup"),
            NavDestination(4, strings.navQurbani, Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_qurbani"),
            NavDestination(11, strings.navPrayerCalendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_prayer_calendar"),
            NavDestination(12, strings.navEvents, Icons.Filled.Event, Icons.Outlined.Event, "nav_events"),
            NavDestination(8, strings.navCommunity, Icons.Filled.Diversity3, Icons.Outlined.Diversity3, "nav_community"),
            NavDestination(13, strings.navAssetsDocs, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_assets_docs"),
            NavDestination(5, strings.navNotices, Icons.Filled.Campaign, Icons.Outlined.Campaign, "nav_notices")
        )
    } else {
        listOf(
            NavDestination(11, strings.navPrayerCalendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_prayer_calendar"),
            NavDestination(12, strings.navEvents, Icons.Filled.Event, Icons.Outlined.Event, "nav_events"),
            NavDestination(5, strings.navNotices, Icons.Filled.Campaign, Icons.Outlined.Campaign, "nav_notices")
        )
    }

    val reportsAndSettings = buildList {
        add(NavDestination(6, if (isStaffReader) strings.navAnalyticsTransparency else strings.tabPublicTransparency, Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports"))
        if (isStaffReader) {
            add(NavDestination(14, strings.navAuditCenter, Icons.Filled.History, Icons.Outlined.History, "nav_audit_center"))
        }
        if (isAdmin) {
            add(NavDestination(15, strings.navBackupRestore, Icons.Filled.Backup, Icons.Outlined.Backup, "nav_backup_restore"))
            add(NavDestination(16, strings.navSecurityRoles, Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_security_roles"))
        }
        add(NavDestination(7, strings.navSettingsTab, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"))
    }

    return listOf(
        NavGroup(strings.navGroupCore, primaryItems),
        NavGroup(strings.navGroupMosque, societyItems),
        NavGroup(strings.navGroupSettings, reportsAndSettings)
    )
}

@Composable
fun AppDrawerSheetContent(
    strings: AppStrings,
    isSignedIn: Boolean,
    isStaffReader: Boolean,
    effectiveRole: String,
    userDisplayName: String,
    userEmail: String?,
    navGroups: List<NavGroup>,
    selectedDest: Int,
    onSelectDest: (Int) -> Unit,
    onAuthAction: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(304.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Emerald User Header Banner (matching Screenshot 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomEnd = 24.dp))
                    .background(SidebarGradientTop)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 36.dp, y = (-36).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userDisplayName.trim().firstOrNull()?.uppercase() ?: "M",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = userDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = userEmail?.takeIf { it.isNotBlank() } ?: if (isStaffReader) effectiveRole.uppercase() else strings.guestBadge,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Scrollable Nav Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                navGroups.forEachIndexed { groupIdx, group ->
                    if (groupIdx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        )
                    }
                    group.items.forEach { dest ->
                        val isSelected = selectedDest == dest.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectDest(dest.id) }
                                .testTag(dest.testTag)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                            tint = if (isSelected) SidebarGradientTop else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Logout / Sign In button (matching Screenshot 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSignedIn) SemanticErrorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onAuthAction)
                        .testTag("drawer_auth_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSignedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = if (isSignedIn) SemanticError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSignedIn) strings.signOut else strings.adminSignIn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSignedIn) SemanticError else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
