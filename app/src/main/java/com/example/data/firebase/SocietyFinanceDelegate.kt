package com.example.data.firebase

import com.example.data.model.BankAccount
import com.example.data.model.BudgetItem
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.PaymentRecord
import com.example.data.model.QurbaniBatch
import com.example.data.model.SocietyMember
import com.example.data.model.WelfareRecord
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal class SocietyFinanceDelegate(private val base: FirestoreBase) {

    private val db get() = base.db

    suspend fun createDue(
        title: String,
        dueType: String,
        amount: Double,
        dueMonth: Long,
        dueYear: Long,
        active: Boolean = true
    ): Result<Unit> {
        val cleanTitle = title.trim().take(100)
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Due title is required" }
            require(dueType in listOf("monthly", "yearly", "special")) { "Invalid due type" }
            require(amount >= 0.0) { "Amount cannot be negative" }
            val docRef = db.collection("dues").document()
            val payload = mapOf(
                "title" to cleanTitle,
                "dueType" to dueType,
                "amount" to amount,
                "dueMonth" to dueMonth.coerceIn(0L, 12L),
                "dueYear" to dueYear.coerceIn(2000L, 2100L),
                "active" to active,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            base.logAudit("CREATE_DUE", "dues", docRef.id, "$cleanTitle ($amount)", afterSummary = "$dueType: $amount")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "dues")
            Result.failure(e)
        }
    }

    suspend fun toggleDueActive(dueId: String, active: Boolean): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("dues").document(dueId).update(
                mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            base.logAudit("TOGGLE_DUE", "dues", dueId, "Set active=$active", beforeSummary = "active=${!active}", afterSummary = "active=$active")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "dues/$dueId")
            Result.failure(e)
        }
    }

    suspend fun deleteDue(dueId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("dues").document(dueId).delete().await()
            base.logAudit("DELETE_DUE", "dues", dueId, "Deleted due $dueId")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "dues/$dueId")
            Result.failure(e)
        }
    }

    suspend fun recordPayment(
        familyId: String,
        familyName: String,
        dueId: String?,
        dueTitle: String?,
        amount: Double,
        paymentMethod: String,
        paymentDate: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null
    ): Result<PaymentRecord> {
        return try {
            val uid = base.requireUserId()
            require(familyId.isNotBlank()) { "Select a family" }
            require(amount > 0.0) { "Payment amount must be greater than zero" }
            require(paymentMethod in listOf("cash", "bkash", "nagad", "bank")) { "Invalid payment method" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val receiptNo = "RCP-$dateStamp-${(1000..9999).random()}"
            val docRef = db.collection("payments").document()
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(300)
            val cleanDueId = dueId?.takeIf { it.isNotBlank() }
            val cleanDueTitle = dueTitle?.takeIf { it.isNotBlank() }?.take(100)
            val cleanAccId = accountId?.takeIf { it.isNotBlank() }
            val cleanAccName = accountName?.takeIf { it.isNotBlank() }?.take(100)
            val cleanDate = paymentDate.trim().ifBlank { base.todayIso() }.take(20)
            val payload = mapOf(
                "familyId" to familyId,
                "familyName" to familyName.trim().take(100),
                "dueId" to cleanDueId,
                "dueTitle" to cleanDueTitle,
                "accountId" to cleanAccId,
                "accountName" to cleanAccName,
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "receiptNo" to receiptNo,
                "paymentDate" to cleanDate,
                "note" to cleanNote,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.runTransaction { tx ->
                if (cleanAccId != null) {
                    val accRef = db.collection("bank_accounts").document(cleanAccId)
                    val accSnap = tx.get(accRef)
                    val prevBal = accSnap.getDouble("balance") ?: 0.0
                    tx.update(accRef, mapOf("balance" to (prevBal + amount), "updatedAt" to FieldValue.serverTimestamp()))
                }
                tx.set(docRef, payload)
            }.await()

            base.logAudit("RECORD_PAYMENT", "payments", docRef.id, "$receiptNo: $familyName - $amount", afterSummary = "$paymentMethod: $amount")
            refreshPublicSummary()
            Result.success(
                PaymentRecord(
                    id = docRef.id, familyId = familyId, familyName = familyName,
                    dueId = cleanDueId, dueTitle = cleanDueTitle, accountId = cleanAccId,
                    accountName = cleanAccName, amount = amount, paymentMethod = paymentMethod,
                    receiptNo = receiptNo, paymentDate = cleanDate, note = cleanNote, createdBy = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "payments")
            Result.failure(e)
        }
    }

    suspend fun deletePayment(paymentId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("payments").document(paymentId).delete().await()
            base.logAudit("DELETE_PAYMENT", "payments", paymentId, "Deleted payment $paymentId")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "payments/$paymentId")
            Result.failure(e)
        }
    }

    suspend fun recordDonation(
        donorName: String,
        anonymous: Boolean,
        fundType: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String?,
        donationDate: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null
    ): Result<DonationRecord> {
        return try {
            val uid = base.requireUserId()
            val cleanDonor = if (anonymous) "Anonymous" else donorName.trim().ifBlank { "Donor" }.take(100)
            require(amount > 0.0) { "Donation amount must be greater than zero" }
            require(fundType in listOf("general", "zakat", "fitrah", "sadqah", "construction", "orphan_welfare")) { "Invalid fund type" }
            require(paymentMethod in listOf("cash", "bkash", "nagad", "bank")) { "Invalid payment method" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val receiptNo = "DON-$dateStamp-${(1000..9999).random()}"
            val docRef = db.collection("donations").document()
            val cleanRef = transactionRef?.trim()?.takeIf { it.isNotBlank() }?.take(80)
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(300)
            val cleanAccId = accountId?.takeIf { it.isNotBlank() }
            val cleanAccName = accountName?.takeIf { it.isNotBlank() }?.take(100)
            val cleanDate = donationDate.trim().ifBlank { base.todayIso() }.take(20)
            val payload = mapOf(
                "donorName" to cleanDonor,
                "anonymous" to anonymous,
                "fundType" to fundType,
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "transactionRef" to cleanRef,
                "accountId" to cleanAccId,
                "accountName" to cleanAccName,
                "receiptNo" to receiptNo,
                "donationDate" to cleanDate,
                "note" to cleanNote,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.runTransaction { tx ->
                if (cleanAccId != null) {
                    val accRef = db.collection("bank_accounts").document(cleanAccId)
                    val accSnap = tx.get(accRef)
                    val prevBal = accSnap.getDouble("balance") ?: 0.0
                    tx.update(accRef, mapOf("balance" to (prevBal + amount), "updatedAt" to FieldValue.serverTimestamp()))
                }
                tx.set(docRef, payload)
            }.await()

            base.logAudit("RECORD_DONATION", "donations", docRef.id, "$receiptNo: $cleanDonor ($fundType) - $amount", afterSummary = "$fundType: $amount")
            refreshPublicSummary()
            Result.success(
                DonationRecord(
                    id = docRef.id, donorName = cleanDonor, anonymous = anonymous, fundType = fundType,
                    amount = amount, paymentMethod = paymentMethod, transactionRef = cleanRef,
                    accountId = cleanAccId, accountName = cleanAccName, receiptNo = receiptNo,
                    donationDate = cleanDate, note = cleanNote, createdBy = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "donations")
            Result.failure(e)
        }
    }

    suspend fun deleteDonation(donationId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("donations").document(donationId).delete().await()
            base.logAudit("DELETE_DONATION", "donations", donationId, "Deleted donation $donationId")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "donations/$donationId")
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        title: String,
        category: String,
        amount: Double,
        expenseDate: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanCategory = category.trim().ifBlank { "Other" }.take(50)
        val cleanDate = expenseDate.trim().ifBlank { base.todayIso() }.take(20)
        val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(300)
        val cleanAccId = accountId?.takeIf { it.isNotBlank() }
        val cleanAccName = accountName?.takeIf { it.isNotBlank() }?.take(100)
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Expense title is required" }
            require(amount > 0.0) { "Expense amount must be greater than zero" }
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val voucherNo = "VCH-$dateStamp-${(1000..9999).random()}"
            val docRef = db.collection("expenses").document()
            val payload = mapOf(
                "title" to cleanTitle,
                "category" to cleanCategory,
                "voucherNo" to voucherNo,
                "accountId" to cleanAccId,
                "accountName" to cleanAccName,
                "amount" to amount,
                "expenseDate" to cleanDate,
                "note" to cleanNote,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.runTransaction { tx ->
                if (cleanAccId != null) {
                    val accRef = db.collection("bank_accounts").document(cleanAccId)
                    val accSnap = tx.get(accRef)
                    val prevBal = accSnap.getDouble("balance") ?: 0.0
                    tx.update(accRef, mapOf("balance" to (prevBal - amount), "updatedAt" to FieldValue.serverTimestamp()))
                }
                tx.set(docRef, payload)
            }.await()

            base.logAudit("CREATE_EXPENSE", "expenses", docRef.id, "$voucherNo: $cleanTitle ($amount)")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "expenses")
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("expenses").document(expenseId).delete().await()
            base.logAudit("DELETE_EXPENSE", "expenses", expenseId, "Deleted expense $expenseId")
            refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "expenses/$expenseId")
            Result.failure(e)
        }
    }

    suspend fun saveBankAccount(
        accountId: String?,
        accountName: String,
        accountType: String,
        accountNumber: String?,
        balance: Double,
        active: Boolean
    ): Result<Unit> {
        val cleanName = accountName.trim().take(100)
        val cleanNumber = accountNumber?.trim()?.takeIf { it.isNotBlank() }?.take(50)
        return try {
            val uid = base.requireUserId()
            require(cleanName.isNotEmpty()) { "Account name is required" }
            require(accountType in listOf("cash", "bank", "bkash", "nagad")) { "Invalid account type" }
            val col = db.collection("bank_accounts")
            val docRef = if (accountId.isNullOrBlank()) col.document() else col.document(accountId)
            if (accountId.isNullOrBlank()) {
                val payload = mapOf(
                    "accountName" to cleanName,
                    "accountType" to accountType,
                    "accountNumber" to cleanNumber,
                    "balance" to balance,
                    "active" to active,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_ACCOUNT", "bank_accounts", docRef.id, "$cleanName ($accountType)", afterSummary = "balance=$balance")
            } else {
                val updates = mutableMapOf<String, Any>(
                    "accountName" to cleanName,
                    "accountType" to accountType,
                    "accountNumber" to (cleanNumber ?: FieldValue.delete()),
                    "balance" to balance,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                base.logAudit("UPDATE_ACCOUNT", "bank_accounts", docRef.id, cleanName, afterSummary = "balance=$balance")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "bank_accounts/${accountId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteBankAccount(accountId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("bank_accounts").document(accountId).delete().await()
            base.logAudit("DELETE_ACCOUNT", "bank_accounts", accountId, "Deleted bank account $accountId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "bank_accounts/$accountId")
            Result.failure(e)
        }
    }

    suspend fun saveBudget(
        budgetId: String?,
        category: String,
        periodLabel: String,
        budgetAmount: Double
    ): Result<Unit> {
        val cleanCat = category.trim().take(50)
        val cleanPeriod = periodLabel.trim().take(30)
        return try {
            val uid = base.requireUserId()
            require(cleanCat.isNotEmpty()) { "Category is required" }
            require(cleanPeriod.isNotEmpty()) { "Period is required" }
            require(budgetAmount >= 0.0) { "Budget must be non-negative" }
            val col = db.collection("budgets")
            val docRef = if (budgetId.isNullOrBlank()) col.document() else col.document(budgetId)
            if (budgetId.isNullOrBlank()) {
                val payload = mapOf(
                    "category" to cleanCat,
                    "periodLabel" to cleanPeriod,
                    "budgetAmount" to budgetAmount,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                base.logAudit("CREATE_BUDGET", "budgets", docRef.id, "$cleanCat ($cleanPeriod): $budgetAmount")
            } else {
                docRef.update(
                    mapOf(
                        "category" to cleanCat,
                        "periodLabel" to cleanPeriod,
                        "budgetAmount" to budgetAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_BUDGET", "budgets", docRef.id, "$cleanCat ($cleanPeriod): $budgetAmount")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "budgets/${budgetId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("budgets").document(budgetId).delete().await()
            base.logAudit("DELETE_BUDGET", "budgets", budgetId, "Deleted budget $budgetId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "budgets/$budgetId")
            Result.failure(e)
        }
    }

    suspend fun refreshPublicSummary() {
        try {
            val families = db.collection("families").get().await().toObjects(Family::class.java)
            val activeFamilies = families.filter { it.active }
            val activeFamiliesCount = activeFamilies.size.toLong()

            val activeMembersCount = db.collection("members").get().await()
                .toObjects(SocietyMember::class.java).count { it.active }.toLong()

            val dues = db.collection("dues").get().await().toObjects(DueItem::class.java)
            val activeDuesTotal = dues.filter { it.active }.sumOf { it.amount }

            val payments = db.collection("payments").get().await().toObjects(PaymentRecord::class.java)
            val totalCollection = payments.sumOf { it.amount }

            val donations = db.collection("donations").get().await().toObjects(DonationRecord::class.java)
            val totalDonations = donations.sumOf { it.amount }

            val paidByFamily = payments.groupBy { it.familyId }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val totalOutstanding = activeFamilies.sumOf { fam ->
                maxOf(0.0, activeDuesTotal - (paidByFamily[fam.id] ?: 0.0))
            }

            val expenses = db.collection("expenses").get().await().toObjects(ExpenseRecord::class.java)
            val totalExpense = expenses.sumOf { it.amount }

            val welfare = db.collection("welfare_records").get().await().toObjects(WelfareRecord::class.java)
            val totalWelfare = welfare.sumOf { it.amount }

            val batches = db.collection("qurbani_batches").get().await().toObjects(QurbaniBatch::class.java)
            val latestQurbani = batches.maxByOrNull { it.createdAt?.seconds ?: 0L }
            val totalQurbaniMeatKg = batches.sumOf { it.totalMeatKg }

            val distributions = db.collection("meat_distributions").get().await().toObjects(MeatDistribution::class.java)
            val totalDistributions = distributions.size.toLong()
            val deliveredDistributions = distributions.count { it.status == "delivered" }.toLong()

            val ymFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthKeys = mutableListOf<String>()
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -5)
            repeat(6) {
                monthKeys.add(ymFormat.format(cal.time))
                cal.add(Calendar.MONTH, 1)
            }
            val recentCollections = monthKeys.map { ym ->
                payments.filter { it.paymentDate.startsWith(ym) }.sumOf { it.amount } +
                    donations.filter { it.donationDate.startsWith(ym) }.sumOf { it.amount }
            }
            val recentExpenses = monthKeys.map { ym ->
                expenses.filter { it.expenseDate.startsWith(ym) }.sumOf { it.amount } +
                    welfare.filter { it.recordDate.startsWith(ym) }.sumOf { it.amount }
            }

            val payload = mapOf(
                "totalFamilies" to activeFamiliesCount,
                "totalMembers" to activeMembersCount,
                "totalCollection" to totalCollection,
                "totalDonations" to totalDonations,
                "totalExpense" to totalExpense,
                "totalWelfare" to totalWelfare,
                "totalOutstanding" to totalOutstanding,
                "totalQurbaniMeatKg" to totalQurbaniMeatKg,
                "totalDistributions" to totalDistributions,
                "deliveredDistributions" to deliveredDistributions,
                "latestQurbaniEvent" to latestQurbani?.eventName,
                "latestTotalMeatKg" to latestQurbani?.totalMeatKg,
                "latestPerFamilyKg" to latestQurbani?.distributionPerFamily,
                "recentMonths" to monthKeys,
                "recentCollections" to recentCollections,
                "recentExpenses" to recentExpenses,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("public_summary").document("overview").set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "public_summary/overview")
        }
    }
}
