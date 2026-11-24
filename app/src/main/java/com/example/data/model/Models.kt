package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object SocietyRoles {
    const val SUPER_ADMIN = "super_admin"
    const val ADMIN = "admin"
    const val ACCOUNTANT = "accountant"
    const val MANAGER = "manager"
    const val VIEWER = "viewer"
    const val MEMBER = "member"

    val ALL_ROLES = listOf(SUPER_ADMIN, ADMIN, ACCOUNTANT, MANAGER, VIEWER, MEMBER)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = SocietyRoles.MEMBER,
    val linkedFamilyId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isSuperAdmin: Boolean get() = role == SocietyRoles.SUPER_ADMIN
    val isFullAdmin: Boolean get() = role == SocietyRoles.SUPER_ADMIN || role == SocietyRoles.ADMIN
    val isFinanceAdmin: Boolean get() = isFullAdmin || role == SocietyRoles.ACCOUNTANT
    val isOpsManager: Boolean get() = isFullAdmin || role == SocietyRoles.MANAGER
    val isStaffReader: Boolean get() = isFullAdmin || role in listOf(SocietyRoles.ACCOUNTANT, SocietyRoles.MANAGER, SocietyRoles.VIEWER)
    val isAdminRole: Boolean get() = isFullAdmin
}

@Keep
@IgnoreExtraProperties
data class PublicSummary(
    @DocumentId val id: String = "",
    val totalFamilies: Long = 0L,
    val totalMembers: Long = 0L,
    val totalCollection: Double = 0.0,
    val totalDonations: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalWelfare: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalQurbaniMeatKg: Double = 0.0,
    val totalDistributions: Long = 0L,
    val deliveredDistributions: Long = 0L,
    val latestQurbaniEvent: String? = null,
    val latestTotalMeatKg: Double? = null,
    val latestPerFamilyKg: Double? = null,
    val recentMonths: List<String> = emptyList(),
    val recentCollections: List<Double> = emptyList(),
    val recentExpenses: List<Double> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalIncome: Double get() = totalCollection + totalDonations
    val totalOutflow: Double get() = totalExpense + totalWelfare
    val balance: Double get() = totalIncome - totalOutflow
}

@Keep
@IgnoreExtraProperties
data class MosqueSchedule(
    @DocumentId val id: String = "main",
    val mosqueName: String = "বায়তুল আমান জামে মসজিদ",
    val fajrTime: String = "05:15 AM",
    val sunriseTime: String? = "06:22 AM",
    val dhuhrTime: String = "01:15 PM",
    val asrTime: String = "04:15 PM",
    val maghribTime: String = "05:30 PM",
    val ishaTime: String = "07:30 PM",
    val jumuahTime: String = "01:30 PM",
    val khutbahTopic: String? = null,
    val sehriEnd: String? = null,
    val iftarTime: String? = null,
    val imamName: String? = null,
    val imamPhone: String? = null,
    val muazzinName: String? = null,
    val muazzinPhone: String? = null,
    val updatedBy: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "general",
    val pinned: Boolean = false,
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Family(
    @DocumentId val id: String = "",
    val familyName: String = "",
    val phone: String? = null,
    val address: String? = null,
    val familyCode: String = "",
    val linkedEmail: String? = null,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocietyMember(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val familyName: String = "",
    val memberName: String = "",
    val phone: String? = null,
    val relation: String? = null,
    val occupation: String? = null,
    val head: Boolean = false,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DueItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val dueType: String = "monthly",
    val amount: Double = 0.0,
    val dueMonth: Long = 0L,
    val dueYear: Long = 2026L,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val familyName: String = "",
    val dueId: String? = null,
    val dueTitle: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val amount: Double = 0.0,
    val paymentMethod: String = "cash",
    val receiptNo: String = "",
    val paymentDate: String = "",
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DonationRecord(
    @DocumentId val id: String = "",
    val donorName: String = "",
    val anonymous: Boolean = false,
    val fundType: String = "general",
    val amount: Double = 0.0,
    val paymentMethod: String = "cash",
    val transactionRef: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val receiptNo: String = "",
    val donationDate: String = "",
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Other",
    val voucherNo: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val amount: Double = 0.0,
    val expenseDate: String = "",
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BankAccount(
    @DocumentId val id: String = "",
    val accountName: String = "",
    val accountType: String = "cash",
    val accountNumber: String? = null,
    val balance: Double = 0.0,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BudgetItem(
    @DocumentId val id: String = "",
    val category: String = "",
    val periodLabel: String = "",
    val budgetAmount: Double = 0.0,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QurbaniBatch(
    @DocumentId val id: String = "",
    val eventName: String = "",
    val animalInfo: String? = null,
    val purchaseCost: Double? = null,
    val totalMeatKg: Double = 0.0,
    val totalFamilies: Long = 0L,
    val distributionPerFamily: Double = 0.0,
    val eventDate: String = "",
    val status: String = "draft",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MeatDistribution(
    @DocumentId val id: String = "",
    val batchId: String = "",
    val familyId: String = "",
    val familyName: String = "",
    val familyCode: String = "",
    val allocatedKg: Double = 0.0,
    val deliveredKg: Double = 0.0,
    val receivedBy: String? = null,
    val receivedAt: Timestamp? = null,
    val status: String = "pending",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MeetingRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val meetingDate: String = "",
    val meetingTime: String? = null,
    val location: String? = null,
    val agenda: String? = null,
    val attendees: String? = null,
    val decisions: String = "",
    val assignedTo: String? = null,
    val deadline: String? = null,
    val status: String = "scheduled",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommitteeMember(
    @DocumentId val id: String = "",
    val name: String = "",
    val role: String = "",
    val phone: String? = null,
    val familyId: String? = null,
    val familyName: String? = null,
    val term: String? = null,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocietyEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val eventDate: String = "",
    val eventTime: String? = null,
    val location: String? = null,
    val eventType: String = "religious",
    val description: String? = null,
    val rsvpEnabled: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocietyAsset(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val value: Double = 0.0,
    val purchaseDate: String? = null,
    val condition: String = "good",
    val location: String? = null,
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocietyDocument(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "",
    val docDate: String = "",
    val fileName: String? = null,
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WelfareRecord(
    @DocumentId val id: String = "",
    val recordType: String = "medical",
    val beneficiaryName: String = "",
    val phone: String? = null,
    val plotNumber: String? = null,
    val amount: Double = 0.0,
    val accountId: String? = null,
    val accountName: String? = null,
    val recordDate: String = "",
    val note: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ComplaintItem(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val familyId: String? = null,
    val subject: String = "",
    val message: String = "",
    val status: String = "open",
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val action: String = "",
    val tableName: String = "",
    val recordId: String? = null,
    val summary: String? = null,
    val beforeSummary: String? = null,
    val afterSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
