package com.example

import com.example.data.model.PublicSummary
import com.example.data.model.SocietyRoles
import com.example.data.model.UserProfile
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun publicSummary_computesIncomeOutflowAndBalance() {
        val summary = PublicSummary(
            totalCollection = 15000.0,
            totalDonations = 8500.0,
            totalExpense = 6000.0,
            totalWelfare = 2500.0
        )
        assertEquals(23500.0, summary.totalIncome, 0.001)
        assertEquals(8500.0, summary.totalOutflow, 0.001)
        assertEquals(15000.0, summary.balance, 0.001)
    }

    @Test
    fun userProfile_rbacHelpersEnforceHierarchy() {
        val accountant = UserProfile(role = SocietyRoles.ACCOUNTANT)
        assertTrue(accountant.isFinanceAdmin)
        assertTrue(accountant.isStaffReader)
        assertFalse(accountant.isOpsManager)
        assertFalse(accountant.isFullAdmin)

        val manager = UserProfile(role = SocietyRoles.MANAGER)
        assertTrue(manager.isOpsManager)
        assertTrue(manager.isStaffReader)
        assertFalse(manager.isFinanceAdmin)

        val viewer = UserProfile(role = SocietyRoles.VIEWER)
        assertTrue(viewer.isStaffReader)
        assertFalse(viewer.isFinanceAdmin)
        assertFalse(viewer.isOpsManager)

        val member = UserProfile(role = SocietyRoles.MEMBER)
        assertFalse(member.isStaffReader)
        assertFalse(member.isFinanceAdmin)
        assertFalse(member.isOpsManager)
    }

    @Test
    fun qrCodeGenerator_rendersValidFinderPatternsAndSizes() {
        val v1 = QrCodeGenerator.encode("MASJID|FAM-001")
        assertEquals(21, v1.size)
        // Top-left finder center (3,3), corners (0,0) and (6,6) must be dark
        assertTrue(v1.get(0, 0))
        assertTrue(v1.get(3, 3))
        assertTrue(v1.get(6, 6))
        // Separator ring at (1,1) is light
        assertFalse(v1.get(1, 1))
        // Dark module at (8, size - 8)
        assertTrue(v1.get(8, v1.size - 8))

        val longer = QrCodeGenerator.encode("MASJID|FAM-001|Karim Family|+8801711223344|North Para")
        assertTrue(longer.size >= 29)
        assertTrue(longer.get(longer.size - 1, 0)) // top-right finder outer corner
        assertTrue(longer.get(0, longer.size - 1)) // bottom-left finder outer corner
    }
}
