package com.example.data.localserver

import com.example.data.model.DonationRecord
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.MosqueSchedule
import com.example.data.model.PaymentRecord
import com.example.data.model.QurbaniBatch
import com.example.data.model.SocietyRoles
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

internal class LocalHttpOps(private val backend: LocalHttpBackend) {

    private fun roundKg(value: Double): Double = (value * 1000.0).roundToLong() / 1000.0

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val s = backend.session ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = backend.getCurrentProfileOrNull(s.userId)
        if (existing != null) return Result.success(existing)
        val payload = mapOf(
            "userId" to s.userId,
            "fullName" to s.fullName,
            "email" to s.email,
            "role" to s.role
        )
        backend.upsertDoc("profiles", s.userId, payload)
        return Result.success(UserProfile(id = s.userId, userId = s.userId, fullName = s.fullName, email = s.email, role = s.role))
    }

    suspend fun linkProfileToFamily(familyId: String): Result<Unit> {
        val uid = backend.session?.userId ?: return Result.failure(IllegalStateException("Not signed in"))
        return backend.upsertDoc("profiles", uid, mapOf("linkedFamilyId" to familyId)).map { }
    }

    suspend fun setUserRole(targetUid: String, role: String): Result<Unit> {
        return try {
            backend.requireUid()
            require(role in SocietyRoles.ALL_ROLES) { "Invalid role" }
            backend.upsertDoc("profiles", targetUid.trim(), mapOf("role" to role)).getOrThrow()
            backend.logAudit("SET_ROLE", "profiles", targetUid.trim(), "Updated role to $role")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMosqueSchedule(schedule: MosqueSchedule): Result<Unit> {
        return try {
            val uid = backend.requireUid()
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
                "updatedBy" to uid
            )
            backend.putSchedule(payload).getOrThrow()
            backend.logAudit("UPDATE_SCHEDULE", "mosque_schedule", "main", schedule.mosqueName)
            Result.success(Unit)
        } catch (e: Exception) {
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
        return try {
            val uid = backend.requireUid()
            val cleanName = familyName.trim().take(100)
            val cleanCode = familyCode.trim().take(30)
            require(cleanName.isNotEmpty()) { "Family name is required" }
            require(cleanCode.isNotEmpty()) { "Family code is required" }
            val payload = mutableMapOf<String, Any?>(
                "familyName" to cleanName,
                "familyCode" to cleanCode,
                "phone" to phone?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                "address" to address?.trim()?.takeIf { it.isNotBlank() }?.take(200),
                "linkedEmail" to linkedEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.take(120),
                "active" to active
            )
            if (familyId.isNullOrBlank()) payload["createdBy"] = uid
            val id = backend.upsertDoc("families", familyId, payload).getOrThrow()
            backend.logAudit(if (familyId.isNullOrBlank()) "CREATE_FAMILY" else "UPDATE_FAMILY", "families", id, "$cleanCode - $cleanName")
            Result.success(id)
        } catch (e: Exception) {
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
        return try {
            val uid = backend.requireUid()
            val cleanName = memberName.trim().take(100)
            require(familyId.isNotBlank()) { "Family is required" }
            require(cleanName.isNotEmpty()) { "Member name is required" }
            val payload = mutableMapOf<String, Any?>(
                "familyId" to familyId,
                "familyName" to familyName.trim().take(100),
                "memberName" to cleanName,
                "phone" to phone?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                "relation" to relation?.trim()?.takeIf { it.isNotBlank() }?.take(40),
                "occupation" to occupation?.trim()?.takeIf { it.isNotBlank() }?.take(60),
                "head" to head,
                "active" to active
            )
            if (memberId.isNullOrBlank()) payload["createdBy"] = uid
            val id = backend.upsertDoc("members", memberId, payload).getOrThrow()
            backend.logAudit(if (memberId.isNullOrBlank()) "CREATE_MEMBER" else "UPDATE_MEMBER", "members", id, "$cleanName ($familyName)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishNotice(title: String, content: String, category: String, pinned: Boolean, authorName: String): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanTitle = title.trim().take(120)
            val cleanContent = content.trim().take(2000)
            require(cleanTitle.isNotEmpty()) { "Notice title is required" }
            require(cleanContent.isNotEmpty()) { "Notice content is required" }
            val id = backend.upsertDoc("notices", null, mapOf(
                "title" to cleanTitle,
                "content" to cleanContent,
                "category" to if (category in listOf("general", "prayer", "finance", "qurbani")) category else "general",
                "pinned" to pinned,
                "authorUid" to uid,
                "authorName" to authorName.trim().ifBlank { "Admin" }.take(80)
            )).getOrThrow()
            backend.logAudit("PUBLISH_NOTICE", "notices", id, cleanTitle)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDue(title: String, dueType: String, amount: Double, dueMonth: Long, dueYear: Long, active: Boolean): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Due title is required" }
            require(amount >= 0.0) { "Amount cannot be negative" }
            val id = backend.upsertDoc("dues", null, mapOf(
                "title" to cleanTitle,
                "dueType" to if (dueType in listOf("monthly", "yearly", "special")) dueType else "monthly",
                "amount" to amount,
                "dueMonth" to dueMonth.coerceIn(0L, 12L),
                "dueYear" to dueYear.coerceIn(2000L, 2100L),
                "active" to active,
                "createdBy" to uid
            )).getOrThrow()
            backend.logAudit("CREATE_DUE", "dues", id, "$cleanTitle ($amount)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun adjustAccountBalance(accountId: String?, delta: Double) {
        if (accountId.isNullOrBlank()) return
        val acc = backend.snapshot.value.bankAccounts.firstOrNull { it.id == accountId } ?: return
        backend.upsertDoc("bank_accounts", acc.id, mapOf("balance" to (acc.balance + delta)))
    }

    suspend fun recordPayment(
        familyId: String, familyName: String, dueId: String?, dueTitle: String?,
        amount: Double, paymentMethod: String, paymentDate: String, note: String?,
        accountId: String?, accountName: String?
    ): Result<PaymentRecord> {
        return try {
            val uid = backend.requireUid()
            require(familyId.isNotBlank()) { "Select a family" }
            require(amount > 0.0) { "Payment amount must be greater than zero" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val receiptNo = "RCP-$dateStamp-${(1000..9999).random()}"
            val cleanDate = paymentDate.trim().ifBlank { backend.todayIso() }.take(20)
            val id = backend.upsertDoc("payments", null, mapOf(
                "familyId" to familyId,
                "familyName" to familyName.trim().take(100),
                "dueId" to dueId?.takeIf { it.isNotBlank() },
                "dueTitle" to dueTitle?.takeIf { it.isNotBlank() }?.take(100),
                "accountId" to accountId?.takeIf { it.isNotBlank() },
                "accountName" to accountName?.takeIf { it.isNotBlank() }?.take(100),
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "receiptNo" to receiptNo,
                "paymentDate" to cleanDate,
                "note" to note?.trim()?.takeIf { it.isNotBlank() }?.take(300),
                "createdBy" to uid
            )).getOrThrow()
            adjustAccountBalance(accountId, amount)
            backend.logAudit("RECORD_PAYMENT", "payments", id, "$receiptNo: $familyName - $amount")
            Result.success(PaymentRecord(id = id, familyId = familyId, familyName = familyName, dueId = dueId, dueTitle = dueTitle, accountId = accountId, accountName = accountName, amount = amount, paymentMethod = paymentMethod, receiptNo = receiptNo, paymentDate = cleanDate, note = note, createdBy = uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordDonation(
        donorName: String, anonymous: Boolean, fundType: String, amount: Double,
        paymentMethod: String, transactionRef: String?, donationDate: String, note: String?,
        accountId: String?, accountName: String?
    ): Result<DonationRecord> {
        return try {
            val uid = backend.requireUid()
            val cleanDonor = if (anonymous) "Anonymous" else donorName.trim().ifBlank { "Donor" }.take(100)
            require(amount > 0.0) { "Donation amount must be greater than zero" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val receiptNo = "DON-$dateStamp-${(1000..9999).random()}"
            val cleanDate = donationDate.trim().ifBlank { backend.todayIso() }.take(20)
            val id = backend.upsertDoc("donations", null, mapOf(
                "donorName" to cleanDonor,
                "anonymous" to anonymous,
                "fundType" to fundType,
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "transactionRef" to transactionRef?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                "accountId" to accountId?.takeIf { it.isNotBlank() },
                "accountName" to accountName?.takeIf { it.isNotBlank() }?.take(100),
                "receiptNo" to receiptNo,
                "donationDate" to cleanDate,
                "note" to note?.trim()?.takeIf { it.isNotBlank() }?.take(300),
                "createdBy" to uid
            )).getOrThrow()
            adjustAccountBalance(accountId, amount)
            backend.logAudit("RECORD_DONATION", "donations", id, "$receiptNo: $cleanDonor - $amount")
            Result.success(DonationRecord(id = id, donorName = cleanDonor, anonymous = anonymous, fundType = fundType, amount = amount, paymentMethod = paymentMethod, transactionRef = transactionRef, accountId = accountId, accountName = accountName, receiptNo = receiptNo, donationDate = cleanDate, note = note, createdBy = uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        title: String, category: String, amount: Double, expenseDate: String,
        note: String?, accountId: String?, accountName: String?
    ): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Expense title is required" }
            require(amount > 0.0) { "Expense amount must be greater than zero" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val voucherNo = "VCH-$dateStamp-${(1000..9999).random()}"
            val id = backend.upsertDoc("expenses", null, mapOf(
                "title" to cleanTitle,
                "category" to category.trim().ifBlank { "Other" }.take(50),
                "voucherNo" to voucherNo,
                "accountId" to accountId?.takeIf { it.isNotBlank() },
                "accountName" to accountName?.takeIf { it.isNotBlank() }?.take(100),
                "amount" to amount,
                "expenseDate" to expenseDate.trim().ifBlank { backend.todayIso() }.take(20),
                "note" to note?.trim()?.takeIf { it.isNotBlank() }?.take(300),
                "createdBy" to uid
            )).getOrThrow()
            adjustAccountBalance(accountId, -amount)
            backend.logAudit("CREATE_EXPENSE", "expenses", id, "$voucherNo: $cleanTitle ($amount)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBankAccount(accountId: String?, accountName: String, accountType: String, accountNumber: String?, balance: Double, active: Boolean): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanName = accountName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Account name is required" }
            val payload = mutableMapOf<String, Any?>(
                "accountName" to cleanName,
                "accountType" to accountType,
                "accountNumber" to accountNumber?.trim()?.takeIf { it.isNotBlank() }?.take(50),
                "balance" to balance,
                "active" to active
            )
            if (accountId.isNullOrBlank()) payload["createdBy"] = uid
            val id = backend.upsertDoc("bank_accounts", accountId, payload).getOrThrow()
            backend.logAudit("SAVE_ACCOUNT", "bank_accounts", id, cleanName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBudget(budgetId: String?, category: String, periodLabel: String, budgetAmount: Double): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(category.isNotBlank() && periodLabel.isNotBlank()) { "Category and period are required" }
            val payload = mutableMapOf<String, Any?>(
                "category" to category.trim().take(50),
                "periodLabel" to periodLabel.trim().take(30),
                "budgetAmount" to budgetAmount.coerceAtLeast(0.0)
            )
            if (budgetId.isNullOrBlank()) payload["createdBy"] = uid
            val id = backend.upsertDoc("budgets", budgetId, payload).getOrThrow()
            backend.logAudit("SAVE_BUDGET", "budgets", id, "$category ($periodLabel)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createQurbaniBatchAndDistributions(
        eventName: String, animalInfo: String?, purchaseCost: Double?, totalMeatKg: Double,
        eventDate: String, activeFamilies: List<Family>, autoDistribute: Boolean
    ): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanName = eventName.trim().take(120)
            require(cleanName.isNotEmpty() && totalMeatKg > 0.0) { "Invalid event name or meat kg" }
            val targets = if (autoDistribute) activeFamilies else emptyList()
            val perFamily = if (targets.isNotEmpty()) roundKg(totalMeatKg / targets.size.toDouble()) else 0.0
            val batchId = backend.upsertDoc("qurbani_batches", null, mapOf(
                "eventName" to cleanName,
                "animalInfo" to animalInfo?.trim()?.takeIf { it.isNotBlank() }?.take(150),
                "purchaseCost" to purchaseCost?.takeIf { it >= 0.0 },
                "totalMeatKg" to totalMeatKg,
                "totalFamilies" to targets.size.toLong(),
                "distributionPerFamily" to perFamily,
                "eventDate" to eventDate.trim().ifBlank { backend.todayIso() }.take(20),
                "status" to if (targets.isNotEmpty()) "distributing" else "draft",
                "createdBy" to uid
            )).getOrThrow()
            targets.forEach { fam ->
                backend.upsertDoc("meat_distributions", "${batchId}_${fam.id}", mapOf(
                    "batchId" to batchId, "familyId" to fam.id,
                    "familyName" to fam.familyName.take(100), "familyCode" to fam.familyCode.take(30),
                    "allocatedKg" to perFamily, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid
                ))
            }
            backend.logAudit("GENERATE_QURBANI", "qurbani_batches", batchId, "$cleanName (${totalMeatKg}kg)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSocietyFamiliesForQurbaniYear(
        year: String, activeFamilies: List<Family>,
        existingYearBatches: List<QurbaniBatch>, existingDistributions: List<MeatDistribution>
    ): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(activeFamilies.isNotEmpty()) { "No active families found." }
            val targetBatch = existingYearBatches.firstOrNull()
            val existingForBatch = if (targetBatch != null) existingDistributions.filter { it.batchId == targetBatch.id } else emptyList()
            val existingIds = existingForBatch.map { it.familyId }.toSet()
            val missing = activeFamilies.filter { it.id !in existingIds }
            val totalCount = existingForBatch.size + missing.size
            val meat = targetBatch?.totalMeatKg?.takeIf { it > 0.0 } ?: existingYearBatches.sumOf { it.totalMeatKg }
            val perFamily = if (meat > 0.0 && totalCount > 0) roundKg(meat / totalCount.toDouble()) else 0.0
            val batchId = if (targetBatch == null) {
                backend.upsertDoc("qurbani_batches", null, mapOf(
                    "eventName" to "কুরবানি বণ্টন $year", "totalMeatKg" to meat,
                    "totalFamilies" to totalCount.toLong(), "distributionPerFamily" to perFamily,
                    "eventDate" to "$year-06-17", "status" to "distributing", "createdBy" to uid
                )).getOrThrow()
            } else {
                backend.upsertDoc("qurbani_batches", targetBatch.id, mapOf(
                    "totalFamilies" to totalCount.toLong(), "distributionPerFamily" to perFamily, "status" to "distributing"
                )).getOrThrow()
                targetBatch.id
            }
            missing.forEach { fam ->
                backend.upsertDoc("meat_distributions", "${batchId}_${fam.id}", mapOf(
                    "batchId" to batchId, "familyId" to fam.id,
                    "familyName" to fam.familyName.take(100), "familyCode" to fam.familyCode.take(30),
                    "allocatedKg" to perFamily, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid
                ))
            }
            if (perFamily > 0.0) {
                existingForBatch.filter { it.status == "pending" }.forEach { dist ->
                    backend.upsertDoc("meat_distributions", dist.id, mapOf("allocatedKg" to perFamily))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFamilyToQurbaniDistribution(year: String, targetBatch: QurbaniBatch?, family: Family, allocatedKg: Double): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val batchId = if (targetBatch == null) {
                backend.upsertDoc("qurbani_batches", null, mapOf(
                    "eventName" to "কুরবানি বণ্টন $year", "totalMeatKg" to allocatedKg,
                    "totalFamilies" to 1L, "distributionPerFamily" to allocatedKg,
                    "eventDate" to "$year-06-17", "status" to "distributing", "createdBy" to uid
                )).getOrThrow()
            } else {
                val exists = backend.snapshot.value.meatDistributions.any { it.id == "${targetBatch.id}_${family.id}" }
                if (!exists) {
                    backend.upsertDoc("qurbani_batches", targetBatch.id, mapOf("totalFamilies" to (targetBatch.totalFamilies + 1L), "status" to "distributing"))
                }
                targetBatch.id
            }
            backend.upsertDoc("meat_distributions", "${batchId}_${family.id}", mapOf(
                "batchId" to batchId, "familyId" to family.id,
                "familyName" to family.familyName.take(100), "familyCode" to family.familyCode.take(30),
                "allocatedKg" to allocatedKg, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid
            )).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recalculateQurbaniEqualShare(batch: QurbaniBatch, batchDistributions: List<MeatDistribution>, totalMeatKgOverride: Double?): Result<Unit> {
        return try {
            backend.requireUid()
            val effectiveMeat = (totalMeatKgOverride ?: batch.totalMeatKg).coerceAtLeast(0.0)
            val count = batchDistributions.size
            val perFamily = if (count > 0 && effectiveMeat > 0.0) roundKg(effectiveMeat / count.toDouble()) else 0.0
            backend.upsertDoc("qurbani_batches", batch.id, mapOf(
                "totalMeatKg" to effectiveMeat, "totalFamilies" to count.toLong(), "distributionPerFamily" to perFamily
            )).getOrThrow()
            batchDistributions.filter { it.status == "pending" }.forEach { dist ->
                backend.upsertDoc("meat_distributions", dist.id, mapOf("allocatedKg" to perFamily))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQurbaniBatch(batchId: String): Result<Unit> {
        return try {
            backend.requireUid()
            backend.snapshot.value.meatDistributions.filter { it.batchId == batchId }.forEach {
                backend.deleteDoc("meat_distributions", it.id)
            }
            backend.deleteDoc("qurbani_batches", batchId).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMeatDistribution(distribution: MeatDistribution): Result<Unit> {
        return try {
            backend.requireUid()
            backend.deleteDoc("meat_distributions", distribution.id).getOrThrow()
            val batch = backend.snapshot.value.qurbaniBatches.firstOrNull { it.id == distribution.batchId }
            val remainingCount = backend.snapshot.value.meatDistributions.count { it.batchId == distribution.batchId && it.id != distribution.id }
            if (batch != null) {
                val perFamily = if (remainingCount > 0 && batch.totalMeatKg > 0.0) roundKg(batch.totalMeatKg / remainingCount.toDouble()) else 0.0
                backend.upsertDoc("qurbani_batches", batch.id, mapOf("totalFamilies" to remainingCount.toLong(), "distributionPerFamily" to perFamily))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMeatDelivered(distribution: MeatDistribution, deliveredKg: Double, receivedBy: String): Result<Unit> {
        return try {
            backend.requireUid()
            val cleanReceiver = receivedBy.trim().take(100)
            require(cleanReceiver.isNotEmpty() && deliveredKg > 0.0) { "Receiver name and positive kg required" }
            backend.upsertDoc("meat_distributions", distribution.id, mapOf(
                "deliveredKg" to deliveredKg,
                "receivedBy" to cleanReceiver,
                "receivedAt" to System.currentTimeMillis(),
                "status" to "delivered"
            )).getOrThrow()
            val anyPending = backend.snapshot.value.meatDistributions.any { it.batchId == distribution.batchId && it.id != distribution.id && it.status == "pending" }
            if (!anyPending) {
                backend.upsertDoc("qurbani_batches", distribution.batchId, mapOf("status" to "completed"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMeeting(meetingId: String?, title: String, meetingDate: String, meetingTime: String?, location: String?, agenda: String?, attendees: String?, decisions: String, assignedTo: String?, deadline: String?, status: String): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Meeting title is required" }
            val payload = mutableMapOf<String, Any?>(
                "title" to cleanTitle,
                "meetingDate" to meetingDate.trim().ifBlank { backend.todayIso() },
                "meetingTime" to meetingTime?.trim()?.takeIf { it.isNotBlank() },
                "location" to location?.trim()?.takeIf { it.isNotBlank() },
                "agenda" to agenda?.trim()?.takeIf { it.isNotBlank() },
                "attendees" to attendees?.trim()?.takeIf { it.isNotBlank() },
                "decisions" to decisions.trim().ifBlank { "-" },
                "assignedTo" to assignedTo?.trim()?.takeIf { it.isNotBlank() },
                "deadline" to deadline?.trim()?.takeIf { it.isNotBlank() },
                "status" to if (status in listOf("scheduled", "in_progress", "completed")) status else "scheduled"
            )
            if (meetingId.isNullOrBlank()) payload["createdBy"] = uid
            backend.upsertDoc("meetings", meetingId, payload).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCommitteeMember(memberId: String?, name: String, role: String, phone: String?, familyId: String?, familyName: String?, term: String?, active: Boolean): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(name.isNotBlank() && role.isNotBlank()) { "Name and role required" }
            val payload = mutableMapOf<String, Any?>(
                "name" to name.trim().take(100), "role" to role.trim().take(80),
                "phone" to phone?.trim()?.takeIf { it.isNotBlank() },
                "familyId" to familyId?.trim()?.takeIf { it.isNotBlank() },
                "familyName" to familyName?.trim()?.takeIf { it.isNotBlank() },
                "term" to term?.trim()?.takeIf { it.isNotBlank() },
                "active" to active
            )
            if (memberId.isNullOrBlank()) payload["createdBy"] = uid
            backend.upsertDoc("committees", memberId, payload).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEvent(eventId: String?, title: String, eventDate: String, eventTime: String?, location: String?, eventType: String, description: String?, rsvpEnabled: Boolean): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(title.isNotBlank()) { "Event title required" }
            val payload = mutableMapOf<String, Any?>(
                "title" to title.trim().take(120),
                "eventDate" to eventDate.trim().ifBlank { backend.todayIso() },
                "eventTime" to eventTime?.trim()?.takeIf { it.isNotBlank() },
                "location" to location?.trim()?.takeIf { it.isNotBlank() },
                "eventType" to if (eventType in listOf("religious", "social", "education", "informational")) eventType else "religious",
                "description" to description?.trim()?.takeIf { it.isNotBlank() },
                "rsvpEnabled" to rsvpEnabled
            )
            if (eventId.isNullOrBlank()) payload["createdBy"] = uid
            backend.upsertDoc("events", eventId, payload).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAsset(assetId: String?, name: String, category: String, value: Double, purchaseDate: String?, condition: String, location: String?, note: String?): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(name.isNotBlank()) { "Asset name required" }
            val payload = mutableMapOf<String, Any?>(
                "name" to name.trim().take(120),
                "category" to category.trim().ifBlank { "General" },
                "value" to value.coerceAtLeast(0.0),
                "purchaseDate" to purchaseDate?.trim()?.takeIf { it.isNotBlank() },
                "condition" to if (condition in listOf("good", "needs_repair", "damaged")) condition else "good",
                "location" to location?.trim()?.takeIf { it.isNotBlank() },
                "note" to note?.trim()?.takeIf { it.isNotBlank() }
            )
            if (assetId.isNullOrBlank()) payload["createdBy"] = uid
            backend.upsertDoc("assets", assetId, payload).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDocument(docId: String?, title: String, category: String, docDate: String, fileName: String?, note: String?): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(title.isNotBlank()) { "Document title required" }
            val payload = mutableMapOf<String, Any?>(
                "title" to title.trim().take(120),
                "category" to category.trim().ifBlank { "General" },
                "docDate" to docDate.trim().ifBlank { backend.todayIso() },
                "fileName" to fileName?.trim()?.takeIf { it.isNotBlank() },
                "note" to note?.trim()?.takeIf { it.isNotBlank() }
            )
            if (docId.isNullOrBlank()) payload["createdBy"] = uid
            backend.upsertDoc("documents", docId, payload).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWelfareRecord(recordType: String, beneficiaryName: String, phone: String?, plotNumber: String?, amount: Double, recordDate: String, note: String?, accountId: String?, accountName: String?): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(beneficiaryName.isNotBlank()) { "Beneficiary name required" }
            backend.upsertDoc("welfare_records", null, mapOf(
                "recordType" to recordType,
                "beneficiaryName" to beneficiaryName.trim().take(100),
                "phone" to phone?.trim()?.takeIf { it.isNotBlank() },
                "plotNumber" to plotNumber?.trim()?.takeIf { it.isNotBlank() },
                "amount" to amount.coerceAtLeast(0.0),
                "accountId" to accountId?.takeIf { it.isNotBlank() },
                "accountName" to accountName?.takeIf { it.isNotBlank() },
                "recordDate" to recordDate.trim().ifBlank { backend.todayIso() },
                "note" to note?.trim()?.takeIf { it.isNotBlank() },
                "createdBy" to uid
            )).getOrThrow()
            if (amount > 0.0) adjustAccountBalance(accountId, -amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitComplaint(authorName: String, familyId: String?, subject: String, message: String): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            require(subject.isNotBlank() && message.isNotBlank()) { "Subject and message required" }
            backend.upsertDoc("complaints", null, mapOf(
                "authorUid" to uid,
                "authorName" to authorName.trim().ifBlank { "Member" }.take(80),
                "familyId" to familyId?.takeIf { it.isNotBlank() },
                "subject" to subject.trim().take(120),
                "message" to message.trim().take(1500),
                "status" to "open"
            )).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun replyComplaint(complaintId: String, status: String, adminReply: String?): Result<Unit> {
        return try {
            backend.requireUid()
            backend.upsertDoc("complaints", complaintId, mapOf(
                "status" to status,
                "adminReply" to adminReply?.trim()?.takeIf { it.isNotBlank() }
            )).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedSampleSocietyData(authorName: String): Result<Unit> {
        return try {
            val uid = backend.requireUid()
            val today = backend.todayIso()
            val cal = Calendar.getInstance()
            val ymFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val thisMonth = ymFormat.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            val prevMonth = ymFormat.format(cal.time)

            val root = JSONObject().apply {
                put("mosque_schedule", JSONObject(mapOf(
                    "id" to "main", "mosqueName" to "বায়তুল আমান জামে মসজিদ",
                    "fajrTime" to "05:15 AM", "sunriseTime" to "06:22 AM",
                    "dhuhrTime" to "01:15 PM", "asrTime" to "04:15 PM",
                    "maghribTime" to "05:30 PM", "ishaTime" to "07:30 PM",
                    "jumuahTime" to "01:30 PM", "khutbahTopic" to "যাকাত, সততা ও সামাজিক ভ্রাতৃত্ব",
                    "imamName" to "মাওলানা ইউসুফ আলী", "muazzinName" to "হাফেজ বিল্লাল হোসেন"
                )))
                put("families", backend.listToJsonArray(listOf(
                    mapOf("id" to "fam_sample_01", "familyName" to "আব্দুর রহমান পরিবার", "familyCode" to "FAM-001", "phone" to "+8801711000011", "address" to "মসজিদ রোড, বাড়ি ১২", "active" to true, "createdBy" to uid),
                    mapOf("id" to "fam_sample_02", "familyName" to "কামাল উদ্দিন পরিবার", "familyCode" to "FAM-002", "phone" to "+8801711000022", "address" to "পূর্ব পাড়া, লেন ৩", "active" to true, "createdBy" to uid),
                    mapOf("id" to "fam_sample_03", "familyName" to "হাজী ইসমাইল পরিবার", "familyCode" to "FAM-003", "phone" to "+8801711000033", "address" to "উত্তর মহল্লা, রোড ৫", "active" to true, "createdBy" to uid),
                    mapOf("id" to "fam_sample_04", "familyName" to "মাহমুদুল হাসান পরিবার", "familyCode" to "FAM-004", "phone" to "+8801711000044", "address" to "দক্ষিণ পাড়া, ব্লক বি", "active" to true, "createdBy" to uid)
                )))
                put("members", backend.listToJsonArray(listOf(
                    mapOf("id" to "mem_sample_01", "familyId" to "fam_sample_01", "familyName" to "আব্দুর রহমান পরিবার", "memberName" to "মোঃ আব্দুর রহমান", "relation" to "প্রধান", "occupation" to "শিক্ষক", "head" to true, "active" to true, "createdBy" to uid),
                    mapOf("id" to "mem_sample_02", "familyId" to "fam_sample_02", "familyName" to "কামাল উদ্দিন পরিবার", "memberName" to "মোঃ কামাল উদ্দিন", "relation" to "প্রধান", "occupation" to "ব্যবসায়ী", "head" to true, "active" to true, "createdBy" to uid),
                    mapOf("id" to "mem_sample_03", "familyId" to "fam_sample_03", "familyName" to "হাজী ইসমাইল পরিবার", "memberName" to "হাজী ইসমাইল হোসেন", "relation" to "প্রধান", "occupation" to "সমাজসেবক", "head" to true, "active" to true, "createdBy" to uid),
                    mapOf("id" to "mem_sample_04", "familyId" to "fam_sample_04", "familyName" to "মাহমুদুল হাসান পরিবার", "memberName" to "মাহমুদুল হাসান", "relation" to "প্রধান", "occupation" to "চাকরিজীবী", "head" to true, "active" to true, "createdBy" to uid)
                )))
                put("bank_accounts", backend.listToJsonArray(listOf(
                    mapOf("id" to "acc_cash_main", "accountName" to "মসজিদ ক্যাশ তহবিল", "accountType" to "cash", "balance" to 18500.0, "active" to true, "createdBy" to uid),
                    mapOf("id" to "acc_bkash_main", "accountName" to "বিকাশ মসজিদ তহবিল", "accountType" to "bkash", "accountNumber" to "01711-000999", "balance" to 6200.0, "active" to true, "createdBy" to uid)
                )))
                put("dues", backend.listToJsonArray(listOf(
                    mapOf("id" to "due_sample_01", "title" to "মাসিক মসজিদ উন্নয়ন চাঁদা", "dueType" to "monthly", "amount" to 500.0, "dueMonth" to 11L, "dueYear" to 2026L, "active" to true, "createdBy" to uid)
                )))
                put("payments", backend.listToJsonArray(listOf(
                    mapOf("id" to "pay_sample_01", "familyId" to "fam_sample_01", "familyName" to "আব্দুর রহমান পরিবার", "dueId" to "due_sample_01", "dueTitle" to "মাসিক মসজিদ উন্নয়ন চাঁদা", "amount" to 1500.0, "paymentMethod" to "cash", "receiptNo" to "RCP-1001", "paymentDate" to today, "createdBy" to uid),
                    mapOf("id" to "pay_sample_02", "familyId" to "fam_sample_02", "familyName" to "কামাল উদ্দিন পরিবার", "dueId" to "due_sample_01", "dueTitle" to "মাসিক মসজিদ উন্নয়ন চাঁদা", "amount" to 1000.0, "paymentMethod" to "bkash", "receiptNo" to "RCP-1002", "paymentDate" to "$prevMonth-14", "createdBy" to uid)
                )))
                put("donations", backend.listToJsonArray(listOf(
                    mapOf("id" to "don_sample_01", "donorName" to "Anonymous", "anonymous" to true, "fundType" to "zakat", "amount" to 5000.0, "paymentMethod" to "bkash", "receiptNo" to "DON-5001", "donationDate" to today, "createdBy" to uid)
                )))
                put("expenses", backend.listToJsonArray(listOf(
                    mapOf("id" to "exp_sample_01", "title" to "ইমাম ও মুয়াজ্জিন সম্মানী", "category" to "Imam & Staff", "voucherNo" to "VCH-101", "amount" to 1200.0, "expenseDate" to today, "createdBy" to uid)
                )))
                put("notices", backend.listToJsonArray(listOf(
                    mapOf("id" to "notice_sample_01", "title" to "জুম্মার খুতবা ও মাসিক সভা", "content" to "আগামী শুক্রবার জুম্মার পর মসজিদ কমিটির মাসিক সভা অনুষ্ঠিত হবে।", "category" to "prayer", "pinned" to true, "authorUid" to uid, "authorName" to authorName.ifBlank { "Admin" })
                )))
                put("qurbani_batches", backend.listToJsonArray(listOf(
                    mapOf("id" to "qb_sample_01", "eventName" to "ঈদুল আযহা কুরবানি বণ্টন", "animalInfo" to "১টি গরু", "purchaseCost" to 95000.0, "totalMeatKg" to 120.0, "totalFamilies" to 4L, "distributionPerFamily" to 30.0, "eventDate" to "$thisMonth-01", "status" to "distributing", "createdBy" to uid)
                )))
                put("meat_distributions", backend.listToJsonArray(listOf(
                    mapOf("id" to "qb_sample_01_fam_sample_01", "batchId" to "qb_sample_01", "familyId" to "fam_sample_01", "familyName" to "আব্দুর রহমান পরিবার", "familyCode" to "FAM-001", "allocatedKg" to 30.0, "deliveredKg" to 30.0, "receivedBy" to "আব্দুর", "status" to "delivered", "createdBy" to uid),
                    mapOf("id" to "qb_sample_01_fam_sample_02", "batchId" to "qb_sample_01", "familyId" to "fam_sample_02", "familyName" to "কামাল উদ্দিন পরিবার", "familyCode" to "FAM-002", "allocatedKg" to 30.0, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid),
                    mapOf("id" to "qb_sample_01_fam_sample_03", "batchId" to "qb_sample_01", "familyId" to "fam_sample_03", "familyName" to "হাজী ইসমাইল পরিবার", "familyCode" to "FAM-003", "allocatedKg" to 30.0, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid),
                    mapOf("id" to "qb_sample_01_fam_sample_04", "batchId" to "qb_sample_01", "familyId" to "fam_sample_04", "familyName" to "মাহমুদুল হাসান পরিবার", "familyCode" to "FAM-004", "allocatedKg" to 30.0, "deliveredKg" to 0.0, "status" to "pending", "createdBy" to uid)
                )))
            }
            backend.postRestore(root).getOrThrow()
            backend.logAudit("SEED_SAMPLE_DATA", "system", "seed", "Seeded sample local server records")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackupJson(): Result<String> {
        return try {
            backend.requireUid()
            backend.refreshSnapshot()
            Result.success(LocalServerJsonCodec.exportSnapshotToJson(backend.snapshot.value))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Unit> {
        return try {
            backend.requireUid()
            backend.postRestore(JSONObject(jsonString)).getOrThrow()
            backend.logAudit("RESTORE_BACKUP", "system", "json", "Restored backup to local server")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
