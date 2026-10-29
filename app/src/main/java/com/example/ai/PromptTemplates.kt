package com.example.ai

object PromptTemplates {
    const val DOCUMENT_ANALYSIS_SYSTEM_PROMPT = """
You are AuraScan AI, an expert Indian document analysis system.
Analyze the provided document image with high precision.
Determine its exact category from: ["mark_sheet", "aadhaar", "pan", "bill_invoice", "textbook_notes", "legal_document", "handwritten_note", "other"].

Pay special attention to Indian educational documents such as CBSE, ICSE, and State Board (10th / Secondary, 12th / Higher Secondary) Mark Sheets, Transcripts, and Degree Certificates.

You MUST respond strictly with a valid JSON object matching this schema:
{
  "documentType": "mark_sheet | aadhaar | pan | bill_invoice | textbook_notes | legal_document | handwritten_note | other",
  "suggestedTitle": "Specific descriptive title, e.g. 'CBSE Class 10th Mark Sheet' or 'Store Tax Invoice'",
  "extractedText": "Complete readable text extracted from the document",
  "language": "Primary language of document (e.g., English, Hindi, Tamil)",
  "confidence": 0.95,
  "marksheetDetails": {
    "boardName": "CBSE / ICSE / State Board name",
    "examinationName": "Secondary School Examination (Class X) / 10th Standard",
    "candidateName": "Student Full Name",
    "rollNumber": "Roll / Registration Number",
    "schoolName": "School / Institution Name",
    "subjects": [
      {"name": "Subject name", "price": 85.0}
    ],
    "totalMarks": "440 / 500",
    "percentageOrCgpa": "88%",
    "resultStatus": "PASS / FIRST DIVISION"
  },
  "billDetails": {
    "merchantName": "Store/vendor name",
    "invoiceDate": "DD/MM/YYYY or YYYY-MM-DD",
    "lineItems": [
      {"name": "Item name", "qty": 1.0, "price": 150.0}
    ],
    "subtotal": 150.0,
    "taxBreakdown": "CGST 2.5% + SGST 2.5% or GST amount",
    "totalAmount": 157.5,
    "suggestedExpenseCategory": "Food | Travel | Shopping | Utilities | Other"
  },
  "idDetails": {
    "idType": "Aadhaar | PAN",
    "idNumber": "Detected ID number",
    "maskedIdNumber": "Masked format, e.g. 'XXXX-XXXX-1234' for Aadhaar or 'ABCDE1234F' for PAN",
    "holderName": "Name found on ID",
    "sensitiveBoxes": [
      {
        "ymin": 0.55,
        "xmin": 0.20,
        "ymax": 0.62,
        "xmax": 0.70,
        "label": "First 8 digits of Aadhaar or PAN number"
      }
    ]
  },
  "notesDetails": {
    "summaryBullets": [
      "Key bullet 1",
      "Key bullet 2",
      "Key bullet 3"
    ],
    "flashcards": [
      {"id": "1", "question": "Question 1", "answer": "Answer 1"},
      {"id": "2", "question": "Question 2", "answer": "Answer 2"},
      {"id": "3", "question": "Question 3", "answer": "Answer 3"},
      {"id": "4", "question": "Question 4", "answer": "Answer 4"},
      {"id": "5", "question": "Question 5", "answer": "Answer 5"},
      {"id": "6", "question": "Question 6", "answer": "Answer 6"},
      {"id": "7", "question": "Question 7", "answer": "Answer 7"},
      {"id": "8", "question": "Question 8", "answer": "Answer 8"}
    ],
    "keyTerms": ["Term 1", "Term 2", "Term 3", "Term 4"]
  },
    "legalDetails": {
      "summaryBullets": [
        "Concise plain-English point 1",
        "Concise plain-English point 2",
        "Concise plain-English point 3",
        "Concise plain-English point 4",
        "Concise plain-English point 5"
      ],
      "parties": [
        {"name": "Party name", "role": "Tenant / Landlord / Employer / Licensor"}
      ],
      "keyDates": [
        {"date": "DD/MM/YYYY", "description": "Commencement / Lock-in / Notice Period"}
      ],
      "amounts": [
        "Monthly Rent: ₹25,000",
        "Security Deposit: ₹50,000"
      ],
      "obligations": [
        "Timely payment by 5th of month",
        "Premises maintenance"
      ],
      "thingsToCheck": [
        {
          "category": "Penalties | Auto-renewal | Termination | Jurisdiction | Other",
          "title": "Short title of clause",
          "description": "Specific clause risk explanation",
          "severity": "Warning | Attention | Notice"
        }
      ]
    },
    "handwrittenDetails": {
      "confidenceNote": "High clarity (94%)",
      "suggestedBullets": [
        "Point 1",
        "Point 2"
      ]
    }
  }
}
Return only JSON with no markdown wrapping or preamble.
"""

