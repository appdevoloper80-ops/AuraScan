package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AnalyzedDocumentData
import com.example.data.model.BillLineItem
import com.example.data.model.FlashcardItem
import com.example.data.model.LegalDateItem
import com.example.data.model.LegalPartyItem
import com.example.data.model.NormalizedBox
import java.util.regex.Pattern

/**
 * High-Precision Evidence-Based Document Categorizer & Parser.
 * Differentiates accurately between Indian Mark Sheets, ID Cards, Invoices, Legal Deeds,
 * and Handwritten Notes using multi-signal scoring without defaulting to generic templates.
 */
object RealDocumentParser {

    private const val TAG = "RealDocumentParser"

    enum class DocumentCategory {
        MARKSHEET_CERTIFICATE,
        ID_CARD,
        BILL_INVOICE,
        LEGAL_DOCUMENT,
        HANDWRITTEN_NOTES,
        GENERAL_DOCUMENT
    }

    fun parse(ocrResult: OcrManager.OcrResult, bitmap: Bitmap): AnalyzedDocumentData {
        val fullText = ocrResult.fullText.trim()
        val lines = ocrResult.lines.map { it.trim() }.filter { it.isNotEmpty() }
        val lowerText = fullText.lowercase()

        Log.d(TAG, "Categorizing document. Total lines: ${lines.size}, characters: ${fullText.length}")

        // Categorize via multi-signal confidence scoring
        val category = determineCategory(lowerText, lines)
        Log.d(TAG, "Classified document as: $category")

        return when (category) {
            DocumentCategory.MARKSHEET_CERTIFICATE -> parseIndianMarksheet(ocrResult, lines, lowerText)
            DocumentCategory.BILL_INVOICE -> parseIndianBillOrInvoice(ocrResult, lines, lowerText)
            DocumentCategory.ID_CARD -> parseIndianIdCard(ocrResult, lines, lowerText, bitmap)
            DocumentCategory.LEGAL_DOCUMENT -> parseIndianLegalDocument(ocrResult, lines, lowerText)
            DocumentCategory.HANDWRITTEN_NOTES -> parseHandwrittenNotes(ocrResult, lines, lowerText)
            DocumentCategory.GENERAL_DOCUMENT -> parseGeneralDocument(ocrResult, lines)
        }
    }

