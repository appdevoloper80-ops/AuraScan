package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.BadgeBill
import com.example.ui.theme.BadgeId
import com.example.ui.theme.BadgeLegal
import com.example.ui.theme.BadgeNotes
import com.example.ui.theme.BadgeOther

enum class DocumentType(
    val key: String,
    val displayName: String,
    val badgeColor: Color,
    val icon: ImageVector,
    val description: String
) {
    AADHAAR(
        key = "aadhaar",
        displayName = "Aadhaar Card",
        badgeColor = BadgeId,
        icon = Icons.Default.Badge,
        description = "UIDAI Indian National ID"
    ),
    PAN(
        key = "pan",
        displayName = "PAN Card",
        badgeColor = BadgeId,
        icon = Icons.Default.Badge,
        description = "Income Tax ID Card"
    ),
    BILL_INVOICE(
        key = "bill_invoice",
        displayName = "Bill / Invoice",
        badgeColor = BadgeBill,
        icon = Icons.Default.ReceiptLong,
        description = "Receipt, GST bill, or retail invoice"
    ),
    TEXTBOOK_NOTES(
        key = "textbook_notes",
        displayName = "Textbook / Notes",
        badgeColor = BadgeNotes,
        icon = Icons.Default.MenuBook,
        description = "Study material, book pages & notes"
    ),
    LEGAL_DOCUMENT(
        key = "legal_document",
        displayName = "Legal Document",
        badgeColor = BadgeLegal,
        icon = Icons.Default.Scale,
        description = "Contracts, agreements & deeds"
    ),
    HANDWRITTEN_NOTE(
        key = "handwritten_note",
        displayName = "Handwritten Note",
        badgeColor = BadgeOther,
        icon = Icons.Default.TextSnippet,
        description = "Handwritten paper or memo"
    ),
    OTHER(
        key = "other",
        displayName = "Other Document",
        badgeColor = BadgeOther,
        icon = Icons.Default.Description,
        description = "General scanned document"
    );

    companion object {
        fun fromKey(raw: String?): DocumentType {
            if (raw == null) return OTHER
            val normalized = raw.trim().lowercase().replace("-", "_")
            return entries.firstOrNull { it.key == normalized }
                ?: when {
                    normalized.contains("aadhaar") || normalized.contains("aadhar") -> AADHAAR
                    normalized.contains("pan") -> PAN
                    normalized.contains("bill") || normalized.contains("invoice") || normalized.contains("receipt") -> BILL_INVOICE
                    normalized.contains("textbook") || normalized.contains("notes") || normalized.contains("study") -> TEXTBOOK_NOTES
                    normalized.contains("legal") || normalized.contains("agreement") || normalized.contains("contract") -> LEGAL_DOCUMENT
                    normalized.contains("handwritten") -> HANDWRITTEN_NOTE
                    else -> OTHER
                }
        }
    }
}