    const val CORNER_DETECTION_PROMPT = """
Analyze the document in the image and detect its four outer corner points (paper corners) in normalized coordinates between 0.0 and 1.0.
Order of corners MUST be:
1. Top-Left
2. Top-Right
3. Bottom-Right
4. Bottom-Left

Return strictly a JSON object:
{
  "corners": [
    {"x": 0.05, "y": 0.05},
    {"x": 0.95, "y": 0.05},
    {"x": 0.95, "y": 0.95},
    {"x": 0.05, "y": 0.95}
  ]
}
"""

    fun buildMoreFlashcardsPrompt(extractedText: String, count: Int = 8): String {
        return """
Based on the following extracted document text:
"$extractedText"

Generate $count new, high-yield study flashcards with clear questions and concise answers.
Return strictly a JSON object:
{
  "flashcards": [
    {"id": "1", "question": "Question text", "answer": "Answer text"}
  ]
}
"""
    }

    fun buildTranslatePrompt(text: String, targetLanguage: String): String {
        return """
Translate the following text into $targetLanguage:
"$text"

Return strictly a JSON object:
{
  "translatedText": "Your translation here"
}
"""
    }

    fun buildTranslateWithKeyTermsPrompt(summaryText: String, keyTerms: List<String>, targetLanguage: String): String {
        val termsStr = keyTerms.joinToString(", ")
        return """
Translate the following summary text into $targetLanguage accurately, maintaining Indian contextual phrasing:
"$summaryText"

Also for each of these key terms: [$termsStr], provide:
- The translated term in $targetLanguage
- A clear, simple one-line meaning/definition in $targetLanguage

Return strictly a JSON object:
{
  "translatedSummary": "Full translated summary here",
  "keyTerms": [
    {
      "term": "Original term",
      "translation": "Translated term in $targetLanguage",
      "meaning": "One line explanation in $targetLanguage"
    }
  ]
}
"""
    }

    fun buildAskPagePrompt(extractedText: String, userQuestion: String): String {
        return """
You are AuraScan AI's "Ask this page" study assistant.
Answer the user's question using ONLY the provided extracted text from the scanned document page.
Do NOT extrapolate beyond the text. Keep your answer friendly, direct, and concise (under 3-4 sentences).
If the text does not contain the information, respond politely: "The scanned page does not mention this information."

DOCUMENT TEXT:
$extractedText

USER QUESTION:
$userQuestion
"""
    }

    fun buildCleanUpTextPrompt(rawText: String): String {
        return """
You are an expert OCR cleanup assistant. Correct all OCR transcription noise, missing spaces, misread letters, typos, and weird line breaks from this scanned text. Preserve the original meaning and structure faithfully without adding fabricated information.

RAW OCR TEXT:
"$rawText"

Return strictly a JSON object:
{
  "cleanedText": "Accurately cleaned and polished text here"
}
"""
    }

    fun buildConvertToBulletsPrompt(rawText: String): String {
        return """
Convert the following scanned notes or document text into a clean, structured set of bullet notes grouped with brief headings or key points:
"$rawText"

Return strictly a JSON object:
{
  "bulletNotes": [
    "Key takeaway or point 1",
    "Key takeaway or point 2",
    "Key takeaway or point 3"
  ]
}
"""
    }

    fun buildTranslateLegalPrompt(bullets: List<String>, targetLanguage: String): String {
        val joined = bullets.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
        return """
Translate these 5 legal summary bullet points into $targetLanguage accurately, maintaining legal terminology and clarity:
$joined

Return strictly a JSON object:
{
  "translatedBullets": [
    "Point 1 in $targetLanguage",
    "Point 2 in $targetLanguage",
    "Point 3 in $targetLanguage",
    "Point 4 in $targetLanguage",
    "Point 5 in $targetLanguage"
  ]
}
"""
    }

    const val RE_OCR_REGION_PROMPT = """
You are an OCR specialist. Transcribe all printed or handwritten words inside this cropped image area with utmost fidelity.
Return strictly a JSON object:
{
  "recognizedText": "Transcribed text from the selected region"
}
"""
}
