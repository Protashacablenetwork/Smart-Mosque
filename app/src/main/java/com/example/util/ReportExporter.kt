package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AuditLogEntry
import com.example.data.model.DonationRecord
import com.example.data.model.ExpenseRecord
import com.example.data.model.MeatDistribution
import com.example.data.model.Notice
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicSummary
import com.example.data.model.QurbaniBatch
import com.example.data.model.WelfareRecord
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private fun reportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun exportPublicSummaryPdf(
        context: Context,
        summary: PublicSummary,
        notices: List<Notice>,
        payments: List<PaymentRecord>,
        expenses: List<ExpenseRecord>,
        donations: List<DonationRecord> = emptyList(),
        welfareRecords: List<WelfareRecord> = emptyList()
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0A7D52")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 96f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D0F0E3")
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText("Masjid & Society Financial Report", 36f, 44f, titlePaint)
        val generatedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("Generated: $generatedDate", 36f, 68f, subtitlePaint)

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0A7D52")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#122019")
            textSize = 11f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E3EBE6")
            strokeWidth = 1f
        }

        val nf = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }
        var y = 124f

        canvas.drawText("1. Public Society Overview", 36f, y, sectionPaint)
        y += 20f
        canvas.drawText("Active Families: ${summary.totalFamilies}   |   Active Members: ${summary.totalMembers}", 44f, y, bodyPaint); y += 17f
        canvas.drawText("Dues Collected: BDT ${nf.format(summary.totalCollection)}   |   Donations: BDT ${nf.format(summary.totalDonations)}", 44f, y, bodyPaint); y += 17f
        canvas.drawText("Masjid Expenses: BDT ${nf.format(summary.totalExpense)}   |   Social Welfare: BDT ${nf.format(summary.totalWelfare)}", 44f, y, bodyPaint); y += 17f
        canvas.drawText("Net Fund Balance: BDT ${nf.format(summary.balance)}   |   Outstanding Dues: BDT ${nf.format(summary.totalOutstanding)}", 44f, y, bodyPaint); y += 17f
        canvas.drawText("Qurbani Total: ${summary.totalQurbaniMeatKg} kg   |   Delivered: ${summary.deliveredDistributions}/${summary.totalDistributions}", 44f, y, bodyPaint); y += 20f
        canvas.drawLine(36f, y, 559f, y, linePaint); y += 20f

        if (summary.recentMonths.isNotEmpty()) {
            canvas.drawText("2. Last 6 Months Cashflow", 36f, y, sectionPaint); y += 18f
            summary.recentMonths.forEachIndexed { idx, month ->
                val col = summary.recentCollections.getOrElse(idx) { 0.0 }
                val exp = summary.recentExpenses.getOrElse(idx) { 0.0 }
                canvas.drawText("$month  ->  Income: BDT ${nf.format(col)}  |  Outflow: BDT ${nf.format(exp)}", 44f, y, bodyPaint)
                y += 15f
            }
            y += 8f
            canvas.drawLine(36f, y, 559f, y, linePaint); y += 20f
        }

        if (!summary.latestQurbaniEvent.isNullOrBlank() && y < 680f) {
            canvas.drawText("3. Latest Qurbani Meat Distribution", 36f, y, sectionPaint); y += 18f
            canvas.drawText("Event: ${summary.latestQurbaniEvent} | Total: ${summary.latestTotalMeatKg ?: 0.0} kg | Share: ${summary.latestPerFamilyKg ?: 0.0} kg/family", 44f, y, bodyPaint)
            y += 20f
            canvas.drawLine(36f, y, 559f, y, linePaint); y += 20f
        }

        if (payments.isNotEmpty() && y < 700f) {
            canvas.drawText("Recent Collections (${payments.size})", 36f, y, sectionPaint); y += 17f
            payments.take(5).forEach { p ->
                if (y < 780f) {
                    canvas.drawText("${p.paymentDate} | ${p.receiptNo} | ${p.familyName} | BDT ${nf.format(p.amount)}", 44f, y, bodyPaint)
                    y += 15f
                }
            }
            y += 8f
        }

        if (donations.isNotEmpty() && y < 720f) {
            canvas.drawText("Recent Donations (${donations.size})", 36f, y, sectionPaint); y += 17f
            donations.take(4).forEach { d ->
                if (y < 780f) {
                    val who = if (d.anonymous) "Anonymous" else d.donorName
                    canvas.drawText("${d.donationDate} | ${d.receiptNo} | $who (${d.fundType}) | BDT ${nf.format(d.amount)}", 44f, y, bodyPaint)
                    y += 15f
                }
            }
            y += 8f
        }

        if (expenses.isNotEmpty() && y < 740f) {
            canvas.drawText("Recent Expenses (${expenses.size})", 36f, y, sectionPaint); y += 17f
            expenses.take(4).forEach { e ->
                if (y < 790f) {
                    canvas.drawText("${e.expenseDate} | ${e.voucherNo ?: "-"} | ${e.title} | BDT ${nf.format(e.amount)}", 44f, y, bodyPaint)
                    y += 15f
                }
            }
            y += 8f
        }

        if (welfareRecords.isNotEmpty() && y < 760f) {
            canvas.drawText("Recent Welfare & Burial (${welfareRecords.size})", 36f, y, sectionPaint); y += 17f
            welfareRecords.take(3).forEach { w ->
                if (y < 800f) {
                    canvas.drawText("${w.recordDate} | ${w.recordType} | ${w.beneficiaryName} | BDT ${nf.format(w.amount)}", 44f, y, bodyPaint)
                    y += 15f
                }
            }
        }

        if (notices.isNotEmpty() && y < 780f) {
            canvas.drawText("Latest Notices", 36f, y, sectionPaint); y += 16f
            notices.take(2).forEach { n ->
                if (y < 815f) {
                    canvas.drawText("- ${n.title}", 44f, y, bodyPaint)
                    y += 15f
                }
            }
        }

        doc.finishPage(page)
        val outFile = File(reportsDir(context), "masjid_financial_report.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, outFile, "application/pdf", "Share Financial PDF Report")
    }

    fun exportPublicSummaryCsv(
        context: Context,
        summary: PublicSummary,
        payments: List<PaymentRecord>,
        expenses: List<ExpenseRecord>,
        donations: List<DonationRecord> = emptyList(),
        welfareRecords: List<WelfareRecord> = emptyList()
    ) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Section,Detail,Category/Method,Date,Amount/Value")
        sb.appendLine("Summary,Active Families,,,${summary.totalFamilies}")
        sb.appendLine("Summary,Active Members,,,${summary.totalMembers}")
        sb.appendLine("Summary,Total Dues Collection,,,${summary.totalCollection}")
        sb.appendLine("Summary,Total Donations,,,${summary.totalDonations}")
        sb.appendLine("Summary,Total Expense,,,${summary.totalExpense}")
        sb.appendLine("Summary,Total Welfare,,,${summary.totalWelfare}")
        sb.appendLine("Summary,Net Balance,,,${summary.balance}")
        sb.appendLine("Summary,Outstanding Dues,,,${summary.totalOutstanding}")
        sb.appendLine("Summary,Qurbani Total Meat (kg),,,${summary.totalQurbaniMeatKg}")
        sb.appendLine("Summary,Meat Delivered Families,,,${summary.deliveredDistributions}/${summary.totalDistributions}")
        summary.recentMonths.forEachIndexed { idx, month ->
            val col = summary.recentCollections.getOrElse(idx) { 0.0 }
            val exp = summary.recentExpenses.getOrElse(idx) { 0.0 }
            sb.appendLine("Monthly,Income,$month,$month,$col")
            sb.appendLine("Monthly,Outflow,$month,$month,$exp")
        }
        payments.forEach { p ->
            sb.appendLine("Payment,\"${escapeCsv(p.familyName)} (${p.receiptNo})\",${p.paymentMethod},${p.paymentDate},${p.amount}")
        }
        donations.forEach { d ->
            val who = if (d.anonymous) "Anonymous" else d.donorName
            sb.appendLine("Donation,\"${escapeCsv(who)} (${d.receiptNo})\",${d.fundType}/${d.paymentMethod},${d.donationDate},${d.amount}")
        }
        expenses.forEach { e ->
            sb.appendLine("Expense,\"${escapeCsv(e.title)} (${e.voucherNo ?: "-"})\"," +
                "\"${escapeCsv(e.category)}\",${e.expenseDate},${e.amount}")
        }
        welfareRecords.forEach { w ->
            sb.appendLine("Welfare,\"${escapeCsv(w.beneficiaryName)}\",${w.recordType},${w.recordDate},${w.amount}")
        }

        val outFile = File(reportsDir(context), "masjid_financial_report.csv")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, outFile, "text/csv", "Share Excel/CSV Report")
    }

    fun exportQurbaniDistributionPdf(
        context: Context,
        batch: QurbaniBatch,
        distributions: List<MeatDistribution>
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply { color = Color.parseColor("#0A7D52") }
        canvas.drawRect(0f, 0f, 595f, 88f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#D0F0E3")
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText("Qurbani Meat Distribution Sheet", 36f, 40f, titlePaint)
        canvas.drawText("${batch.eventName} | ${batch.eventDate} | Total: ${batch.totalMeatKg} kg | Share: ${batch.distributionPerFamily} kg/family", 36f, 64f, subPaint)

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#122019")
            textSize = 11f
            isAntiAlias = true
        }
        var y = 120f
        distributions.take(36).forEachIndexed { idx, d ->
            val statusStr = if (d.status == "delivered") "Delivered (${d.deliveredKg}kg) - ${d.receivedBy ?: ""}" else "Pending"
            canvas.drawText("${idx + 1}. [${d.familyCode}] ${d.familyName} | Alloc: ${d.allocatedKg}kg | $statusStr", 36f, y, bodyPaint)
            y += 18f
        }

        doc.finishPage(page)
        val outFile = File(reportsDir(context), "qurbani_distribution_${batch.id.take(8)}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, outFile, "application/pdf", "Share Qurbani Distribution PDF")
    }

    fun exportQurbaniDistributionCsv(
        context: Context,
        batch: QurbaniBatch,
        distributions: List<MeatDistribution>
    ) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Event,Family Code,Family Name,Allocated (kg),Delivered (kg),Status,Received By")
        distributions.forEach { d ->
            sb.appendLine("\"${escapeCsv(batch.eventName)}\",\"${escapeCsv(d.familyCode)}\",\"${escapeCsv(d.familyName)}\",${d.allocatedKg},${d.deliveredKg},${d.status},\"${escapeCsv(d.receivedBy ?: "")}\"")
        }
        val outFile = File(reportsDir(context), "qurbani_distribution_${batch.id.take(8)}.csv")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, outFile, "text/csv", "Share Qurbani Distribution CSV")
    }

    fun exportJsonBackup(context: Context, jsonContent: String) {
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val outFile = File(reportsDir(context), "masjid_society_backup_$stamp.json")
        outFile.writeText(jsonContent, Charsets.UTF_8)
        shareFile(context, outFile, "application/json", "Export Society JSON Backup")
    }

    fun exportAuditLogsCsv(context: Context, logs: List<AuditLogEntry>) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Timestamp,UserEmail,Action,Table,RecordId,Summary,Before,After")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        logs.forEach { log ->
            val ts = log.createdAt?.toDate()?.let { fmt.format(it) } ?: ""
            sb.appendLine(
                "\"$ts\",\"${escapeCsv(log.userEmail ?: log.userId)}\",\"${escapeCsv(log.action)}\"," +
                    "\"${escapeCsv(log.tableName)}\",\"${escapeCsv(log.recordId ?: "")}\"," +
                    "\"${escapeCsv(log.summary ?: "")}\",\"${escapeCsv(log.beforeSummary ?: "")}\"," +
                    "\"${escapeCsv(log.afterSummary ?: "")}\""
            )
        }
        val outFile = File(reportsDir(context), "masjid_audit_logs.csv")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, outFile, "text/csv", "Share Audit Logs CSV")
    }

    fun exportTransparencyPdf(
        context: Context,
        mosqueName: String,
        summary: PublicSummary,
        expenses: List<ExpenseRecord>
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply { color = Color.parseColor("#0A7D52") }
        canvas.drawRect(0f, 0f, 595f, 92f, headerPaint)
        val titlePaint = Paint().apply {
            color = Color.WHITE; textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        val subPaint = Paint().apply { color = Color.parseColor("#D0F0E3"); textSize = 12f; isAntiAlias = true }
        canvas.drawText("Public Financial Transparency Report", 36f, 40f, titlePaint)
        canvas.drawText("$mosqueName  |  ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}", 36f, 64f, subPaint)

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0A7D52"); textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        val bodyPaint = Paint().apply { color = Color.parseColor("#122019"); textSize = 11.5f; isAntiAlias = true }
        val nf = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }
        var y = 124f

        canvas.drawText("Fund Summary (Non-PII Public Record)", 36f, y, sectionPaint); y += 22f
        canvas.drawText("Total Dues + Donations Income: BDT ${nf.format(summary.totalIncome)}", 44f, y, bodyPaint); y += 18f
        canvas.drawText("Total Mosque Expense + Welfare: BDT ${nf.format(summary.totalOutflow)}", 44f, y, bodyPaint); y += 18f
        canvas.drawText("Net Society Balance: BDT ${nf.format(summary.balance)}", 44f, y, bodyPaint); y += 18f
        canvas.drawText("Active Families: ${summary.totalFamilies}   |   Active Members: ${summary.totalMembers}", 44f, y, bodyPaint); y += 28f

        val byCat = expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
        if (byCat.isNotEmpty()) {
            canvas.drawText("Expense Breakdown by Category", 36f, y, sectionPaint); y += 20f
            byCat.forEach { (cat, amt) ->
                if (y < 780f) {
                    canvas.drawText("- $cat: BDT ${nf.format(amt)}", 44f, y, bodyPaint)
                    y += 17f
                }
            }
        }
        doc.finishPage(page)
        val outFile = File(reportsDir(context), "masjid_public_transparency.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, outFile, "application/pdf", "Share Public Transparency PDF")
    }

    fun sharePaymentReceiptText(context: Context, payment: PaymentRecord) {
        val receipt = buildString {
            appendLine("=== Masjid & Society Payment Receipt ===")
            appendLine("Receipt No : ${payment.receiptNo}")
            appendLine("Date       : ${payment.paymentDate}")
            appendLine("Family     : ${payment.familyName}")
            if (!payment.dueTitle.isNullOrBlank()) appendLine("Due        : ${payment.dueTitle}")
            appendLine("Amount     : ৳ ${payment.amount}")
            appendLine("Method     : ${payment.paymentMethod.uppercase()}")
            if (!payment.accountName.isNullOrBlank()) appendLine("Account    : ${payment.accountName}")
            if (!payment.note.isNullOrBlank()) appendLine("Note       : ${payment.note}")
            appendLine("========================================")
        }
        shareWhatsAppOrText(context, receipt, "Share Payment Receipt")
    }

    fun shareDonationReceiptText(context: Context, donation: DonationRecord) {
        val donorLabel = if (donation.anonymous) "Anonymous (গোপন দাতা)" else donation.donorName
        val receipt = buildString {
            appendLine("=== Masjid & Society Donation Receipt ===")
            appendLine("Receipt No : ${donation.receiptNo}")
            appendLine("Date       : ${donation.donationDate}")
            appendLine("Donor      : $donorLabel")
            appendLine("Fund       : ${donation.fundType.uppercase()}")
            appendLine("Amount     : ৳ ${donation.amount}")
            appendLine("Method     : ${donation.paymentMethod.uppercase()}")
            if (!donation.transactionRef.isNullOrBlank()) appendLine("TrxID/Ref  : ${donation.transactionRef}")
            if (!donation.note.isNullOrBlank()) appendLine("Note       : ${donation.note}")
            appendLine("=========================================")
        }
        shareWhatsAppOrText(context, receipt, "Share Donation Receipt")
    }

    fun sendSmsReminder(context: Context, phone: String?, message: String) {
        val cleanPhone = phone?.trim().orEmpty()
        if (cleanPhone.isNotBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
                putExtra("sms_body", message)
            }
            val started = runCatching { context.startActivity(smsIntent) }.isSuccess
            if (started) return
        }
        shareWhatsAppOrText(context, message, "Send Due Reminder")
    }

    fun shareWhatsAppOrText(context: Context, text: String, chooserTitle: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareLocalServerScript(context: Context, scriptContent: String) {
        val outFile = File(reportsDir(context), "masjid-local-server.js.txt")
        outFile.writeText(scriptContent, Charsets.UTF_8)
        shareFile(context, outFile, "text/plain", "Share Local Server Script (rename to server.js & run: node server.js)")
    }

    private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")
}