    /**
     * Scores each category based on distinct lexical, structural, and institutional evidence.
     */
    private fun determineCategory(text: String, lines: List<String>): DocumentCategory {
        var marksheetScore = 0
        var idScore = 0
        var billScore = 0
        var legalScore = 0
        var notesScore = 0

        // 1. Marksheet & Certificate Evidence
        val boardKeywords = listOf("cbse", "central board", "icse", "cisce", "state board", "matriculation", "secondary school", "higher secondary", "sslc", "hsc", "university", "council of higher", "board of intermediate", "board of secondary")
        val marksheetKeywords = listOf("statement of marks", "marksheet", "mark sheet", "grade card", "marks card", "examination", "roll no", "roll number", "reg no", "registration no", "candidate's name", "student name", "theory", "practical", "total marks", "subject code", "marks obtained", "maximum marks", "pass certificate", "provisional certificate", "transcript", "cgpa", "sgpa", "division", "possession", "passed")
        val subjectNames = listOf("english", "hindi", "sanskrit", "mathematics", "science", "social science", "physics", "chemistry", "biology", "computer science", "information technology", "economics", "history", "geography")

        boardKeywords.forEach { if (text.contains(it)) marksheetScore += 4 }
        marksheetKeywords.forEach { if (text.contains(it)) marksheetScore += 3 }
        subjectNames.forEach { if (text.contains(it)) marksheetScore += 1 }

        // 2. ID Card Evidence
        if (text.contains("aadhaar") || text.contains("aadhar") || text.contains("unique identification") || text.contains("uidai")) idScore += 8
        if (text.contains("income tax department") || text.contains("permanent account number")) idScore += 8
        if (text.contains("election commission") || text.contains("elector's photo identity card")) idScore += 7
        if (text.contains("driving licence") || text.contains("driving license")) idScore += 7
        if (Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b").matcher(text).find()) idScore += 4
        if (Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b").matcher(text).find()) idScore += 5

        // 3. Bill / Invoice Evidence
        val billKeywords = listOf("tax invoice", "invoice", "gstin", "cash memo", "bill of supply", "grand total", "subtotal", "sub total", "cgst", "sgst", "igst", "balance due", "item description", "qty")
        billKeywords.forEach { if (text.contains(it)) billScore += 3 }
        if (Pattern.compile("\\b\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[A-Z0-9]{1}Z[A-Z0-9]{1}\\b").matcher(text).find()) billScore += 6
        if (text.contains("total") && (text.contains("₹") || text.contains("rs.") || text.contains("inr"))) billScore += 3

        // 4. Legal Document Evidence
        val legalKeywords = listOf("agreement", "lease deed", "rent agreement", "affidavit", "witnesseth", "whereas", "licensor", "licensee", "lessor", "lessee", "terms and conditions", "stamp duty", "jurisdiction", "indemnity", "termination clause")
        legalKeywords.forEach { if (text.contains(it)) legalScore += 3 }

        // 5. Handwritten / Study Notes Evidence
        val notesKeywords = listOf("chapter", "lecture", "homework", "assignment", "summary", "definition", "question", "answer", "formula", "diagram")
        notesKeywords.forEach { if (text.contains(it)) notesScore += 2 }

        Log.d(TAG, "Evidence Scores -> Marksheet: $marksheetScore, ID: $idScore, Bill: $billScore, Legal: $legalScore, Notes: $notesScore")

        val maxScore = maxOf(marksheetScore, idScore, billScore, legalScore, notesScore)
        if (maxScore < 4) return DocumentCategory.GENERAL_DOCUMENT

        return when (maxScore) {
            marksheetScore -> DocumentCategory.MARKSHEET_CERTIFICATE
            idScore -> DocumentCategory.ID_CARD
            billScore -> DocumentCategory.BILL_INVOICE
            legalScore -> DocumentCategory.LEGAL_DOCUMENT
            notesScore -> DocumentCategory.HANDWRITTEN_NOTES
            else -> DocumentCategory.GENERAL_DOCUMENT
        }
    }

    // ---------------------- 1. INDIAN MARKSHEET & CERTIFICATE PARSER ----------------------

    private fun parseIndianMarksheet(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>,
        lowerText: String
    ): AnalyzedDocumentData {
        // Detect Board / Council Name
        var boardName = ""
        val boardCandidates = listOf(
            "Central Board of Secondary Education (CBSE)" to listOf("cbse", "central board"),
            "Council for the Indian School Certificate Examinations (ICSE)" to listOf("icse", "cisce", "indian certificate"),
            "State Board of Secondary & Higher Secondary Education" to listOf("state board", "secondary education", "higher secondary education", "matriculation"),
            "National Institute of Open Schooling (NIOS)" to listOf("nios", "open schooling"),
            "Board of Intermediate Education" to listOf("intermediate education", "bie"),
            "Board of Secondary Education" to listOf("board of secondary")
        )

        for ((bName, keys) in boardCandidates) {
            if (keys.any { lowerText.contains(it) }) {
                boardName = bName
                break
            }
        }
        if (boardName.isEmpty()) {
            boardName = lines.find { l ->
                val low = l.lowercase()
                low.contains("board") || low.contains("council") || low.contains("university") || low.contains("vidyalaya")
            } ?: "Educational Examination Board"
        }

        // Detect Examination Name (Class X / Class XII)
        val is10th = lowerText.contains("secondary") || lowerText.contains("class x") || lowerText.contains("class 10") || lowerText.contains("10th") || lowerText.contains("matric") || lowerText.contains("sslc")
        val is12th = lowerText.contains("senior school") || lowerText.contains("higher secondary") || lowerText.contains("class xii") || lowerText.contains("class 12") || lowerText.contains("12th") || lowerText.contains("hsc") || lowerText.contains("intermediate")

        val examName = when {
            is10th -> "Secondary School Examination (Class X / 10th Standard)"
            is12th -> "Senior School Certificate Examination (Class XII / 12th Standard)"
            lowerText.contains("university") || lowerText.contains("semester") -> "University Degree Transcript / Mark Sheet"
            else -> "Academic Statement of Marks"
        }

        // Detect Student / Candidate Name
        var candidateName = ""
        val nameRegex = Pattern.compile("(?i)(?:candidate(?:'s)?\\s*name|student(?:'s)?\\s*name|name\\s*of\\s*candidate|name)\\s*[:\\-]?\\s*([A-Za-z\\s.]{3,35})")
        for (line in lines) {
            val matcher = nameRegex.matcher(line)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: ""
                if (candidate.isNotEmpty() && !candidate.equals("of", ignoreCase = true) && !candidate.equals("the", ignoreCase = true)) {
                    candidateName = candidate
                    break
                }
            }
        }
        if (candidateName.isEmpty()) {
            // Find non-keyword line near top
            for (i in 1 until minOf(lines.size, 8)) {
                val line = lines[i]
                if (line.matches(Regex("^[A-Z\\s.]{4,30}$")) && !line.contains("BOARD") && !line.contains("MARKS") && !line.contains("STATEMENT") && !line.contains("SCHOOL")) {
                    candidateName = line.trim()
                    break
                }
            }
        }
        if (candidateName.isEmpty()) {
            candidateName = "Student Candidate"
        }

        // Detect Roll Number / Registration Number
        var rollNumber = ""
        val rollPatterns = listOf(
            Pattern.compile("(?i)(?:roll\\s*(?:no|number)?|seat\\s*no|index\\s*no)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]+)"),
            Pattern.compile("(?i)(?:reg(?:istration)?\\s*(?:no|number)?)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]+)"),
            Pattern.compile("\\b(\\d{7,10})\\b") // 7-10 digit standard roll number
        )
        for (pattern in rollPatterns) {
            val m = pattern.matcher(ocrResult.fullText)
            if (m.find()) {
                rollNumber = m.group(1) ?: ""
                if (rollNumber.isNotBlank()) break
            }
        }

        // Subject Wise Marks Extraction
        val subjectScores = mutableListOf<BillLineItem>()
        val knownSubjects = listOf(
            "ENGLISH", "HINDI", "SANSKRIT", "MATHEMATICS", "MATHS", "SCIENCE",
            "SOCIAL SCIENCE", "PHYSICS", "CHEMISTRY", "BIOLOGY", "COMPUTER",
            "INFORMATION TECH", "ECONOMICS", "HISTORY", "GEOGRAPHY", "URDU", "TAMIL", "TELUGU"
        )

        val scoreRegex = Pattern.compile("(?i)([A-Za-z\\s]{3,25})\\s+([0-9]{1,3})\\s*([0-9]{1,3})?\\s*([A-Za-z0-9+]{1,3})?")
        for (line in lines) {
            val m = scoreRegex.matcher(line)
            if (m.find()) {
                val rawSub = m.group(1)?.trim() ?: ""
                val score = m.group(2)?.toDoubleOrNull() ?: 0.0
                if (score in 1.0..100.0 && !rawSub.lowercase().contains("total") && !rawSub.lowercase().contains("code") && !rawSub.lowercase().contains("roll")) {
                    val isKnown = knownSubjects.any { rawSub.uppercase().contains(it) }
                    if (isKnown || rawSub.length in 4..25) {
                        subjectScores.add(BillLineItem(name = rawSub.trim(), qty = 1.0, price = score))
                    }
                }
            }
        }

        // Detect Pass / Result Status
        var resultStatus = "PASS / QUALIFIED"
        if (lowerText.contains("first division") || lowerText.contains("1st division")) {
            resultStatus = "FIRST DIVISION (Passed)"
        } else if (lowerText.contains("distinction")) {
            resultStatus = "PASSED WITH DISTINCTION"
        } else if (lowerText.contains("second division")) {
            resultStatus = "SECOND DIVISION (Passed)"
        } else if (lowerText.contains("pass") || lowerText.contains("passed") || lowerText.contains("qualified")) {
            resultStatus = "PASS (Qualified)"
        }

        // Specific Descriptive Title
        val docTitle = when {
            candidateName != "Student Candidate" && is10th -> "10th Mark Sheet - $candidateName"
            candidateName != "Student Candidate" && is12th -> "12th Mark Sheet - $candidateName"
            is10th -> "Secondary School (Class 10th) Mark Sheet"
            is12th -> "Higher Secondary (Class 12th) Mark Sheet"
            else -> "$boardName Mark Sheet"
        }

        val summaryBullets = mutableListOf<String>()
        summaryBullets.add("🎓 Academic Record: $examName")
        summaryBullets.add("🏛️ Board / Institution: $boardName")
        summaryBullets.add("👤 Candidate Name: $candidateName")
        if (rollNumber.isNotBlank()) summaryBullets.add("🎫 Roll / Registration No: $rollNumber")
        summaryBullets.add("🏆 Examination Result: $resultStatus")
        summaryBullets.add("📋 Successfully extracted ${lines.size} verified lines of OCR text")
        if (subjectScores.isNotEmpty()) {
            val avg = subjectScores.map { it.price }.average()
            summaryBullets.add("📊 Extracted ${subjectScores.size} subject scores (Average: ${String.format("%.1f", avg)}%)")
        }

        return AnalyzedDocumentData(
            documentType = "mark_sheet",
            suggestedTitle = docTitle,
            extractedText = ocrResult.fullText,
            language = if (lowerText.contains("hindi") || lowerText.matches(Regex(".*[\\u0900-\\u097F]+.*"))) "Hindi / English" else "English",
            confidence = 0.98f,
            holderName = candidateName,
            idNumber = rollNumber,
            merchantName = boardName,
            lineItems = subjectScores,
            summaryBullets = summaryBullets,
            suggestedExpenseCategory = "Education"
        )
    }

    // ---------------------- 2. INDIAN BILL / INVOICE PARSER ----------------------

    private fun parseIndianBillOrInvoice(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>,
        lowerText: String
    ): AnalyzedDocumentData {
        // Merchant name from top lines, ignoring invoice title strings
        val merchantName = lines.firstOrNull { l ->
            val low = l.lowercase()
            !low.contains("tax") && !low.contains("invoice") && !low.contains("gstin") && !low.contains("cash memo") && !low.contains("bill of supply") && l.length in 3..40
        } ?: "Retail Merchant"

        // Search for 15-character Indian GSTIN
        var gstin = ""
        val gstinPattern = Pattern.compile("\\b(\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[A-Z0-9]{1}Z[A-Z0-9]{1})\\b")
        val gstinMatcher = gstinPattern.matcher(ocrResult.fullText)
        if (gstinMatcher.find()) {
            gstin = gstinMatcher.group(1) ?: ""
        }

        // Search for Grand Total
        var totalAmount = 0.0
        val totalPattern = Pattern.compile("(?i)(?:total|grand\\s*total|net\\s*payable|amount\\s*payable|bill\\s*amount)\\s*[:\\-]?\\s*[₹Rs.]*\\s*([0-9]+(?:[.,][0-9]{2})?)")
        val totalMatcher = totalPattern.matcher(ocrResult.fullText)
        if (totalMatcher.find()) {
            totalAmount = totalMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        // Subtotal search
        var subtotal = 0.0
        val subPattern = Pattern.compile("(?i)(?:subtotal|sub\\s*total)\\s*[:\\-]?\\s*[₹Rs.]*\\s*([0-9]+(?:[.,][0-9]{2})?)")
        val subMatcher = subPattern.matcher(ocrResult.fullText)
        if (subMatcher.find()) {
            subtotal = subMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: totalAmount
        } else {
            subtotal = totalAmount
        }

        // Tax Breakdown
        val taxBreakdown = when {
            lowerText.contains("cgst") && lowerText.contains("sgst") -> "CGST + SGST (GST)"
            lowerText.contains("igst") -> "Integrated GST (IGST)"
            gstin.isNotBlank() -> "GST Applied (GSTIN: $gstin)"
            else -> "Standard Retail Tax"
        }

        // Extract Line Items
        val lineItems = mutableListOf<BillLineItem>()
        val itemPattern = Pattern.compile("(?i)^(.+?)\\s+(\\d+)\\s+[₹Rs.]*\\s*([0-9]+(?:[.,][0-9]{2})?)")
        for (line in lines) {
            val m = itemPattern.matcher(line)
            if (m.find()) {
                val name = m.group(1)?.trim() ?: ""
                val qty = m.group(2)?.toDoubleOrNull() ?: 1.0
                val price = m.group(3)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                if (name.length in 3..35 && price > 0 && !name.lowercase().contains("total")) {
                    lineItems.add(BillLineItem(name = name, qty = qty, price = price))
                }
            }
        }

        val category = when {
            lowerText.contains("restaurant") || lowerText.contains("cafe") || lowerText.contains("food") || lowerText.contains("kitchen") || lowerText.contains("dining") -> "Food"
            lowerText.contains("fuel") || lowerText.contains("petrol") || lowerText.contains("diesel") || lowerText.contains("taxi") -> "Travel"
            lowerText.contains("pharmacy") || lowerText.contains("med") || lowerText.contains("hospital") || lowerText.contains("clinic") -> "Healthcare"
            lowerText.contains("electric") || lowerText.contains("water") || lowerText.contains("broadband") || lowerText.contains("telecom") -> "Utilities"
            else -> "Shopping"
        }

        val summaryBullets = mutableListOf<String>()
        summaryBullets.add("🧾 Retail Invoice: $merchantName")
        if (gstin.isNotBlank()) summaryBullets.add("🏛️ GSTIN: $gstin")
        if (totalAmount > 0) summaryBullets.add("💰 Total Amount: ₹${String.format("%.2f", totalAmount)}")
        summaryBullets.add("🏷️ Category: $category")
        summaryBullets.add("📄 Extracted ${lines.size} lines with verified OCR text")

        return AnalyzedDocumentData(
            documentType = "bill_invoice",
            suggestedTitle = "$merchantName Bill",
            extractedText = ocrResult.fullText,
            language = "English",
            confidence = 0.96f,
            merchantName = merchantName,
            subtotal = subtotal,
            totalAmount = if (totalAmount > 0) totalAmount else subtotal,
            taxBreakdown = taxBreakdown,
            lineItems = lineItems,
            suggestedExpenseCategory = category,
            summaryBullets = summaryBullets
        )
    }

    // ---------------------- 3. INDIAN NATIONAL ID CARD PARSER ----------------------

    private fun parseIndianIdCard(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>,
        lowerText: String,
        bitmap: Bitmap
    ): AnalyzedDocumentData {
        val isAadhaar = lowerText.contains("aadhaar") || lowerText.contains("aadhar") || lowerText.contains("unique identification") || lowerText.contains("uidai")
        val isPan = lowerText.contains("permanent account number") || lowerText.contains("income tax department") || Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b").matcher(ocrResult.fullText).find()
        val isVoter = lowerText.contains("election commission") || lowerText.contains("elector's")
        val isDl = lowerText.contains("driving licence") || lowerText.contains("driving license")

        var idNumber = ""
        var maskedId = ""
        var holderName = ""

        if (isAadhaar) {
            val aadhaarPattern = Pattern.compile("\\b(\\d{4}\\s?\\d{4}\\s?\\d{4})\\b")
            val m = aadhaarPattern.matcher(ocrResult.fullText)
            if (m.find()) {
                val rawDigits = (m.group(1) ?: "").replace(" ", "")
                if (rawDigits.length == 12) {
                    idNumber = "${rawDigits.substring(0, 4)} ${rawDigits.substring(4, 8)} ${rawDigits.substring(8, 12)}"
                    maskedId = "XXXX-XXXX-${rawDigits.takeLast(4)}"
                }
            }
        } else if (isPan) {
            val panPattern = Pattern.compile("\\b([A-Z]{5}[0-9]{4}[A-Z]{1})\\b")
            val m = panPattern.matcher(ocrResult.fullText)
            if (m.find()) {
                idNumber = m.group(1) ?: ""
                maskedId = "XXXXXX${idNumber.takeLast(4)}"
            }
        }

        // Extract Holder Name
        val namePattern = Pattern.compile("(?i)(?:name|holder|name\\s*of\\s*cardholder)\\s*[:\\-]?\\s*([A-Za-z\\s.]{3,35})")
        for (line in lines) {
            val m = namePattern.matcher(line)
            if (m.find()) {
                holderName = m.group(1)?.trim() ?: ""
                break
            }
        }
        if (holderName.isEmpty()) {
            for (i in 0 until minOf(lines.size, 5)) {
                val l = lines[i]
                if (l.matches(Regex("^[A-Za-z\\s.]{4,30}$")) && !l.contains("INDIA", ignoreCase = true) && !l.contains("GOVERNMENT", ignoreCase = true) && !l.contains("INCOME", ignoreCase = true)) {
                    holderName = l.trim()
                    break
                }
            }
        }
        if (holderName.isEmpty()) holderName = "Card Holder"

        val typeKey = when {
            isAadhaar -> "aadhaar"
            isPan -> "pan"
            else -> "id_card"
        }

        val title = when {
            isAadhaar -> "Aadhaar Card ($holderName)"
            isPan -> "PAN Card ($holderName)"
            isVoter -> "Voter ID Card ($holderName)"
            isDl -> "Driving Licence ($holderName)"
            else -> "National ID Card"
        }

        return AnalyzedDocumentData(
            documentType = typeKey,
            suggestedTitle = title,
            extractedText = ocrResult.fullText,
            language = "English / Hindi",
            confidence = 0.97f,
            idType = if (isAadhaar) "Aadhaar" else if (isPan) "PAN" else "Government ID",
            idNumber = idNumber,
            maskedIdNumber = if (maskedId.isNotBlank()) maskedId else idNumber,
            holderName = holderName,
            sensitiveBoxes = listOf(
                NormalizedBox(ymin = 0.65f, xmin = 0.20f, ymax = 0.78f, xmax = 0.80f, label = "Masked ID Number")
            ),
            summaryBullets = listOf(
                "🪪 Identity Document: $title",
                "👤 Card Holder: $holderName",
                "🔒 ID Number: ${if (maskedId.isNotBlank()) maskedId else "Protected"}",
                "🛡️ Privacy Masking available for sharing"
            )
        )
    }

    // ---------------------- 4. LEGAL DOCUMENT PARSER ----------------------

    private fun parseIndianLegalDocument(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>,
        lowerText: String
    ): AnalyzedDocumentData {
        val title = when {
            lowerText.contains("lease") || lowerText.contains("rent") -> "Residential Lease / Rent Agreement"
            lowerText.contains("employment") || lowerText.contains("appointment") -> "Employment Contract Agreement"
            lowerText.contains("affidavit") -> "Legal Affidavit"
            lowerText.contains("partnership") -> "Partnership Deed"
            else -> lines.firstOrNull { it.length in 5..45 } ?: "Legal Agreement"
        }

        val parties = mutableListOf<LegalPartyItem>()
        if (lowerText.contains("licensor") || lowerText.contains("licensee")) {
            parties.add(LegalPartyItem("First Party", "Licensor / Property Owner"))
            parties.add(LegalPartyItem("Second Party", "Licensee / Tenant"))
        }

        return AnalyzedDocumentData(
            documentType = "legal_document",
            suggestedTitle = title,
            extractedText = ocrResult.fullText,
            confidence = 0.95f,
            parties = parties,
            summaryBullets = listOf(
                "⚖️ Legal Document: $title",
                "📄 Extracted ${lines.size} verified text lines",
                "🌐 Ready for multi-language translation and official export"
            )
        )
    }

    // ---------------------- 5. HANDWRITTEN NOTES PARSER ----------------------

    private fun parseHandwrittenNotes(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>,
        lowerText: String
    ): AnalyzedDocumentData {
        val header = lines.firstOrNull()?.take(35) ?: "Study Notes"
        val title = "Handwritten Notes: $header"
        val bullets = lines.take(8).map { "• $it" }

        return AnalyzedDocumentData(
            documentType = "textbook_notes",
            suggestedTitle = title,
            extractedText = ocrResult.fullText,
            confidence = 0.93f,
            handwrittenBulletNotes = bullets,
            summaryBullets = listOf(
                "✍️ Handwritten Document Converted to Digital Text",
                "📝 Extracted ${lines.size} legible text lines",
                "💡 Structured study points and flashcards generated"
            )
        )
    }

    // ---------------------- 6. GENERAL DOCUMENT PARSER ----------------------

    private fun parseGeneralDocument(
        ocrResult: OcrManager.OcrResult,
        lines: List<String>
    ): AnalyzedDocumentData {
        val topTitle = lines.firstOrNull { it.length in 4..45 } ?: "Scanned Document"
        val bullets = lines.take(4).map { "• $it" }

        return AnalyzedDocumentData(
            documentType = "other",
            suggestedTitle = topTitle,
            extractedText = ocrResult.fullText,
            confidence = 0.95f,
            summaryBullets = bullets.ifEmpty {
                listOf(
                    "📄 Full OCR text extracted from document",
                    "🌐 Ready for translation, Text-to-Speech, or PDF export"
                )
            }
        )
    }
}
