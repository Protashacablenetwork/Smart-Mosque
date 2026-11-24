package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.example.data.model.UserProfile
import com.example.data.model.WelfareRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocietyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val base = FirestoreBase(db, auth)
    private val finance = SocietyFinanceDelegate(base)
    private val community = SocietyCommunityDelegate(base, finance)

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "aliakbor1624@gmail.com"
        private const val PUBLIC_SUMMARY_DOC = "overview"
        private const val MOSQUE_SCHEDULE_DOC = "main"
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = base.requireUserId()
        val path = "profiles/$userId"
        emitAll(
            db.collection("profiles").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val toLog = fe ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) handleFirestoreError(toLog, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observePublicSummary(): Flow<PublicSummary> = flow {
        val path = "public_summary/$PUBLIC_SUMMARY_DOC"
        emitAll(
            db.collection("public_summary").document(PUBLIC_SUMMARY_DOC)
                .snapshots()
                .map { snap -> snap.toObject(PublicSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: PublicSummary() }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val toLog = fe ?: (error as? Exception)
                    if (toLog != null) handleFirestoreError(toLog, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeMosqueSchedule(): Flow<MosqueSchedule> = flow {
        val path = "mosque_schedule/$MOSQUE_SCHEDULE_DOC"
        emitAll(
            db.collection("mosque_schedule").document(MOSQUE_SCHEDULE_DOC)
                .snapshots()
                .map { snap -> snap.toObject(MosqueSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: MosqueSchedule() }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val toLog = fe ?: (error as? Exception)
                    if (toLog != null) handleFirestoreError(toLog, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeNotices(): Flow<List<Notice>> =
        base.observeCollection("notices", Notice::class.java, requiresAuth = false) { list ->
            list.sortedWith(compareByDescending<Notice> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
        }

    fun observeFamilies(): Flow<List<Family>> =
        base.observeCollection("families", Family::class.java) { it.sortedBy { f -> f.familyCode } }

    fun observeMembers(): Flow<List<SocietyMember>> =
        base.observeCollection("members", SocietyMember::class.java) { it.sortedBy { m -> m.memberName } }

    fun observeDues(): Flow<List<DueItem>> =
        base.observeCollection("dues", DueItem::class.java) { it.sortedByDescending { d -> d.dueYear * 100 + d.dueMonth } }

    fun observePayments(): Flow<List<PaymentRecord>> =
        base.observeCollection("payments", PaymentRecord::class.java) { list ->
            list.sortedWith(compareByDescending<PaymentRecord> { it.paymentDate }.thenByDescending { it.createdAt?.seconds ?: 0L })
        }

    fun observeDonations(): Flow<List<DonationRecord>> =
        base.observeCollection("donations", DonationRecord::class.java) { list ->
            list.sortedWith(compareByDescending<DonationRecord> { it.donationDate }.thenByDescending { it.createdAt?.seconds ?: 0L })
        }

    fun observeExpenses(): Flow<List<ExpenseRecord>> =
        base.observeCollection("expenses", ExpenseRecord::class.java) { list ->
            list.sortedWith(compareByDescending<ExpenseRecord> { it.expenseDate }.thenByDescending { it.createdAt?.seconds ?: 0L })
        }

    fun observeBankAccounts(): Flow<List<BankAccount>> =
        base.observeCollection("bank_accounts", BankAccount::class.java) { it.sortedBy { a -> a.accountName } }

    fun observeBudgets(): Flow<List<BudgetItem>> =
        base.observeCollection("budgets", BudgetItem::class.java) { it.sortedByDescending { b -> b.periodLabel } }

    fun observeQurbaniBatches(): Flow<List<QurbaniBatch>> =
        base.observeCollection("qurbani_batches", QurbaniBatch::class.java) { it.sortedByDescending { q -> q.createdAt?.seconds ?: 0L } }

    fun observeMeatDistributions(): Flow<List<MeatDistribution>> =
        base.observeCollection("meat_distributions", MeatDistribution::class.java) { it.sortedBy { d -> d.familyCode } }

    fun observeCommittees(): Flow<List<CommitteeMember>> =
        base.observeCollection("committees", CommitteeMember::class.java) { it.sortedBy { c -> c.name } }

    fun observeMeetings(): Flow<List<MeetingRecord>> =
        base.observeCollection("meetings", MeetingRecord::class.java) { it.sortedByDescending { m -> m.meetingDate } }

    fun observeEvents(): Flow<List<SocietyEvent>> =
        base.observeCollection("events", SocietyEvent::class.java, requiresAuth = false) { it.sortedBy { e -> e.eventDate } }

    fun observeAssets(): Flow<List<SocietyAsset>> =
        base.observeCollection("assets", SocietyAsset::class.java) { it.sortedByDescending { a -> a.createdAt?.seconds ?: 0L } }

    fun observeDocuments(): Flow<List<SocietyDocument>> =
        base.observeCollection("documents", SocietyDocument::class.java) { it.sortedByDescending { d -> d.docDate } }

    fun observeWelfareRecords(): Flow<List<WelfareRecord>> =
        base.observeCollection("welfare_records", WelfareRecord::class.java) { it.sortedByDescending { w -> w.recordDate } }

    fun observeComplaints(): Flow<List<ComplaintItem>> =
        base.observeCollection("complaints", ComplaintItem::class.java) { it.sortedByDescending { c -> c.createdAt?.seconds ?: 0L } }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> =
        base.observeCollection("audit_logs", AuditLogEntry::class.java) { it.sortedByDescending { l -> l.createdAt?.seconds ?: 0L } }

    // Member Portal isolated queries
    fun observeLinkedFamiliesForEmail(email: String): Flow<List<Family>> =
        base.observeQuery(
            "families?linkedEmail=$email",
            db.collection("families").whereEqualTo("linkedEmail", email.lowercase()),
            Family::class.java
        ) { it }

    fun observeMembersForFamily(familyId: String): Flow<List<SocietyMember>> =
        base.observeQuery(
            "members?familyId=$familyId",
            db.collection("members").whereEqualTo("familyId", familyId),
            SocietyMember::class.java
        ) { it.sortedBy { m -> m.memberName } }

    fun observePaymentsForFamily(familyId: String): Flow<List<PaymentRecord>> =
        base.observeQuery(
            "payments?familyId=$familyId",
            db.collection("payments").whereEqualTo("familyId", familyId),
            PaymentRecord::class.java
        ) { it.sortedByDescending { p -> p.paymentDate } }

    fun observeDistributionsForFamily(familyId: String): Flow<List<MeatDistribution>> =
        base.observeQuery(
            "meat_distributions?familyId=$familyId",
            db.collection("meat_distributions").whereEqualTo("familyId", familyId),
            MeatDistribution::class.java
        ) { it }

    fun observeOwnComplaints(uid: String): Flow<List<ComplaintItem>> =
        base.observeQuery(
            "complaints?authorUid=$uid",
            db.collection("complaints").whereEqualTo("authorUid", uid),
            ComplaintItem::class.java
        ) { it.sortedByDescending { c -> c.createdAt?.seconds ?: 0L } }

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "profiles/$uid"
        return try {
            val user = auth.currentUser!!
            val docRef = db.collection("profiles").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (parsed != null) return Result.success(parsed)
            }
            val email = (user.email?.takeIf { it.length >= 3 } ?: "member@masjid.local").take(120)
            val isBootstrapped = email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
            val role = if (isBootstrapped) SocietyRoles.SUPER_ADMIN else SocietyRoles.MEMBER
            val fullName = (user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")).take(80)
            val payload = mapOf(
                "userId" to uid,
                "fullName" to fullName,
                "email" to email,
                "role" to role,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(UserProfile(id = uid, userId = uid, fullName = fullName, email = email, role = role))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun linkProfileToFamily(familyId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("profiles").document(uid).update(
                mapOf("linkedFamilyId" to familyId, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun setUserRole(targetUid: String, role: String): Result<Unit> {
        val cleanUid = targetUid.trim()
        val path = "profiles/$cleanUid"
        return try {
            base.requireUserId()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid user UID format" }
            require(role in SocietyRoles.ALL_ROLES) { "Invalid role" }
            val docRef = db.collection("profiles").document(cleanUid)
            val snap = docRef.get().await()
            require(snap.exists()) { "User profile not found. Ask them to sign in once first." }
            val prevRole = snap.getString("role") ?: "member"
            docRef.update(mapOf("role" to role, "updatedAt" to FieldValue.serverTimestamp())).await()
            base.logAudit("SET_ROLE", "profiles", cleanUid, "Updated role for $cleanUid to $role", beforeSummary = "role=$prevRole", afterSummary = "role=$role")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMosqueSchedule(schedule: MosqueSchedule): Result<Unit> {
        return try {
            val uid = base.requireUserId()
            require(schedule.mosqueName.isNotBlank()) { "Mosque name is required" }
            val payload = mapOf(
                "mosqueName" to schedule.mosqueName.trim().take(120),
                "fajrTime" to schedule.fajrTime.trim().take(20),
                "sunriseTime" to schedule.sunriseTime?.trim()?.takeIf { it.isNotBlank() }?.take(20),
                "dhuhrTime" to schedule.dhuhrTime.trim().take(20),
                "asrTime" to schedule.asrTime.trim().take(20),
                "maghribTime" to schedule.maghribTime.trim().take(20),
                "ishaTime" to schedule.ishaTime.trim().take(20),
                "jumuahTime" to schedule.jumuahTime.trim().take(30),
                "khutbahTopic" to schedule.khutbahTopic?.trim()?.takeIf { it.isNotBlank() }?.take(200),
                "sehriEnd" to schedule.sehriEnd?.trim()?.takeIf { it.isNotBlank() }?.take(20),
                "iftarTime" to schedule.iftarTime?.trim()?.takeIf { it.isNotBlank() }?.take(20),
                "imamName" to schedule.imamName?.trim()?.takeIf { it.isNotBlank() }?.take(100),
                "imamPhone" to schedule.imamPhone?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                "muazzinName" to schedule.muazzinName?.trim()?.takeIf { it.isNotBlank() }?.take(100),
                "muazzinPhone" to schedule.muazzinPhone?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("mosque_schedule").document(MOSQUE_SCHEDULE_DOC).set(payload).await()
            base.logAudit("UPDATE_SCHEDULE", "mosque_schedule", MOSQUE_SCHEDULE_DOC, schedule.mosqueName, afterSummary = "Jumuah: ${schedule.jumuahTime}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "mosque_schedule/$MOSQUE_SCHEDULE_DOC")
            Result.failure(e)
        }
    }

    suspend fun saveFamily(
        familyId: String?,
        familyName: String,
        familyCode: String,
        phone: String?,
        address: String?,
        linkedEmail: String?,
        active: Boolean
    ): Result<String> {
        val cleanName = familyName.trim().take(100)
        val cleanCode = familyCode.trim().take(30)
        val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
        val cleanAddress = address?.trim()?.takeIf { it.isNotBlank() }?.take(200)
        val cleanEmail = linkedEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.take(120)
        return try {
            val uid = base.requireUserId()
            require(cleanName.isNotEmpty()) { "Family name is required" }
            require(cleanCode.isNotEmpty()) { "Family code is required" }
            val col = db.collection("families")
            val docRef = if (familyId.isNullOrBlank()) col.document() else col.document(familyId)
            if (familyId.isNullOrBlank()) {
                val payload = mapOf(
                    "familyName" to cleanName,
                    "phone" to cleanPhone,
                    "address" to cleanAddress,
                    "familyCode" to cleanCode,
                    "linkedEmail" to cleanEmail,
                    "active" to active,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_FAMILY", "families", docRef.id, "$cleanCode - $cleanName", afterSummary = "$cleanName ($cleanCode)")
            } else {
                val prev = docRef.get().await()
                val prevSummary = "${prev.getString("familyName")} (${prev.getString("familyCode")})"
                val updates = mutableMapOf<String, Any>(
                    "familyName" to cleanName,
                    "familyCode" to cleanCode,
                    "active" to active,
                    "phone" to (cleanPhone ?: FieldValue.delete()),
                    "address" to (cleanAddress ?: FieldValue.delete()),
                    "linkedEmail" to (cleanEmail ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                base.logAudit("UPDATE_FAMILY", "families", docRef.id, "$cleanCode - $cleanName", beforeSummary = prevSummary, afterSummary = "$cleanName ($cleanCode)")
            }
            finance.refreshPublicSummary()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "families/${familyId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteFamily(familyId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("families").document(familyId).delete().await()
            base.logAudit("DELETE_FAMILY", "families", familyId, "Deleted family $familyId")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "families/$familyId")
            Result.failure(e)
        }
    }

    suspend fun saveMember(
        memberId: String?,
        familyId: String,
        familyName: String,
        memberName: String,
        phone: String?,
        relation: String?,
        occupation: String?,
        head: Boolean,
        active: Boolean
    ): Result<Unit> {
        val cleanName = memberName.trim().take(100)
        val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
        val cleanRelation = relation?.trim()?.takeIf { it.isNotBlank() }?.take(40)
        val cleanOccupation = occupation?.trim()?.takeIf { it.isNotBlank() }?.take(60)
        return try {
            val uid = base.requireUserId()
            require(familyId.isNotBlank()) { "Family is required" }
            require(cleanName.isNotEmpty()) { "Member name is required" }
            val col = db.collection("members")
            val docRef = if (memberId.isNullOrBlank()) col.document() else col.document(memberId)
            if (memberId.isNullOrBlank()) {
                val payload = mapOf(
                    "familyId" to familyId,
                    "familyName" to familyName.trim().take(100),
                    "memberName" to cleanName,
                    "phone" to cleanPhone,
                    "relation" to cleanRelation,
                    "occupation" to cleanOccupation,
                    "head" to head,
                    "active" to active,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_MEMBER", "members", docRef.id, "$cleanName ($familyName)")
            } else {
                docRef.update(
                    mapOf(
                        "familyName" to familyName.trim().take(100),
                        "memberName" to cleanName,
                        "phone" to (cleanPhone ?: FieldValue.delete()),
                        "relation" to (cleanRelation ?: FieldValue.delete()),
                        "occupation" to (cleanOccupation ?: FieldValue.delete()),
                        "head" to head,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_MEMBER", "members", docRef.id, "$cleanName ($familyName)")
            }
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "members/${memberId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteMember(memberId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("members").document(memberId).delete().await()
            base.logAudit("DELETE_MEMBER", "members", memberId, "Deleted member $memberId")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "members/$memberId")
            Result.failure(e)
        }
    }

    suspend fun publishNotice(title: String, content: String, category: String, pinned: Boolean, authorName: String): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanContent = content.trim().take(2000)
        val cleanCategory = if (category in listOf("general", "prayer", "finance", "qurbani")) category else "general"
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Notice title is required" }
            require(cleanContent.isNotEmpty()) { "Notice content is required" }
            val docRef = db.collection("notices").document()
            val payload = mapOf(
                "title" to cleanTitle, "content" to cleanContent, "category" to cleanCategory,
                "pinned" to pinned, "authorUid" to uid,
                "authorName" to authorName.trim().ifBlank { "Admin" }.take(80),
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            base.logAudit("PUBLISH_NOTICE", "notices", docRef.id, cleanTitle)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notices")
            Result.failure(e)
        }
    }

    suspend fun toggleNoticePinned(noticeId: String, pinned: Boolean): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("notices").document(noticeId).update(
                mapOf("pinned" to pinned, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            base.logAudit("PIN_NOTICE", "notices", noticeId, "Set pinned=$pinned")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notices/$noticeId")
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("notices").document(noticeId).delete().await()
            base.logAudit("DELETE_NOTICE", "notices", noticeId, "Deleted notice $noticeId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "notices/$noticeId")
            Result.failure(e)
        }
    }

    // Delegated Financial ERP methods
    suspend fun createDue(title: String, dueType: String, amount: Double, dueMonth: Long, dueYear: Long, active: Boolean = true) =
        finance.createDue(title, dueType, amount, dueMonth, dueYear, active)
    suspend fun toggleDueActive(dueId: String, active: Boolean) = finance.toggleDueActive(dueId, active)
    suspend fun deleteDue(dueId: String) = finance.deleteDue(dueId)
    suspend fun recordPayment(familyId: String, familyName: String, dueId: String?, dueTitle: String?, amount: Double, paymentMethod: String, paymentDate: String, note: String?, accountId: String? = null, accountName: String? = null) =
        finance.recordPayment(familyId, familyName, dueId, dueTitle, amount, paymentMethod, paymentDate, note, accountId, accountName)
    suspend fun deletePayment(paymentId: String) = finance.deletePayment(paymentId)
    suspend fun recordDonation(donorName: String, anonymous: Boolean, fundType: String, amount: Double, paymentMethod: String, transactionRef: String?, donationDate: String, note: String?, accountId: String? = null, accountName: String? = null) =
        finance.recordDonation(donorName, anonymous, fundType, amount, paymentMethod, transactionRef, donationDate, note, accountId, accountName)
    suspend fun deleteDonation(donationId: String) = finance.deleteDonation(donationId)
    suspend fun addExpense(title: String, category: String, amount: Double, expenseDate: String, note: String?, accountId: String? = null, accountName: String? = null) =
        finance.addExpense(title, category, amount, expenseDate, note, accountId, accountName)
    suspend fun deleteExpense(expenseId: String) = finance.deleteExpense(expenseId)
    suspend fun saveBankAccount(accountId: String?, accountName: String, accountType: String, accountNumber: String?, balance: Double, active: Boolean) =
        finance.saveBankAccount(accountId, accountName, accountType, accountNumber, balance, active)
    suspend fun deleteBankAccount(accountId: String) = finance.deleteBankAccount(accountId)
    suspend fun saveBudget(budgetId: String?, category: String, periodLabel: String, budgetAmount: Double) =
        finance.saveBudget(budgetId, category, periodLabel, budgetAmount)
    suspend fun deleteBudget(budgetId: String) = finance.deleteBudget(budgetId)
    suspend fun refreshPublicSummary() = finance.refreshPublicSummary()

    // Delegated Community / Qurbani / Meetings / Welfare / Complaints / Backup methods
    suspend fun createQurbaniBatchAndDistributions(eventName: String, animalInfo: String?, purchaseCost: Double?, totalMeatKg: Double, eventDate: String, activeFamilies: List<Family>, autoDistribute: Boolean = true) =
        community.createQurbaniBatchAndDistributions(eventName, animalInfo, purchaseCost, totalMeatKg, eventDate, activeFamilies, autoDistribute)
    suspend fun loadSocietyFamiliesForQurbaniYear(year: String, activeFamilies: List<Family>, existingYearBatches: List<QurbaniBatch>, existingDistributions: List<MeatDistribution>) =
        community.loadSocietyFamiliesForQurbaniYear(year, activeFamilies, existingYearBatches, existingDistributions)
    suspend fun addFamilyToQurbaniDistribution(year: String, targetBatch: QurbaniBatch?, family: Family, allocatedKg: Double) =
        community.addFamilyToQurbaniDistribution(year, targetBatch, family, allocatedKg)
    suspend fun recalculateQurbaniEqualShare(batch: QurbaniBatch, batchDistributions: List<MeatDistribution>, totalMeatKgOverride: Double? = null) =
        community.recalculateQurbaniEqualShare(batch, batchDistributions, totalMeatKgOverride)
    suspend fun deleteQurbaniBatch(batchId: String) = community.deleteQurbaniBatch(batchId)
    suspend fun deleteMeatDistribution(distribution: MeatDistribution) = community.deleteMeatDistribution(distribution)
    suspend fun markMeatDelivered(distribution: MeatDistribution, deliveredKg: Double, receivedBy: String) =
        community.markMeatDelivered(distribution, deliveredKg, receivedBy)
    suspend fun saveMeeting(meetingId: String?, title: String, meetingDate: String, meetingTime: String?, location: String?, agenda: String?, attendees: String?, decisions: String, assignedTo: String?, deadline: String?, status: String) =
        community.saveMeeting(meetingId, title, meetingDate, meetingTime, location, agenda, attendees, decisions, assignedTo, deadline, status)
    suspend fun deleteMeeting(meetingId: String) = community.deleteMeeting(meetingId)
    suspend fun saveCommitteeMember(memberId: String?, name: String, role: String, phone: String?, familyId: String?, familyName: String?, term: String?, active: Boolean) =
        community.saveCommitteeMember(memberId, name, role, phone, familyId, familyName, term, active)
    suspend fun deleteCommitteeMember(memberId: String) = community.deleteCommitteeMember(memberId)
    suspend fun saveEvent(eventId: String?, title: String, eventDate: String, eventTime: String?, location: String?, eventType: String, description: String?, rsvpEnabled: Boolean) =
        community.saveEvent(eventId, title, eventDate, eventTime, location, eventType, description, rsvpEnabled)
    suspend fun deleteEvent(eventId: String) = community.deleteEvent(eventId)
    suspend fun saveAsset(assetId: String?, name: String, category: String, value: Double, purchaseDate: String?, condition: String, location: String?, note: String?) =
        community.saveAsset(assetId, name, category, value, purchaseDate, condition, location, note)
    suspend fun deleteAsset(assetId: String) = community.deleteAsset(assetId)
    suspend fun saveDocument(docId: String?, title: String, category: String, docDate: String, fileName: String?, note: String?) =
        community.saveDocument(docId, title, category, docDate, fileName, note)
    suspend fun deleteDocument(docId: String) = community.deleteDocument(docId)
    suspend fun addWelfareRecord(recordType: String, beneficiaryName: String, phone: String?, plotNumber: String?, amount: Double, recordDate: String, note: String?, accountId: String? = null, accountName: String? = null) =
        community.addWelfareRecord(recordType, beneficiaryName, phone, plotNumber, amount, recordDate, note, accountId, accountName)
    suspend fun deleteWelfareRecord(recordId: String) = community.deleteWelfareRecord(recordId)
    suspend fun submitComplaint(authorName: String, familyId: String?, subject: String, message: String) =
        community.submitComplaint(authorName, familyId, subject, message)
    suspend fun replyComplaint(complaintId: String, status: String, adminReply: String?) =
        community.replyComplaint(complaintId, status, adminReply)
    suspend fun deleteComplaint(complaintId: String) = community.deleteComplaint(complaintId)
    suspend fun seedSampleSocietyData(authorName: String) = community.seedSampleSocietyData(authorName)
    suspend fun exportBackupJson() = community.exportBackupJson()
    suspend fun restoreBackupJson(jsonString: String) = community.restoreBackupJson(jsonString)
}
