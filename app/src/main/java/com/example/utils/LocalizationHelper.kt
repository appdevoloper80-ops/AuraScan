package com.example.utils

object LocalizationHelper {

    private val en = mapOf(
        "app_name" to "AuraScan",
        "home_greeting" to "Welcome to AuraScan",
        "home_subtitle" to "Smart document scanner & instant actions",
        "scan_button" to "Scan Document",
        "recent_scans" to "Recent Scans",
        "view_all" to "View All",
        "search_hint" to "Search scans by title or OCR text...",
        "nav_home" to "Home",
        "nav_scans" to "Scans",
        "nav_tools" to "Tools",
        "nav_settings" to "Settings",
        "legal_summary" to "5-Point Legal Summary",
        "legal_parties" to "Parties Involved",
        "legal_dates" to "Key Dates & Deadlines",
        "legal_amounts" to "Financials & Amounts",
        "legal_obligations" to "Key Obligations",
        "legal_things_to_check" to "Things to Check (High-Risk Clauses)",
        "legal_disclaimer" to "This is an AI summary, not legal advice. Please consult a qualified lawyer.",
        "handwritten_title" to "Handwritten Notes & General OCR",
        "confidence_note" to "OCR Confidence",
        "clean_up_text" to "Clean up text",
        "convert_bullets" to "Convert to bullet notes",
        "re_ocr_region" to "Re-scan Region",
        "expenses_title" to "Expense Log",
        "monthly_summary" to "Monthly Summary",
        "total_spent" to "Total Spent",
        "total_gst" to "Total GST Paid",
        "export_csv" to "Export CSV",
        "no_expenses" to "No expenses logged for this month.",
        "ai_free_mode" to "AI-Free Mode",
        "ask_before_ai" to "Ask before sending to AI",
        "offline_warning" to "You're offline. This feature needs internet.",
        "privacy_policy" to "Privacy & On-Device Processing"
    )

    private val hi = mapOf(
        "app_name" to "ऑरास्कैन",
        "home_greeting" to "ऑरास्कैन में आपका स्वागत है",
        "home_subtitle" to "स्मार्ट दस्तावेज़ स्कैनर और त्वरित कार्य",
        "scan_button" to "दस्तावेज़ स्कैन करें",
        "recent_scans" to "हाल के स्कैन",
        "view_all" to "सभी देखें",
        "search_hint" to "शीर्षक या टेक्स्ट द्वारा खोजें...",
        "nav_home" to "होम",
        "nav_scans" to "स्कैन",
        "nav_tools" to "टूल्स",
        "nav_settings" to "सेटिंग्स",
        "legal_summary" to "5-बिंदु कानूनी सारांश",
        "legal_parties" to "शामिल पक्ष",
        "legal_dates" to "मुख्य तिथियां और समयसीमा",
        "legal_amounts" to "वित्तीय और भुगतान राशि",
        "legal_obligations" to "मुख्य जिम्मेदारियां",
        "legal_things_to_check" to "जांचने योग्य बिंदु (जोखिम खंड)",
        "legal_disclaimer" to "यह एक AI सारांश है, कानूनी सलाह नहीं। कृपया योग्य वकील से परामर्श लें।",
        "handwritten_title" to "हस्तलिखित नोट्स और OCR",
        "confidence_note" to "पहचान सटीकता",
        "clean_up_text" to "टेक्स्ट सुधारें",
        "convert_bullets" to "बुलेट नोट्स में बदलें",
        "re_ocr_region" to "क्षेत्र पुनः स्कैन करें",
        "expenses_title" to "खर्च लॉग",
        "monthly_summary" to "मासिक सारांश",
        "total_spent" to "कुल खर्च",
        "total_gst" to "कुल भुगतान किया गया GST",
        "export_csv" to "CSV निर्यात करें",
        "no_expenses" to "इस महीने के लिए कोई खर्च दर्ज नहीं है।",
        "ai_free_mode" to "AI-मुक्त मोड",
        "ask_before_ai" to "AI को भेजने से पहले पूछें",
        "offline_warning" to "आप ऑफ़लाइन हैं। इस सुविधा के लिए इंटरनेट आवश्यक है।",
        "privacy_policy" to "गोपनीयता और ऑन-डिवाइस प्रोसेसिंग"
    )

    fun getString(key: String, languageCode: String = "en"): String {
        return if (languageCode == "hi") {
            hi[key] ?: en[key] ?: key
        } else {
            en[key] ?: key
        }
    }
}
