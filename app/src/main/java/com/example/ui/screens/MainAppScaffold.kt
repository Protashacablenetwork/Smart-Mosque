package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.signOutUser
import com.example.data.model.Family
import com.example.data.model.MosqueSchedule
import com.example.data.model.PublicSummary
import com.example.data.model.UiState
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.ServerMode
import com.example.ui.components.CurvedEmeraldTopBar
import com.example.ui.components.FloatingEmeraldBottomBar
import com.example.ui.components.buildShellBottomTabs
import com.example.ui.i18n.stringsFor
import com.example.util.ReportExporter
import com.example.viewmodel.SocietyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: SocietyViewModel,
    isSignedIn: Boolean,
    isDarkTheme: Boolean,
    userDisplayName: String,
    onRequestSignIn: () -> Unit,
    onRestartRequired: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val automationPrefs by viewModel.automationPrefs.collectAsStateWithLifecycle()
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val serverTestResult by viewModel.serverTestResult.collectAsStateWithLifecycle()
    val isTestingServer by viewModel.isTestingServer.collectAsStateWithLifecycle()
    val strings = stringsFor(language)
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isFinanceAdmin by viewModel.isFinanceAdmin.collectAsStateWithLifecycle()
    val isOpsManager by viewModel.isOpsManager.collectAsStateWithLifecycle()
    val isStaffReader by viewModel.isStaffReader.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val publicSummaryState by viewModel.publicSummaryState.collectAsStateWithLifecycle()
    val mosqueScheduleState by viewModel.mosqueScheduleState.collectAsStateWithLifecycle()
    val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val familiesState by viewModel.familiesState.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val duesState by viewModel.duesState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val donationsState by viewModel.donationsState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val bankAccountsState by viewModel.bankAccountsState.collectAsStateWithLifecycle()
    val budgetsState by viewModel.budgetsState.collectAsStateWithLifecycle()
    val batchesState by viewModel.qurbaniBatchesState.collectAsStateWithLifecycle()
    val distsState by viewModel.meatDistributionsState.collectAsStateWithLifecycle()
    val committeesState by viewModel.committeesState.collectAsStateWithLifecycle()
    val meetingsState by viewModel.meetingsState.collectAsStateWithLifecycle()
    val assetsState by viewModel.assetsState.collectAsStateWithLifecycle()
    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val welfareState by viewModel.welfareRecordsState.collectAsStateWithLifecycle()
    val complaintsState by viewModel.complaintsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()

    val myFamilyState by viewModel.myFamilyState.collectAsStateWithLifecycle()
    val myMembersState by viewModel.myFamilyMembersState.collectAsStateWithLifecycle()
    val myPaymentsState by viewModel.myFamilyPaymentsState.collectAsStateWithLifecycle()
    val myDistributionsState by viewModel.myFamilyDistributionsState.collectAsStateWithLifecycle()
    val myComplaintsState by viewModel.myComplaintsState.collectAsStateWithLifecycle()

    val summary = (publicSummaryState as? UiState.Success)?.data ?: PublicSummary()
    val schedule = (mosqueScheduleState as? UiState.Success)?.data ?: MosqueSchedule()
    val notices = (noticesState as? UiState.Success)?.data ?: emptyList()
    val events = (eventsState as? UiState.Success)?.data ?: emptyList()
    val families = (familiesState as? UiState.Success)?.data ?: emptyList()
    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val dues = (duesState as? UiState.Success)?.data ?: emptyList()
    val payments = (paymentsState as? UiState.Success)?.data ?: emptyList()
    val donations = (donationsState as? UiState.Success)?.data ?: emptyList()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val bankAccounts = (bankAccountsState as? UiState.Success)?.data ?: emptyList()
    val budgets = (budgetsState as? UiState.Success)?.data ?: emptyList()
    val batches = (batchesState as? UiState.Success)?.data ?: emptyList()
    val dists = (distsState as? UiState.Success)?.data ?: emptyList()
    val committees = (committeesState as? UiState.Success)?.data ?: emptyList()
    val meetings = (meetingsState as? UiState.Success)?.data ?: emptyList()
    val assets = (assetsState as? UiState.Success)?.data ?: emptyList()
    val documents = (documentsState as? UiState.Success)?.data ?: emptyList()
    val welfareRecords = (welfareState as? UiState.Success)?.data ?: emptyList()
    val complaints = (complaintsState as? UiState.Success)?.data ?: emptyList()
    val auditLogs = (auditLogsState as? UiState.Success)?.data ?: emptyList()

    val myFamily = (myFamilyState as? UiState.Success)?.data
    val myMembers = (myMembersState as? UiState.Success)?.data ?: emptyList()
    val myPayments = (myPaymentsState as? UiState.Success)?.data ?: emptyList()
    val myDistributions = (myDistributionsState as? UiState.Success)?.data ?: emptyList()
    val myComplaints = (myComplaintsState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val navGroups = remember(strings, isSignedIn, isStaffReader, isAdmin) {
        buildNavGroups(strings, isSignedIn, isStaffReader, isAdmin)
    }
    val allNavItems = remember(navGroups) { navGroups.flatMap { it.items } }
    val bottomTabs = remember(strings) { buildShellBottomTabs(strings) }
    val mainBottomIds = remember { setOf(0, 3, 7, 9, 17) }

    var selectedDest by rememberSaveable { mutableIntStateOf(0) }
    var financeSubTab by rememberSaveable { mutableIntStateOf(0) }
    var communitySubTab by rememberSaveable { mutableIntStateOf(0) }
    var assetsSubTab by rememberSaveable { mutableIntStateOf(0) }
    var reportsSubTab by remember { mutableStateOf<Int?>(null) }

    val validIds = allNavItems.map { it.id }
    val safeDest = if (selectedDest in validIds) selectedDest else 0

    var quickPayFamily by remember { mutableStateOf<Family?>(null) }
    var quickPayAmount by remember { mutableStateOf<Double?>(null) }
    val currentUserEmail = viewModel.currentUserEmail

    fun handleSignOut() {
        if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER) {
            viewModel.signOutLocalServer()
        } else {
            signOutUser(credentialManager, scope)
        }
    }

    fun navigateTo(destId: Int, subTab: Int = 0) {
        when (destId) {
            3 -> financeSubTab = subTab
            10 -> financeSubTab = 2
            8 -> communitySubTab = subTab
            13 -> assetsSubTab = subTab
            6 -> reportsSubTab = subTab
        }
        selectedDest = destId
    }

    BackHandler(enabled = drawerState.isOpen || safeDest != 0) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else selectedDest = 0
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerSheetContent(
                strings = strings,
                isSignedIn = isSignedIn,
                isStaffReader = isStaffReader,
                effectiveRole = effectiveRole,
                userDisplayName = userDisplayName,
                userEmail = currentUserEmail,
                navGroups = navGroups,
                selectedDest = safeDest,
                onSelectDest = { id ->
                    when (id) {
                        3 -> financeSubTab = 0
                        6 -> reportsSubTab = null
                        8 -> communitySubTab = 0
                        13 -> assetsSubTab = 0
                    }
                    selectedDest = id
                    scope.launch { drawerState.close() }
                },
                onAuthAction = {
                    scope.launch { drawerState.close() }
                    if (isSignedIn) handleSignOut() else onRequestSignIn()
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (safeDest != 4) {
                        val isHome = safeDest == 0
                        val showBack = safeDest !in mainBottomIds
                        val barTitle = when (safeDest) {
                            0 -> schedule.mosqueName.ifBlank { strings.appTitle }
                            1 -> strings.navFamilies
                            2 -> strings.navMembers
                            3 -> strings.navAccountsTab
                            5 -> strings.navNotices
                            6 -> if (isStaffReader) strings.navAnalyticsTransparency else strings.tabPublicTransparency
                            7 -> strings.navSettingsTab
                            8 -> strings.navCommunity
                            9 -> strings.navMyMosqueTab
                            10 -> strings.navDuesSetup
                            11 -> strings.navPrayerCalendar
                            12 -> strings.navEvents
                            13 -> strings.navAssetsDocs
                            14 -> strings.navAuditCenter
                            15 -> strings.navBackupRestore
                            16 -> strings.navSecurityRoles
                            17 -> strings.navDashboardTab
                            else -> strings.appTitle
                        }
                        val barSubtitle = if (isHome) {
                            "${strings.greetingSalam.substringBefore(" 🕌")}, $userDisplayName"
                        } else null

                        CurvedEmeraldTopBar(
                            title = barTitle,
                            subtitle = barSubtitle,
                            isHomeMode = isHome,
                            showBackArrow = showBack,
                            isSignedIn = isSignedIn,
                            hasNoticesBadge = notices.isNotEmpty(),
                            onMenuOrBackClick = {
                                if (showBack) selectedDest = 0
                                else scope.launch { drawerState.open() }
                            },
                            onTitleClick = if (isHome) { { selectedDest = 11 } } else null,
                            onNoticesClick = { selectedDest = 5 },
                            onAuthClick = {
                                if (isSignedIn) handleSignOut() else onRequestSignIn()
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!useNavRail && safeDest in mainBottomIds) {
                        FloatingEmeraldBottomBar(
                            tabs = bottomTabs,
                            selectedDest = safeDest,
                            onSelectDest = { destId ->
                                if (destId == 3) financeSubTab = 0
                                selectedDest = destId
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (useNavRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            allNavItems.take(7).forEach { tab ->
                                val selected = safeDest == tab.id
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        if (tab.id == 3) financeSubTab = 0
                                        if (tab.id == 6) reportsSubTab = null
                                        selectedDest = tab.id
                                    },
                                    icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                        when (safeDest) {
                            0 -> HomeHubScreen(
                                strings = strings,
                                isBangla = language == AppLanguage.BANGLA,
                                isSignedIn = isSignedIn,
                                isStaffReader = isStaffReader,
                                isAdmin = isAdmin,
                                canEditSchedule = isOpsManager,
                                summaryState = publicSummaryState,
                                scheduleState = mosqueScheduleState,
                                noticesState = noticesState,
                                events = events,
                                onSaveSchedule = viewModel::saveMosqueSchedule,
                                onSeedDemo = { viewModel.seedDemoData(userDisplayName) },
                                onRequestSignIn = onRequestSignIn,
                                onNavigate = { dest, sub -> navigateTo(dest, sub) }
                            )
                            17 -> DashboardOverviewScreen(
                                strings = strings,
                                isStaffReader = isStaffReader,
                                showDashboardChart = automationPrefs.showDashboardChart,
                                summary = summary,
                                notices = notices,
                                events = events,
                                families = families,
                                committees = committees,
                                payments = payments,
                                donations = donations,
                                expenses = expenses,
                                welfareRecords = welfareRecords,
                                onNavigate = { dest, sub -> navigateTo(dest, sub) }
                            )
                            1, 2 -> FamiliesScreen(
                                strings = strings,
                                canEdit = isOpsManager,
                                canDelete = isAdmin,
                                canCollectPayment = isFinanceAdmin,
                                families = families,
                                members = members,
                                dues = dues,
                                payments = payments,
                                initialDirectoryMode = if (safeDest == 2) 1 else 0,
                                onSaveFamily = viewModel::saveFamily,
                                onDeleteFamily = viewModel::deleteFamily,
                                onSaveMember = viewModel::saveMember,
                                onDeleteMember = viewModel::deleteMember,
                                onQuickCollect = { fam, amt -> quickPayFamily = fam; quickPayAmount = amt; selectedDest = 3 }
                            )
                            3, 10 -> FinanceScreen(
                                strings = strings,
                                canEditFinance = isFinanceAdmin,
                                canDeleteAdmin = isAdmin,
                                families = families,
                                dues = dues,
                                payments = payments,
                                donations = donations,
                                expenses = expenses,
                                bankAccounts = bankAccounts,
                                budgets = budgets,
                                welfareRecords = welfareRecords,
                                initialSubTab = if (safeDest == 10) 2 else financeSubTab,
                                autoReceiptPreview = automationPrefs.autoReceipt,
                                quickPayFamily = quickPayFamily,
                                quickPayAmount = quickPayAmount,
                                onConsumeQuickPay = { quickPayFamily = null; quickPayAmount = null },
                                onCreateDue = viewModel::createDue,
                                onToggleDueActive = viewModel::toggleDueActive,
                                onDeleteDue = viewModel::deleteDue,
                                onRecordPayment = { fam, due, amt, method, date, note, acc, onRec ->
                                    viewModel.recordPayment(fam.id, fam.familyName, due?.id, due?.title, amt, method, date, note, acc?.id, acc?.accountName, onRec)
                                },
                                onDeletePayment = viewModel::deletePayment,
                                onRecordDonation = { donor, anon, fund, amt, method, trxRef, date, note, acc, onRec ->
                                    viewModel.recordDonation(donor, anon, fund, amt, method, trxRef, date, note, acc?.id, acc?.accountName, onRec)
                                },
                                onDeleteDonation = viewModel::deleteDonation,
                                onAddExpense = { title, cat, amt, date, note, acc ->
                                    viewModel.addExpense(title, cat, amt, date, note, acc?.id, acc?.accountName)
                                },
                                onDeleteExpense = viewModel::deleteExpense,
                                onSaveBankAccount = viewModel::saveBankAccount,
                                onDeleteBankAccount = viewModel::deleteBankAccount,
                                onSaveBudget = viewModel::saveBudget,
                                onDeleteBudget = viewModel::deleteBudget
                            )
                            4 -> QurbaniScreen(
                                strings = strings,
                                canEdit = isOpsManager,
                                canDelete = isAdmin,
                                families = families,
                                batches = batches,
                                distributions = dists,
                                onCreateBatch = viewModel::createQurbaniBatch,
                                onLoadSocietyFamilies = viewModel::loadSocietyFamiliesForQurbaniYear,
                                onAddRecipient = viewModel::addFamilyToQurbaniDistribution,
                                onRecalculateShare = viewModel::recalculateQurbaniEqualShare,
                                onDeleteBatch = viewModel::deleteQurbaniBatch,
                                onDeleteDistribution = viewModel::deleteMeatDistribution,
                                onDeliverMeat = viewModel::markMeatDelivered,
                                onNavigateBack = { selectedDest = 0 }
                            )
                            5 -> NoticesAuditScreen(
                                strings = strings,
                                isStaffReader = isStaffReader,
                                canManageNotices = isOpsManager,
                                currentUserName = userDisplayName,
                                notices = notices,
                                auditLogs = auditLogs,
                                onPublishNotice = viewModel::publishNotice,
                                onToggleNoticePinned = viewModel::toggleNoticePinned,
                                onDeleteNotice = viewModel::deleteNotice
                            )
                            6 -> ReportsScreen(
                                strings = strings,
                                isStaffReader = isStaffReader,
                                mosqueName = schedule.mosqueName,
                                summary = summary,
                                notices = notices,
                                families = families,
                                payments = payments,
                                donations = donations,
                                expenses = expenses,
                                welfareRecords = welfareRecords,
                                initialSubTab = reportsSubTab
                            )
                            8 -> CommunityScreen(
                                strings = strings,
                                currentUserName = userDisplayName,
                                canManageOps = isOpsManager,
                                canWriteWelfare = isFinanceAdmin || isOpsManager,
                                canDeleteAdmin = isAdmin,
                                families = families,
                                committees = committees,
                                meetings = meetings,
                                welfareRecords = welfareRecords,
                                complaints = complaints,
                                bankAccounts = bankAccounts,
                                initialSubTab = communitySubTab,
                                onSaveCommittee = viewModel::saveCommitteeMember,
                                onDeleteCommittee = viewModel::deleteCommitteeMember,
                                onSaveMeeting = viewModel::saveMeeting,
                                onDeleteMeeting = viewModel::deleteMeeting,
                                onAddWelfare = viewModel::addWelfareRecord,
                                onDeleteWelfare = viewModel::deleteWelfareRecord,
                                onSubmitComplaint = viewModel::submitComplaint,
                                onReplyComplaint = viewModel::replyComplaint,
                                onDeleteComplaint = viewModel::deleteComplaint
                            )
                            9 -> MemberPortalScreen(
                                strings = strings,
                                userId = viewModel.currentUserId ?: "",
                                userName = userDisplayName,
                                userEmail = currentUserEmail,
                                myFamily = myFamily,
                                myMembers = myMembers,
                                dues = dues,
                                myPayments = myPayments,
                                myDistributions = myDistributions,
                                myComplaints = myComplaints,
                                onSubmitComplaint = viewModel::submitComplaint,
                                isStaffReader = isStaffReader,
                                schedule = schedule,
                                onRequestSignIn = onRequestSignIn,
                                allFamilies = families,
                                allMembers = members,
                                allPayments = payments,
                                allDistributions = dists
                            )
                            11 -> PrayerCalendarScreen(
                                strings = strings,
                                schedule = schedule,
                                events = events,
                                meetings = meetings,
                                canEditSchedule = isOpsManager,
                                onSaveSchedule = viewModel::saveMosqueSchedule
                            )
                            12 -> EventsScreen(
                                strings = strings,
                                canManageOps = isOpsManager,
                                events = events,
                                onSaveEvent = viewModel::saveEvent,
                                onDeleteEvent = viewModel::deleteEvent
                            )
                            13 -> AssetsDocsScreen(
                                strings = strings,
                                canWrite = isOpsManager || isFinanceAdmin,
                                canDeleteAdmin = isAdmin,
                                assets = assets,
                                documents = documents,
                                initialSubTab = assetsSubTab,
                                onSaveAsset = viewModel::saveAsset,
                                onDeleteAsset = viewModel::deleteAsset,
                                onSaveDocument = viewModel::saveDocument,
                                onDeleteDocument = viewModel::deleteDocument
                            )
                            14 -> AuditCenterScreen(strings = strings, auditLogs = auditLogs)
                            15 -> {
                                val counts = remember(families, members, dues, payments, donations, expenses, bankAccounts, budgets, batches, dists, committees, meetings, events, assets, documents, welfareRecords, complaints, notices) {
                                    listOf(
                                        "families" to families.size, "members" to members.size, "dues" to dues.size,
                                        "payments" to payments.size, "donations" to donations.size, "expenses" to expenses.size,
                                        "bank_accounts" to bankAccounts.size, "budgets" to budgets.size, "qurbani_batches" to batches.size,
                                        "meat_distributions" to dists.size, "committees" to committees.size, "meetings" to meetings.size,
                                        "events" to events.size, "assets" to assets.size, "documents" to documents.size,
                                        "welfare_records" to welfareRecords.size, "complaints" to complaints.size, "notices" to notices.size
                                    )
                                }
                                val total = counts.sumOf { it.second }
                                val kb = if (total == 0) 0 else maxOf(1, (total * 260) / 1024)
                                BackupRestoreScreen(
                                    strings = strings,
                                    collectionCounts = counts,
                                    estimatedJsonKb = kb,
                                    onExportBackupJson = { viewModel.exportBackupJson { json -> ReportExporter.exportJsonBackup(context, json) } },
                                    onRestoreBackupJson = viewModel::restoreBackupJson
                                )
                            }
                            16 -> SecurityRolesScreen(
                                strings = strings,
                                currentUserId = viewModel.currentUserId ?: "",
                                effectiveRole = effectiveRole,
                                isSuperAdmin = isSuperAdmin,
                                onSetUserRole = viewModel::setUserRole
                            )
                            else -> SettingsScreen(
                                strings = strings,
                                currentUserId = viewModel.currentUserId ?: "",
                                effectiveRole = effectiveRole,
                                language = language,
                                themeMode = themeMode,
                                automationPrefs = automationPrefs,
                                serverConfig = serverConfig,
                                serverTestResult = serverTestResult,
                                isTestingServer = isTestingServer,
                                isStaffReader = isStaffReader,
                                families = families,
                                members = members,
                                payments = payments,
                                committees = committees,
                                onToggleLanguage = viewModel::toggleLanguage,
                                onSetThemeMode = viewModel::setThemeMode,
                                onUpdateAutomationPrefs = viewModel::updateAutomationPrefs,
                                onTestServerConnection = viewModel::testLocalServerConnection,
                                onSaveServerConfig = { cfg ->
                                    viewModel.updateServerConfig(cfg, onRestartRequired = onRestartRequired)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
