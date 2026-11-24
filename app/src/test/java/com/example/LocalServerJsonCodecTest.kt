package com.example

import com.example.data.localserver.LocalServerJsonCodec
import com.example.data.localserver.ServerRuntimeState
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.data.prefs.ServerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalServerJsonCodecTest {

    @Test
    fun normalizeUrl_addsHttpSchemeAndStripsTrailingSlash() {
        assertEquals("http://192.168.1.50:3000", ServerRuntimeState.normalizeUrl("192.168.1.50:3000/"))
        assertEquals("https://masjid.local:8443", ServerRuntimeState.normalizeUrl("https://masjid.local:8443/"))
        assertEquals("http://10.0.2.2:3000", ServerRuntimeState.normalizeUrl("   "))
    }

    @Test
    fun serverMode_parsesAndChecksRestartRequirements() {
        assertEquals(ServerMode.CLOUD_FIREBASE, ServerMode.fromCode("cloud"))
        assertEquals(ServerMode.LOCAL_EMULATOR, ServerMode.fromCode("emulator"))
        assertEquals(ServerMode.LOCAL_HTTP_SERVER, ServerMode.fromCode("local_http"))
        assertFalse(ServerRuntimeState.requiresRestartForSwitch(ServerMode.CLOUD_FIREBASE, ServerMode.LOCAL_HTTP_SERVER))
        assertTrue(ServerRuntimeState.requiresRestartForSwitch(ServerMode.CLOUD_FIREBASE, ServerMode.LOCAL_EMULATOR))
    }

    @Test
    fun computeSummary_calculatesBalancesAndOutstandingAccurately() {
        val families = listOf(
            Family(id = "f1", familyName = "রহমান পরিবার", familyCode = "FAM-01", active = true),
            Family(id = "f2", familyName = "করিম পরিবার", familyCode = "FAM-02", active = true)
        )
        val dues = listOf(
            DueItem(id = "d1", title = "Monthly", dueType = "monthly", amount = 500.0, active = true)
        )
        val payments = listOf(
            PaymentRecord(id = "p1", familyId = "f1", familyName = "রহমান পরিবার", amount = 500.0, paymentMethod = "cash", receiptNo = "RCP-1", paymentDate = "2026-11-24")
        )
        val donations = listOf(
            DonationRecord(id = "don1", donorName = "Donor", fundType = "general", amount = 1000.0, paymentMethod = "bkash", receiptNo = "DON-1", donationDate = "2026-11-24")
        )
        val expenses = listOf(
            ExpenseRecord(id = "exp1", title = "Electricity", category = "Utility", amount = 300.0, expenseDate = "2026-11-24")
        )

        val summary = LocalServerJsonCodec.computeSummary(
            families = families,
            members = emptyList(),
            dues = dues,
            payments = payments,
            donations = donations,
            expenses = expenses,
            welfare = emptyList(),
            batches = emptyList(),
            distributions = emptyList()
        )

        assertEquals(2L, summary.totalFamilies)
        assertEquals(500.0, summary.totalCollection, 0.01)
        assertEquals(1000.0, summary.totalDonations, 0.01)
        assertEquals(300.0, summary.totalExpense, 0.01)
        assertEquals(1200.0, summary.balance, 0.01)
        assertEquals(500.0, summary.totalOutstanding, 0.01)
    }
}
