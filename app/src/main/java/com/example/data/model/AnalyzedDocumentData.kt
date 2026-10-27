package com.example.data.model

data class AnalyzedDocumentData(
    val documentType: String = "other",
    val suggestedTitle: String = "Scanned Document",
    val extractedText: String = "",
    val language: String = "English",
    val confidence: Float = 0.95f,
    // Bill / Invoice fields
    val merchantName: String = "",
    val invoiceDate: String = "",
    val lineItems: List<BillLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxBreakdown: String = "",
    val totalAmount: Double = 0.0,
    val suggestedExpenseCategory: String = "Food",
    // ID card fields (Aadhaar / PAN)
    val idType: String = "",
    val idNumber: String = "",
    val maskedIdNumber: String = "",
    val holderName: String = "",
    val sensitiveBoxes: List<NormalizedBox> = emptyList(),
    // Textbook / Notes fields
    val summaryBullets: List<String> = emptyList(),
    val flashcards: List<FlashcardItem> = emptyList(),
    val keyTerms: List<String> = emptyList(),
    val translatedKeyTerms: List<TranslatedTermItem> = emptyList(),
    val chatMessages: List<ChatMessageItem> = emptyList(),
    // Legal Document fields
    val legalSummary: String = "",
    val legalSummaryBullets: List<String> = emptyList(),
    val keyDates: List<LegalDateItem> = emptyList(),
    val parties: List<LegalPartyItem> = emptyList(),
    val legalAmounts: List<String> = emptyList(),
    val legalObligations: List<String> = emptyList(),
    val legalThingsToCheck: List<LegalClauseItem> = emptyList(),
    val legalTranslatedSummary: String? = null,
    val isLegalDisclaimerAcknowledged: Boolean = false,
    // Handwritten / Other fields
    val handwrittenConfidenceNote: String = "Good (92%)",
    val handwrittenCleanedText: String? = null,
    val handwrittenBulletNotes: List<String> = emptyList(),
    val reOcredRegionText: String? = null
)

data class BillLineItem(
    val name: String,
    val qty: Double = 1.0,
    val price: Double = 0.0
)

data class NormalizedBox(
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float,
    val label: String = "Sensitive Number"
)

data class PointNorm(
    val x: Float,
    val y: Float
)

data class FlashcardItem(
    val id: String,
    val question: String,
    val answer: String,
    val isKnown: Boolean = false
)

data class TranslatedTermItem(
    val term: String,
    val translation: String,
    val meaning: String
)

data class ChatMessageItem(
    val id: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LegalDateItem(
    val date: String,
    val description: String
)

data class LegalPartyItem(
    val name: String,
    val role: String
)

data class LegalClauseItem(
    val category: String, // "Penalties", "Auto-renewal", "Termination", "Jurisdiction", "Other"
    val title: String,
    val description: String,
    val severity: String = "Warning" // "Warning", "Attention", "Notice"
)
