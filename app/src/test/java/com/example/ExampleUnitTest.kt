package com.example

import com.example.ai.PromptTemplates
import com.example.utils.IndianCurrencyFormatter
import com.example.utils.LocalizationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testIndianCurrencyFormatting() {
    // Basic formatting with decimals
    assertEquals("₹1,23,456.78", IndianCurrencyFormatter.formatRupees(123456.78))
    assertEquals("₹5,000.00", IndianCurrencyFormatter.formatRupees(5000.0))
    assertEquals("₹100.50", IndianCurrencyFormatter.formatRupees(100.5))
    assertEquals("₹0.00", IndianCurrencyFormatter.formatRupees(0.0))

    // Crores and lakhs formatting
    assertEquals("₹1,00,00,000.00", IndianCurrencyFormatter.formatRupees(10000000.0))
    assertEquals("₹12,34,56,789.00", IndianCurrencyFormatter.formatRupees(123456789.0))

    // Without decimals
    assertEquals("₹1,23,456", IndianCurrencyFormatter.formatRupees(123456.78, includeDecimals = false))
    assertEquals("₹50", IndianCurrencyFormatter.formatRupees(50.0, includeDecimals = false))

    // Negative amounts
    assertEquals("-₹500.00", IndianCurrencyFormatter.formatRupees(-500.0))
  }

  @Test
  fun testLocalizationHelper() {
    // English default
    assertEquals("AuraScan", LocalizationHelper.getString("app_name", "en"))
    assertEquals("Scan Document", LocalizationHelper.getString("scan_button", "en"))
    assertEquals("5-Point Legal Summary", LocalizationHelper.getString("legal_summary", "en"))
    assertEquals("Expense Log", LocalizationHelper.getString("expenses_title", "en"))

    // Hindi translations
    assertEquals("ऑरास्कैन", LocalizationHelper.getString("app_name", "hi"))
    assertEquals("दस्तावेज़ स्कैन करें", LocalizationHelper.getString("scan_button", "hi"))
    assertEquals("5-बिंदु कानूनी सारांश", LocalizationHelper.getString("legal_summary", "hi"))
    assertEquals("खर्च लॉग", LocalizationHelper.getString("expenses_title", "hi"))

    // Fallback for missing keys
    assertEquals("fallback_custom_key", LocalizationHelper.getString("fallback_custom_key", "en"))
    assertEquals("fallback_custom_key", LocalizationHelper.getString("fallback_custom_key", "hi"))
  }

  @Test
  fun testPromptTemplates() {
    val cleanUpPrompt = PromptTemplates.buildCleanUpTextPrompt("Sample raw handwritten text")
    assertTrue(cleanUpPrompt.contains("Sample raw handwritten text"))
    assertTrue(cleanUpPrompt.contains("typos"))

    val bulletsPrompt = PromptTemplates.buildConvertToBulletsPrompt("Key point one. Key point two.")
    assertTrue(bulletsPrompt.contains("Key point one"))
    assertTrue(bulletsPrompt.contains("bullet notes"))

    val translationPrompt = PromptTemplates.buildTranslateLegalPrompt(listOf("Summary bullet 1"), "Hindi")
    assertTrue(translationPrompt.contains("Hindi"))
    assertTrue(translationPrompt.contains("Summary bullet 1"))
  }

  @Test
  fun testRewardedAdUnitId() {
    assertEquals(
      "ca-app-pub-3940256099942544/5224354917",
      com.example.utils.RewardedAdManager.TEST_REWARDED_AD_UNIT_ID
    )
    assertEquals(
      "ca-app-pub-6086454785172418/1543538492",
      com.example.utils.RewardedAdManager.PROD_REWARDED_AD_UNIT_ID
    )
    assertEquals(
      "ca-app-pub-6086454785172418~7875023453",
      com.example.utils.AdConfig.ADMOB_APP_ID
    )
  }
}


