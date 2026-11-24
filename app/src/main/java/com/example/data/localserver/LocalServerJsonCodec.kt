package com.example.data.localserver

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
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LocalSnapshotData(
    val mosqueSchedule: MosqueSchedule = MosqueSchedule(),
    val publicSummary: PublicSummary = PublicSummary(),
    val families: List<Family> = emptyList(),
    val members: List<SocietyMember> = emptyList(),
    val dues: List<DueItem> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val donations: List<DonationRecord> = emptyList(),
    val expenses: List<ExpenseRecord> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val budgets: List<BudgetItem> = emptyList(),
    val qurbaniBatches: List<QurbaniBatch> = emptyList(),
    val meatDistributions: List<MeatDistribution> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val committees: List<CommitteeMember> = emptyList(),
    val meetings: List<MeetingRecord> = emptyList(),
    val events: List<SocietyEvent> = emptyList(),
    val assets: List<SocietyAsset> = emptyList(),
    val documents: List<SocietyDocument> = emptyList(),
    val welfareRecords: List<WelfareRecord> = emptyList(),
    val complaints: List<ComplaintItem> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val profiles: List<UserProfile> = emptyList()
)

internal object LocalServerJsonCodec {

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val s = optString(key, "").trim()
        return s.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun JSONObject.optDoubleSafe(key: String, default: Double = 0.0): Double {
        if (!has(key) || isNull(key)) return default
        val v = opt(key)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun JSONObject.optLongSafe(key: String, default: Long = 0L): Long {
        if (!has(key) || isNull(key)) return default
        val v = opt(key)
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun JSONObject.optTimestamp(key: String): Timestamp? {
        if (!has(key) || isNull(key)) return null
        val raw = opt(key)
        return when (raw) {
            is Number -> {
                val n = raw.toLong()
                if (n > 10_000_000_000L) Timestamp(Date(n)) else Timestamp(n, 0)
            }
            is JSONObject -> {
                val sec = raw.optLongSafe("seconds", raw.optLongSafe("_seconds", -1L))
                if (sec >= 0L) Timestamp(sec, 0) else null
            }
            is String -> {
                val numeric = raw.toLongOrNull()
                if (numeric != null) {
                    if (numeric > 10_000_000_000L) Timestamp(Date(numeric)) else Timestamp(numeric, 0)
                } else null
            }
            else -> null
        }
    }

    private fun <T> JSONObject.parseArray(key: String, mapper: (JSONObject) -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        val out = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(mapper(obj))
        }
        return out
    }

    fun parseSchedule(obj: JSONObject?): MosqueSchedule {
        if (obj == null) return MosqueSchedule()
        return MosqueSchedule(
            id = obj.optNullableString("id") ?: "main",
            mosqueName = obj.optNullableString("mosqueName") ?: "বায়তুল আমান জামে মসজিদ",
            fajrTime = obj.optNullableString("fajrTime") ?: "05:15 AM",
            sunriseTime = obj.optNullableString("sunriseTime"),
            dhuhrTime = obj.optNullableString("dhuhrTime") ?: "01:15 PM",
            asrTime = obj.optNullableString("asrTime") ?: "04:15 PM",
            maghribTime = obj.optNullableString("maghribTime") ?: "05:30 PM",
            ishaTime = obj.optNullableString("ishaTime") ?: "07:30 PM",
            jumuahTime = obj.optNullableString("jumuahTime") ?: "01:30 PM",
            khutbahTopic = obj.optNullableString("khutbahTopic"),
            sehriEnd = obj.optNullableString("sehriEnd"),
            iftarTime = obj.optNullableString("iftarTime"),
            imamName = obj.optNullableString("imamName"),
            imamPhone = obj.optNullableString("imamPhone"),
            muazzinName = obj.optNullableString("muazzinName"),
            muazzinPhone = obj.optNullableString("muazzinPhone"),
            updatedBy = obj.optNullableString("updatedBy"),
            updatedAt = obj.optTimestamp("updatedAt")
        )
    }

    fun parseSnapshot(root: JSONObject): LocalSnapshotData {
        val schedObj = when (val raw = root.opt("mosque_schedule")) {
            is JSONObject -> raw
            is JSONArray -> raw.optJSONObject(0)
            else -> null
        }
        val schedule = parseSchedule(schedObj)
        val families = root.parseArray("families") { o ->
            Family(
                id = o.optString("id"),
                familyName = o.optString("familyName"),
                phone = o.optNullableString("phone"),
                address = o.optNullableString("address"),
                familyCode = o.optString("familyCode"),
                linkedEmail = o.optNullableString("linkedEmail"),
                active = o.optBoolean("active", true),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val members = root.parseArray("members") { o ->
            SocietyMember(
                id = o.optString("id"),
                familyId = o.optString("familyId"),
                familyName = o.optString("familyName"),
                memberName = o.optString("memberName"),
                phone = o.optNullableString("phone"),
                relation = o.optNullableString("relation"),
                occupation = o.optNullableString("occupation"),
                head = o.optBoolean("head", false),
                active = o.optBoolean("active", true),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val dues = root.parseArray("dues") { o ->
            DueItem(
                id = o.optString("id"),
                title = o.optString("title"),
                dueType = o.optNullableString("dueType") ?: "monthly",
                amount = o.optDoubleSafe("amount"),
                dueMonth = o.optLongSafe("dueMonth"),
                dueYear = o.optLongSafe("dueYear", 2026L),
                active = o.optBoolean("active", true),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val payments = root.parseArray("payments") { o ->
            PaymentRecord(
                id = o.optString("id"),
                familyId = o.optString("familyId"),
                familyName = o.optString("familyName"),
                dueId = o.optNullableString("dueId"),
                dueTitle = o.optNullableString("dueTitle"),
                accountId = o.optNullableString("accountId"),
                accountName = o.optNullableString("accountName"),
                amount = o.optDoubleSafe("amount"),
                paymentMethod = o.optNullableString("paymentMethod") ?: "cash",
                receiptNo = o.optString("receiptNo"),
                paymentDate = o.optString("paymentDate"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val donations = root.parseArray("donations") { o ->
            DonationRecord(
                id = o.optString("id"),
                donorName = o.optNullableString("donorName") ?: "Donor",
                anonymous = o.optBoolean("anonymous", false),
                fundType = o.optNullableString("fundType") ?: "general",
                amount = o.optDoubleSafe("amount"),
                paymentMethod = o.optNullableString("paymentMethod") ?: "cash",
                transactionRef = o.optNullableString("transactionRef"),
                accountId = o.optNullableString("accountId"),
                accountName = o.optNullableString("accountName"),
                receiptNo = o.optString("receiptNo"),
                donationDate = o.optString("donationDate"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val expenses = root.parseArray("expenses") { o ->
            ExpenseRecord(
                id = o.optString("id"),
                title = o.optString("title"),
                category = o.optNullableString("category") ?: "Other",
                voucherNo = o.optNullableString("voucherNo"),
                accountId = o.optNullableString("accountId"),
                accountName = o.optNullableString("accountName"),
                amount = o.optDoubleSafe("amount"),
                expenseDate = o.optString("expenseDate"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val bankAccounts = root.parseArray("bank_accounts") { o ->
            BankAccount(
                id = o.optString("id"),
                accountName = o.optString("accountName"),
                accountType = o.optNullableString("accountType") ?: "cash",
                accountNumber = o.optNullableString("accountNumber"),
                balance = o.optDoubleSafe("balance"),
                active = o.optBoolean("active", true),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val budgets = root.parseArray("budgets") { o ->
            BudgetItem(
                id = o.optString("id"),
                category = o.optString("category"),
                periodLabel = o.optString("periodLabel"),
                budgetAmount = o.optDoubleSafe("budgetAmount"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val qurbaniBatches = root.parseArray("qurbani_batches") { o ->
            QurbaniBatch(
                id = o.optString("id"),
                eventName = o.optString("eventName"),
                animalInfo = o.optNullableString("animalInfo"),
                purchaseCost = if (o.has("purchaseCost") && !o.isNull("purchaseCost")) o.optDoubleSafe("purchaseCost") else null,
                totalMeatKg = o.optDoubleSafe("totalMeatKg"),
                totalFamilies = o.optLongSafe("totalFamilies"),
                distributionPerFamily = o.optDoubleSafe("distributionPerFamily"),
                eventDate = o.optString("eventDate"),
                status = o.optNullableString("status") ?: "distributing",
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val meatDistributions = root.parseArray("meat_distributions") { o ->
            MeatDistribution(
                id = o.optString("id"),
                batchId = o.optString("batchId"),
                familyId = o.optString("familyId"),
                familyName = o.optString("familyName"),
                familyCode = o.optString("familyCode"),
                allocatedKg = o.optDoubleSafe("allocatedKg"),
                deliveredKg = o.optDoubleSafe("deliveredKg"),
                receivedBy = o.optNullableString("receivedBy"),
                receivedAt = o.optTimestamp("receivedAt"),
                status = o.optNullableString("status") ?: "pending",
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val notices = root.parseArray("notices") { o ->
            Notice(
                id = o.optString("id"),
                title = o.optString("title"),
                content = o.optString("content"),
                category = o.optNullableString("category") ?: "general",
                pinned = o.optBoolean("pinned", false),
                authorUid = o.optString("authorUid"),
                authorName = o.optNullableString("authorName") ?: "Admin",
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val committees = root.parseArray("committees") { o ->
            CommitteeMember(
                id = o.optString("id"),
                name = o.optString("name"),
                role = o.optString("role"),
                phone = o.optNullableString("phone"),
                familyId = o.optNullableString("familyId"),
                familyName = o.optNullableString("familyName"),
                term = o.optNullableString("term"),
                active = o.optBoolean("active", true),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val meetings = root.parseArray("meetings") { o ->
            MeetingRecord(
                id = o.optString("id"),
                title = o.optString("title"),
                meetingDate = o.optString("meetingDate"),
                meetingTime = o.optNullableString("meetingTime"),
                location = o.optNullableString("location"),
                agenda = o.optNullableString("agenda"),
                attendees = o.optNullableString("attendees"),
                decisions = o.optString("decisions"),
                assignedTo = o.optNullableString("assignedTo"),
                deadline = o.optNullableString("deadline"),
                status = o.optNullableString("status") ?: "scheduled",
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val events = root.parseArray("events") { o ->
            SocietyEvent(
                id = o.optString("id"),
                title = o.optString("title"),
                eventDate = o.optString("eventDate"),
                eventTime = o.optNullableString("eventTime"),
                location = o.optNullableString("location"),
                eventType = o.optNullableString("eventType") ?: "religious",
                description = o.optNullableString("description"),
                rsvpEnabled = o.optBoolean("rsvpEnabled", false),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val assets = root.parseArray("assets") { o ->
            SocietyAsset(
                id = o.optString("id"),
                name = o.optString("name"),
                category = o.optNullableString("category") ?: "General",
                value = o.optDoubleSafe("value"),
                purchaseDate = o.optNullableString("purchaseDate"),
                condition = o.optNullableString("condition") ?: "good",
                location = o.optNullableString("location"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val documents = root.parseArray("documents") { o ->
            SocietyDocument(
                id = o.optString("id"),
                title = o.optString("title"),
                category = o.optNullableString("category") ?: "General",
                docDate = o.optString("docDate"),
                fileName = o.optNullableString("fileName"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val welfare = root.parseArray("welfare_records") { o ->
            WelfareRecord(
                id = o.optString("id"),
                recordType = o.optNullableString("recordType") ?: "medical",
                beneficiaryName = o.optString("beneficiaryName"),
                phone = o.optNullableString("phone"),
                plotNumber = o.optNullableString("plotNumber"),
                amount = o.optDoubleSafe("amount"),
                accountId = o.optNullableString("accountId"),
                accountName = o.optNullableString("accountName"),
                recordDate = o.optString("recordDate"),
                note = o.optNullableString("note"),
                createdBy = o.optString("createdBy"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val complaints = root.parseArray("complaints") { o ->
            ComplaintItem(
                id = o.optString("id"),
                authorUid = o.optString("authorUid"),
                authorName = o.optNullableString("authorName") ?: "Member",
                familyId = o.optNullableString("familyId"),
                subject = o.optString("subject"),
                message = o.optString("message"),
                status = o.optNullableString("status") ?: "open",
                adminReply = o.optNullableString("adminReply"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }
        val auditLogs = root.parseArray("audit_logs") { o ->
            AuditLogEntry(
                id = o.optString("id"),
                userId = o.optString("userId"),
                userEmail = o.optNullableString("userEmail"),
                action = o.optString("action"),
                tableName = o.optString("tableName"),
                recordId = o.optNullableString("recordId"),
                summary = o.optNullableString("summary"),
                beforeSummary = o.optNullableString("beforeSummary"),
                afterSummary = o.optNullableString("afterSummary"),
                createdAt = o.optTimestamp("createdAt")
            )
        }
        val profiles = root.parseArray("profiles") { o ->
            UserProfile(
                id = o.optString("id"),
                userId = o.optNullableString("userId") ?: o.optString("id"),
                fullName = o.optNullableString("fullName") ?: "Member",
                email = o.optNullableString("email") ?: "",
                role = o.optNullableString("role") ?: SocietyRoles.MEMBER,
                linkedFamilyId = o.optNullableString("linkedFamilyId"),
                createdAt = o.optTimestamp("createdAt"),
                updatedAt = o.optTimestamp("updatedAt")
            )
        }

        val summary = computeSummary(
            families = families,
            members = members,
            dues = dues,
            payments = payments,
            donations = donations,
            expenses = expenses,
            welfare = welfare,
            batches = qurbaniBatches,
            distributions = meatDistributions
        )

        return LocalSnapshotData(
            mosqueSchedule = schedule,
            publicSummary = summary,
            families = families,
            members = members,
            dues = dues,
            payments = payments,
            donations = donations,
            expenses = expenses,
            bankAccounts = bankAccounts,
            budgets = budgets,
            qurbaniBatches = qurbaniBatches,
            meatDistributions = meatDistributions,
            notices = notices,
            committees = committees,
            meetings = meetings,
            events = events,
            assets = assets,
            documents = documents,
            welfareRecords = welfare,
            complaints = complaints,
            auditLogs = auditLogs,
            profiles = profiles
        )
    }

    fun computeSummary(
        families: List<Family>,
        members: List<SocietyMember>,
        dues: List<DueItem>,
        payments: List<PaymentRecord>,
        donations: List<DonationRecord>,
        expenses: List<ExpenseRecord>,
        welfare: List<WelfareRecord>,
        batches: List<QurbaniBatch>,
        distributions: List<MeatDistribution>
    ): PublicSummary {
        val activeFamilies = families.filter { it.active }
        val activeDuesTotal = dues.filter { it.active }.sumOf { it.amount }
        val paidByFamily = payments.groupBy { it.familyId }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val totalOutstanding = activeFamilies.sumOf { fam ->
            maxOf(0.0, activeDuesTotal - (paidByFamily[fam.id] ?: 0.0))
        }
        val latestQurbani = batches.maxByOrNull { it.createdAt?.seconds ?: 0L }
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

        return PublicSummary(
            id = "overview",
            totalFamilies = activeFamilies.size.toLong(),
            totalMembers = members.count { it.active }.toLong(),
            totalCollection = payments.sumOf { it.amount },
            totalDonations = donations.sumOf { it.amount },
            totalExpense = expenses.sumOf { it.amount },
            totalWelfare = welfare.sumOf { it.amount },
            totalOutstanding = totalOutstanding,
            totalQurbaniMeatKg = batches.sumOf { it.totalMeatKg },
            totalDistributions = distributions.size.toLong(),
            deliveredDistributions = distributions.count { it.status == "delivered" }.toLong(),
            latestQurbaniEvent = latestQurbani?.eventName,
            latestTotalMeatKg = latestQurbani?.totalMeatKg,
            latestPerFamilyKg = latestQurbani?.distributionPerFamily,
            recentMonths = monthKeys,
            recentCollections = recentCollections,
            recentExpenses = recentExpenses
        )
    }

    fun exportSnapshotToJson(snap: LocalSnapshotData): String {
        val root = JSONObject()
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        root.put("version", 3)

        val schedArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "main")
                put("mosqueName", snap.mosqueSchedule.mosqueName)
                put("fajrTime", snap.mosqueSchedule.fajrTime)
                snap.mosqueSchedule.sunriseTime?.let { put("sunriseTime", it) }
                put("dhuhrTime", snap.mosqueSchedule.dhuhrTime)
                put("asrTime", snap.mosqueSchedule.asrTime)
                put("maghribTime", snap.mosqueSchedule.maghribTime)
                put("ishaTime", snap.mosqueSchedule.ishaTime)
                put("jumuahTime", snap.mosqueSchedule.jumuahTime)
                snap.mosqueSchedule.khutbahTopic?.let { put("khutbahTopic", it) }
                snap.mosqueSchedule.sehriEnd?.let { put("sehriEnd", it) }
                snap.mosqueSchedule.iftarTime?.let { put("iftarTime", it) }
                snap.mosqueSchedule.imamName?.let { put("imamName", it) }
                snap.mosqueSchedule.imamPhone?.let { put("imamPhone", it) }
                snap.mosqueSchedule.muazzinName?.let { put("muazzinName", it) }
                snap.mosqueSchedule.muazzinPhone?.let { put("muazzinPhone", it) }
            })
        }
        root.put("mosque_schedule", schedArr)
        root.put("families", JSONArray().apply {
            snap.families.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id); put("familyName", f.familyName); put("familyCode", f.familyCode); put("active", f.active)
                    f.phone?.let { put("phone", it) }; f.address?.let { put("address", it) }; f.linkedEmail?.let { put("linkedEmail", it) }
                })
            }
        })
        root.put("members", JSONArray().apply {
            snap.members.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("familyId", m.familyId); put("familyName", m.familyName); put("memberName", m.memberName)
                    put("head", m.head); put("active", m.active)
                    m.phone?.let { put("phone", it) }; m.relation?.let { put("relation", it) }; m.occupation?.let { put("occupation", it) }
                })
            }
        })
        root.put("dues", JSONArray().apply {
            snap.dues.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("title", d.title); put("dueType", d.dueType); put("amount", d.amount)
                    put("dueMonth", d.dueMonth); put("dueYear", d.dueYear); put("active", d.active)
                })
            }
        })
        root.put("bank_accounts", JSONArray().apply {
            snap.bankAccounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("accountName", a.accountName); put("accountType", a.accountType)
                    put("balance", a.balance); put("active", a.active); a.accountNumber?.let { put("accountNumber", it) }
                })
            }
        })
        root.put("budgets", JSONArray().apply {
            snap.budgets.forEach { b ->
                put(JSONObject().apply { put("id", b.id); put("category", b.category); put("periodLabel", b.periodLabel); put("budgetAmount", b.budgetAmount) })
            }
        })
        root.put("payments", JSONArray().apply {
            snap.payments.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("familyId", p.familyId); put("familyName", p.familyName); put("amount", p.amount)
                    put("paymentMethod", p.paymentMethod); put("receiptNo", p.receiptNo); put("paymentDate", p.paymentDate)
                    p.dueId?.let { put("dueId", it) }; p.dueTitle?.let { put("dueTitle", it) }
                    p.accountId?.let { put("accountId", it) }; p.accountName?.let { put("accountName", it) }; p.note?.let { put("note", it) }
                })
            }
        })
        root.put("donations", JSONArray().apply {
            snap.donations.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("donorName", d.donorName); put("anonymous", d.anonymous); put("fundType", d.fundType)
                    put("amount", d.amount); put("paymentMethod", d.paymentMethod); put("receiptNo", d.receiptNo); put("donationDate", d.donationDate)
                    d.transactionRef?.let { put("transactionRef", it) }; d.accountId?.let { put("accountId", it) }; d.accountName?.let { put("accountName", it) }; d.note?.let { put("note", it) }
                })
            }
        })
        root.put("expenses", JSONArray().apply {
            snap.expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("title", e.title); put("category", e.category); put("amount", e.amount); put("expenseDate", e.expenseDate)
                    e.voucherNo?.let { put("voucherNo", it) }; e.accountId?.let { put("accountId", it) }; e.accountName?.let { put("accountName", it) }; e.note?.let { put("note", it) }
                })
            }
        })
        root.put("qurbani_batches", JSONArray().apply {
            snap.qurbaniBatches.forEach { q ->
                put(JSONObject().apply {
                    put("id", q.id); put("eventName", q.eventName); put("totalMeatKg", q.totalMeatKg); put("totalFamilies", q.totalFamilies)
                    put("distributionPerFamily", q.distributionPerFamily); put("eventDate", q.eventDate); put("status", q.status)
                    q.animalInfo?.let { put("animalInfo", it) }; q.purchaseCost?.let { put("purchaseCost", it) }
                })
            }
        })
        root.put("meat_distributions", JSONArray().apply {
            snap.meatDistributions.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("batchId", m.batchId); put("familyId", m.familyId); put("familyName", m.familyName)
                    put("familyCode", m.familyCode); put("allocatedKg", m.allocatedKg); put("deliveredKg", m.deliveredKg); put("status", m.status)
                    m.receivedBy?.let { put("receivedBy", it) }
                })
            }
        })
        root.put("notices", JSONArray().apply {
            snap.notices.forEach { n ->
                put(JSONObject().apply { put("id", n.id); put("title", n.title); put("content", n.content); put("category", n.category); put("pinned", n.pinned); put("authorName", n.authorName) })
            }
        })
        root.put("committees", JSONArray().apply {
            snap.committees.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("role", c.role); put("active", c.active)
                    c.phone?.let { put("phone", it) }; c.familyId?.let { put("familyId", it) }; c.familyName?.let { put("familyName", it) }; c.term?.let { put("term", it) }
                })
            }
        })
        root.put("meetings", JSONArray().apply {
            snap.meetings.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("title", m.title); put("meetingDate", m.meetingDate); put("decisions", m.decisions); put("status", m.status)
                    m.meetingTime?.let { put("meetingTime", it) }; m.location?.let { put("location", it) }; m.agenda?.let { put("agenda", it) }
                    m.attendees?.let { put("attendees", it) }; m.assignedTo?.let { put("assignedTo", it) }; m.deadline?.let { put("deadline", it) }
                })
            }
        })
        root.put("events", JSONArray().apply {
            snap.events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("title", e.title); put("eventDate", e.eventDate); put("eventType", e.eventType); put("rsvpEnabled", e.rsvpEnabled)
                    e.eventTime?.let { put("eventTime", it) }; e.location?.let { put("location", it) }; e.description?.let { put("description", it) }
                })
            }
        })
        root.put("assets", JSONArray().apply {
            snap.assets.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("name", a.name); put("category", a.category); put("value", a.value); put("condition", a.condition)
                    a.purchaseDate?.let { put("purchaseDate", it) }; a.location?.let { put("location", it) }; a.note?.let { put("note", it) }
                })
            }
        })
        root.put("documents", JSONArray().apply {
            snap.documents.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("title", d.title); put("category", d.category); put("docDate", d.docDate)
                    d.fileName?.let { put("fileName", it) }; d.note?.let { put("note", it) }
                })
            }
        })
        root.put("welfare_records", JSONArray().apply {
            snap.welfareRecords.forEach { w ->
                put(JSONObject().apply {
                    put("id", w.id); put("recordType", w.recordType); put("beneficiaryName", w.beneficiaryName); put("amount", w.amount); put("recordDate", w.recordDate)
                    w.phone?.let { put("phone", it) }; w.plotNumber?.let { put("plotNumber", it) }; w.accountId?.let { put("accountId", it) }; w.accountName?.let { put("accountName", it) }; w.note?.let { put("note", it) }
                })
            }
        })
        return root.toString(2)
    }
}
