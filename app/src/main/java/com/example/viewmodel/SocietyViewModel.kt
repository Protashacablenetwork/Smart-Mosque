package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.SocietyRepository
import com.example.data.model.AuditLogEntry
import com.example.data.model.BankAccount
import com.example.data.model.BudgetItem
import com.example.data.model.CommitteeMember
import com.example.data.model.ComplaintItem
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.MeetingRecord
import com.example.data.model.MosqueSchedule
import com.example.data.model.Notice
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicSummary
import com.example.data.model.QurbaniBatch
import com.example.data.model.SocietyAsset
import com.example.data.model.SocietyDocument
import com.example.data.model.SocietyEvent
import com.example.data.model.SocietyMember
import com.example.data.model.SocietyRoles
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WelfareRecord
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppThemeMode
import com.example.data.prefs.AutomationPrefs
import com.example.data.prefs.LanguagePreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocietyViewModel(
    private val repository: SocietyRepository,
    private val languagePrefs: LanguagePreferences,
    val currentUserId: String?,
    private val currentUserEmail: String?,
    private val currentUserEmailVerified: Boolean
) : ViewModel() {

    private companion object {
        const val TAG = "SocietyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val language: StateFlow<AppLanguage> = languagePrefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLanguage.BANGLA)

    val themeMode: StateFlow<AppThemeMode> = languagePrefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppThemeMode.SYSTEM)

    val automationPrefs: StateFlow<AutomationPrefs> = languagePrefs.automationPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AutomationPrefs())

    private fun <T> Flow<T>.toUiStateFlow(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        if (currentUserId == null) {
            flowOf<UiState<UserProfile?>>(UiState.Success(null))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(null))
        } else {
            repository.observeUserProfile(currentUserId)
                .toUiStateFlow("profile")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val effectiveRole: StateFlow<String> = userProfileState
        .map { state ->
            if (currentUserId == null) return@map "guest"
            val bootstrapped = currentUserEmail.equals(SocietyRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && currentUserEmailVerified
            val profileRole = (state as? UiState.Success)?.data?.role
            if (bootstrapped && (profileRole == null || profileRole == SocietyRoles.MEMBER)) {
                SocietyRoles.SUPER_ADMIN
            } else {
                profileRole ?: SocietyRoles.MEMBER
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), if (currentUserId == null) "guest" else SocietyRoles.MEMBER)

    val isSuperAdmin: StateFlow<Boolean> = effectiveRole
        .map { it == SocietyRoles.SUPER_ADMIN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val isAdmin: StateFlow<Boolean> = effectiveRole
        .map { it in listOf(SocietyRoles.SUPER_ADMIN, SocietyRoles.ADMIN) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val isFinanceAdmin: StateFlow<Boolean> = effectiveRole
        .map { it in listOf(SocietyRoles.SUPER_ADMIN, SocietyRoles.ADMIN, SocietyRoles.ACCOUNTANT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val isOpsManager: StateFlow<Boolean> = effectiveRole
        .map { it in listOf(SocietyRoles.SUPER_ADMIN, SocietyRoles.ADMIN, SocietyRoles.MANAGER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val isStaffReader: StateFlow<Boolean> = effectiveRole
        .map { it in listOf(SocietyRoles.SUPER_ADMIN, SocietyRoles.ADMIN, SocietyRoles.ACCOUNTANT, SocietyRoles.MANAGER, SocietyRoles.VIEWER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    // Public (unauthenticated-friendly) streams
    val publicSummaryState: StateFlow<UiState<PublicSummary>> =
        repository.observePublicSummary()
            .toUiStateFlow("public_summary")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val mosqueScheduleState: StateFlow<UiState<MosqueSchedule>> =
        repository.observeMosqueSchedule()
            .toUiStateFlow("mosque_schedule")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val noticesState: StateFlow<UiState<List<Notice>>> =
        repository.observeNotices()
            .toUiStateFlow("notices")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val eventsState: StateFlow<UiState<List<SocietyEvent>>> =
        repository.observeEvents()
            .toUiStateFlow("events")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private fun <T> staffGatedListFlow(
        label: String,
        source: () -> Flow<List<T>>
    ): StateFlow<UiState<List<T>>> =
        isStaffReader
            .flatMapLatest { staff ->
                if (!staff) flowOf<UiState<List<T>>>(UiState.Success(emptyList()))
                else source().toUiStateFlow(label)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val familiesState: StateFlow<UiState<List<Family>>> =
        staffGatedListFlow("families") { repository.observeFamilies() }

    val membersState: StateFlow<UiState<List<SocietyMember>>> =
        staffGatedListFlow("members") { repository.observeMembers() }

    val duesState: StateFlow<UiState<List<DueItem>>> =
        if (currentUserId == null) {
            flowOf<UiState<List<DueItem>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            repository.observeDues()
                .toUiStateFlow("dues")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val paymentsState: StateFlow<UiState<List<PaymentRecord>>> =
        staffGatedListFlow("payments") { repository.observePayments() }

    val donationsState: StateFlow<UiState<List<DonationRecord>>> =
        staffGatedListFlow("donations") { repository.observeDonations() }

    val expensesState: StateFlow<UiState<List<ExpenseRecord>>> =
        staffGatedListFlow("expenses") { repository.observeExpenses() }

    val bankAccountsState: StateFlow<UiState<List<BankAccount>>> =
        staffGatedListFlow("bank_accounts") { repository.observeBankAccounts() }

    val budgetsState: StateFlow<UiState<List<BudgetItem>>> =
        staffGatedListFlow("budgets") { repository.observeBudgets() }

    val qurbaniBatchesState: StateFlow<UiState<List<QurbaniBatch>>> =
        staffGatedListFlow("qurbani_batches") { repository.observeQurbaniBatches() }

    val meatDistributionsState: StateFlow<UiState<List<MeatDistribution>>> =
        staffGatedListFlow("meat_distributions") { repository.observeMeatDistributions() }

    val committeesState: StateFlow<UiState<List<CommitteeMember>>> =
        staffGatedListFlow("committees") { repository.observeCommittees() }

    val meetingsState: StateFlow<UiState<List<MeetingRecord>>> =
        staffGatedListFlow("meetings") { repository.observeMeetings() }

    val assetsState: StateFlow<UiState<List<SocietyAsset>>> =
        staffGatedListFlow("assets") { repository.observeAssets() }

    val documentsState: StateFlow<UiState<List<SocietyDocument>>> =
        staffGatedListFlow("documents") { repository.observeDocuments() }

    val welfareRecordsState: StateFlow<UiState<List<WelfareRecord>>> =
        staffGatedListFlow("welfare_records") { repository.observeWelfareRecords() }

    val complaintsState: StateFlow<UiState<List<ComplaintItem>>> =
        staffGatedListFlow("complaints") { repository.observeComplaints() }

    val auditLogsState: StateFlow<UiState<List<AuditLogEntry>>> =
        staffGatedListFlow("audit_logs") { repository.observeAuditLogs() }

    // Member Portal isolated flows (active for signed-in members linked via verified email)
    val myFamilyState: StateFlow<UiState<Family?>> =
        if (currentUserId == null || currentUserEmail.isNullOrBlank() || !currentUserEmailVerified) {
            flowOf<UiState<Family?>>(UiState.Success(null))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(null))
        } else {
            repository.observeLinkedFamiliesForEmail(currentUserEmail)
                .map { it.firstOrNull() }
                .onEach { fam ->
                    val currentLinked = (userProfileState.value as? UiState.Success)?.data?.linkedFamilyId
                    if (fam != null && fam.id != currentLinked) {
                        repository.linkProfileToFamily(fam.id)
                    }
                }
                .toUiStateFlow("my_family")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val myFamilyMembersState: StateFlow<UiState<List<SocietyMember>>> =
        myFamilyState
            .flatMapLatest { state ->
                val fam = (state as? UiState.Success)?.data
                if (fam == null) flowOf<UiState<List<SocietyMember>>>(UiState.Success(emptyList()))
                else repository.observeMembersForFamily(fam.id).toUiStateFlow("my_family_members")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myFamilyPaymentsState: StateFlow<UiState<List<PaymentRecord>>> =
        myFamilyState
            .flatMapLatest { state ->
                val fam = (state as? UiState.Success)?.data
                if (fam == null) flowOf<UiState<List<PaymentRecord>>>(UiState.Success(emptyList()))
                else repository.observePaymentsForFamily(fam.id).toUiStateFlow("my_family_payments")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myFamilyDistributionsState: StateFlow<UiState<List<MeatDistribution>>> =
        myFamilyState
            .flatMapLatest { state ->
                val fam = (state as? UiState.Success)?.data
                if (fam == null) flowOf<UiState<List<MeatDistribution>>>(UiState.Success(emptyList()))
                else repository.observeDistributionsForFamily(fam.id).toUiStateFlow("my_family_distributions")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myComplaintsState: StateFlow<UiState<List<ComplaintItem>>> =
        if (currentUserId == null) {
            flowOf<UiState<List<ComplaintItem>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            repository.observeOwnComplaints(currentUserId)
                .toUiStateFlow("my_complaints")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    init {
        if (currentUserId != null) {
            viewModelScope.launch {
                repository.ensureUserProfile()
            }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
            languagePrefs.setLanguage(next)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            languagePrefs.setThemeMode(mode)
        }
    }

    fun updateAutomationPrefs(updated: AutomationPrefs) {
        viewModelScope.launch {
            languagePrefs.updateAutomationPrefs(updated)
            _events.emit("Preferences saved")
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Any?>) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    if (successMessage.isNotBlank()) _events.emit(successMessage)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveMosqueSchedule(schedule: MosqueSchedule) =
        runAction("Prayer schedule updated") { repository.saveMosqueSchedule(schedule) }

    fun saveFamily(id: String?, name: String, code: String, phone: String?, address: String?, linkedEmail: String?, active: Boolean) =
        runAction("Family saved") { repository.saveFamily(id, name, code, phone, address, linkedEmail, active) }

    fun deleteFamily(id: String) =
        runAction("Family deleted") { repository.deleteFamily(id) }

    fun saveMember(id: String?, familyId: String, familyName: String, memberName: String, phone: String?, relation: String?, occupation: String?, head: Boolean, active: Boolean) =
        runAction("Member saved") { repository.saveMember(id, familyId, familyName, memberName, phone, relation, occupation, head, active) }

    fun deleteMember(id: String) =
        runAction("Member deleted") { repository.deleteMember(id) }

    fun createDue(title: String, dueType: String, amount: Double, month: Long, year: Long, active: Boolean = true) =
        runAction("Due schedule created") { repository.createDue(title, dueType, amount, month, year, active) }

    fun toggleDueActive(id: String, active: Boolean) =
        runAction("Due updated") { repository.toggleDueActive(id, active) }

    fun deleteDue(id: String) =
        runAction("Due deleted") { repository.deleteDue(id) }

    fun recordPayment(
        familyId: String,
        familyName: String,
        dueId: String?,
        dueTitle: String?,
        amount: Double,
        method: String,
        date: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null,
        onRecorded: (PaymentRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.recordPayment(familyId, familyName, dueId, dueTitle, amount, method, date, note, accountId, accountName)
                res.onSuccess { record ->
                    _events.emit("Receipt ${record.receiptNo} recorded")
                    onRecorded(record)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Failed to record payment")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deletePayment(id: String) =
        runAction("Payment deleted") { repository.deletePayment(id) }

    fun recordDonation(
        donorName: String,
        anonymous: Boolean,
        fundType: String,
        amount: Double,
        method: String,
        transactionRef: String?,
        date: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null,
        onRecorded: (DonationRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.recordDonation(donorName, anonymous, fundType, amount, method, transactionRef, date, note, accountId, accountName)
                res.onSuccess { record ->
                    _events.emit("Donation ${record.receiptNo} recorded")
                    onRecorded(record)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Failed to record donation")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteDonation(id: String) =
        runAction("Donation deleted") { repository.deleteDonation(id) }

    fun addExpense(title: String, category: String, amount: Double, date: String, note: String?, accountId: String? = null, accountName: String? = null) =
        runAction("Expense added") { repository.addExpense(title, category, amount, date, note, accountId, accountName) }

    fun deleteExpense(id: String) =
        runAction("Expense deleted") { repository.deleteExpense(id) }

    fun saveBankAccount(id: String?, name: String, type: String, number: String?, balance: Double, active: Boolean) =
        runAction("Account saved") { repository.saveBankAccount(id, name, type, number, balance, active) }

    fun deleteBankAccount(id: String) =
        runAction("Account deleted") { repository.deleteBankAccount(id) }

    fun saveBudget(id: String?, category: String, periodLabel: String, budgetAmount: Double) =
        runAction("Budget saved") { repository.saveBudget(id, category, periodLabel, budgetAmount) }

    fun deleteBudget(id: String) =
        runAction("Budget deleted") { repository.deleteBudget(id) }

    fun createQurbaniBatch(eventName: String, animalInfo: String?, purchaseCost: Double?, totalMeatKg: Double, eventDate: String, activeFamilies: List<Family>, autoDistribute: Boolean = true) =
        runAction("Qurbani animal / batch saved") { repository.createQurbaniBatchAndDistributions(eventName, animalInfo, purchaseCost, totalMeatKg, eventDate, activeFamilies, autoDistribute) }

    fun loadSocietyFamiliesForQurbaniYear(year: String, activeFamilies: List<Family>, existingYearBatches: List<QurbaniBatch>, existingDistributions: List<MeatDistribution>) =
        runAction("Society families loaded for $year") { repository.loadSocietyFamiliesForQurbaniYear(year, activeFamilies, existingYearBatches, existingDistributions) }

    fun addFamilyToQurbaniDistribution(year: String, targetBatch: QurbaniBatch?, family: Family, allocatedKg: Double) =
        runAction("Recipient added") { repository.addFamilyToQurbaniDistribution(year, targetBatch, family, allocatedKg) }

    fun recalculateQurbaniEqualShare(batch: QurbaniBatch, batchDistributions: List<MeatDistribution>, totalMeatKgOverride: Double? = null) =
        runAction("Equal share recalculated") { repository.recalculateQurbaniEqualShare(batch, batchDistributions, totalMeatKgOverride) }

    fun deleteQurbaniBatch(batchId: String) =
        runAction("Qurbani batch deleted") { repository.deleteQurbaniBatch(batchId) }

    fun deleteMeatDistribution(distribution: MeatDistribution) =
        runAction("Recipient removed") { repository.deleteMeatDistribution(distribution) }

    fun markMeatDelivered(dist: MeatDistribution, deliveredKg: Double, receivedBy: String) =
        runAction("Delivery recorded") { repository.markMeatDelivered(dist, deliveredKg, receivedBy) }

    fun saveMeeting(id: String?, title: String, date: String, time: String?, location: String?, agenda: String?, attendees: String?, decisions: String, assignedTo: String?, deadline: String?, status: String) =
        runAction("Meeting saved") { repository.saveMeeting(id, title, date, time, location, agenda, attendees, decisions, assignedTo, deadline, status) }

    fun deleteMeeting(id: String) =
        runAction("Meeting deleted") { repository.deleteMeeting(id) }

    fun saveCommitteeMember(id: String?, name: String, role: String, phone: String?, familyId: String?, familyName: String?, term: String?, active: Boolean) =
        runAction("Committee member saved") { repository.saveCommitteeMember(id, name, role, phone, familyId, familyName, term, active) }

    fun deleteCommitteeMember(id: String) =
        runAction("Committee member deleted") { repository.deleteCommitteeMember(id) }

    fun saveEvent(id: String?, title: String, eventDate: String, eventTime: String?, location: String?, eventType: String, description: String?, rsvpEnabled: Boolean) =
        runAction("Event saved") { repository.saveEvent(id, title, eventDate, eventTime, location, eventType, description, rsvpEnabled) }

    fun deleteEvent(id: String) =
        runAction("Event deleted") { repository.deleteEvent(id) }

    fun saveAsset(id: String?, name: String, category: String, value: Double, purchaseDate: String?, condition: String, location: String?, note: String?) =
        runAction("Asset saved") { repository.saveAsset(id, name, category, value, purchaseDate, condition, location, note) }

    fun deleteAsset(id: String) =
        runAction("Asset deleted") { repository.deleteAsset(id) }

    fun saveDocument(id: String?, title: String, category: String, docDate: String, fileName: String?, note: String?) =
        runAction("Document saved") { repository.saveDocument(id, title, category, docDate, fileName, note) }

    fun deleteDocument(id: String) =
        runAction("Document deleted") { repository.deleteDocument(id) }

    fun addWelfareRecord(recordType: String, beneficiaryName: String, phone: String?, plotNumber: String?, amount: Double, date: String, note: String?, accountId: String? = null, accountName: String? = null) =
        runAction("Welfare record saved") { repository.addWelfareRecord(recordType, beneficiaryName, phone, plotNumber, amount, date, note, accountId, accountName) }

    fun deleteWelfareRecord(id: String) =
        runAction("Welfare record deleted") { repository.deleteWelfareRecord(id) }

    fun submitComplaint(authorName: String, familyId: String?, subject: String, message: String) =
        runAction("Submitted successfully") { repository.submitComplaint(authorName, familyId, subject, message) }

    fun replyComplaint(id: String, status: String, reply: String?) =
        runAction("Response saved") { repository.replyComplaint(id, status, reply) }

    fun deleteComplaint(id: String) =
        runAction("Complaint deleted") { repository.deleteComplaint(id) }

    fun publishNotice(title: String, content: String, category: String, pinned: Boolean, authorName: String) =
        runAction("Notice published") { repository.publishNotice(title, content, category, pinned, authorName) }

    fun toggleNoticePinned(id: String, pinned: Boolean) =
        runAction("Notice updated") { repository.toggleNoticePinned(id, pinned) }

    fun deleteNotice(id: String) =
        runAction("Notice deleted") { repository.deleteNotice(id) }

    fun setUserRole(targetUid: String, role: String) =
        runAction("Role updated") { repository.setUserRole(targetUid, role) }

    fun seedDemoData(authorName: String) =
        runAction("Sample data loaded") { repository.seedSampleSocietyData(authorName) }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.exportBackupJson()
                    .onSuccess { json -> onReady(json) }
                    .onFailure { err -> _events.emit(err.localizedMessage ?: "Backup failed") }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun restoreBackupJson(jsonString: String) =
        runAction("Backup restored") { repository.restoreBackupJson(jsonString) }
}
