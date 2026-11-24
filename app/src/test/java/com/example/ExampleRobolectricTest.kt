package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PublicSummary
import com.example.data.prefs.AppLanguage
import com.example.ui.i18n.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Masjid Society", appName)
    }

    @Test
    fun `public summary balance equals collection minus expense`() {
        val summary = PublicSummary(
            totalFamilies = 12L,
            totalMembers = 48L,
            totalCollection = 25500.0,
            totalExpense = 11250.0
        )
        assertEquals(14250.0, summary.balance, 0.001)
    }

    @Test
    fun `qurbani equal distribution rounding is accurate`() {
        val totalMeatKg = 100.0
        val activeFamilies = 3
        val perFamily = ((totalMeatKg / activeFamilies.toDouble()) * 1000.0).roundToLong() / 1000.0
        assertEquals(33.333, perFamily, 0.0001)
    }

    @Test
    fun `bilingual strings switch between Bangla and English`() {
        val bn = stringsFor(AppLanguage.BANGLA)
        val en = stringsFor(AppLanguage.ENGLISH)
        assertEquals("মসজিদ ও সমাজ", bn.appTitle)
        assertEquals("Masjid Society", en.appTitle)
    }
}
