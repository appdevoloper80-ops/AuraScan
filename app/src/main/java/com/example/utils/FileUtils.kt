package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "scan"): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "scans").apply { mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        }

    suspend fun loadBitmapFromFile(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun exportTextFile(context: Context, text: String, title: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val clean = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(dir, "${clean}_extracted.txt")
        file.writeText(text)
        file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Scanned Document") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareText(context: Context, text: String, title: String = "Share Text") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun formatStorageSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes Bytes"
        }
    }

    /**
     * Generates crisp, realistic synthetic Indian test documents so emulator testing works out of the box!
     */
    fun createSampleDocumentBitmap(type: String): Bitmap {
        return when (type) {
            "bill" -> createSampleBillBitmap()
            "aadhaar" -> createSampleAadhaarBitmap()
            "pan" -> createSamplePanBitmap()
            "notes" -> createSampleNotesBitmap()
            "legal" -> createSampleLegalBitmap()
            else -> createSampleBillBitmap()
        }
    }

    private fun createSampleBillBitmap(): Bitmap {
        val width = 900
        val height = 1400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY }

        // Header
        paint.color = Color.rgb(0, 105, 92)
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, paint)

        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("HALDIRAM FOODS INTL", width / 2f, 75f, paint)

        // Store info
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Connaught Place, New Delhi - 110001", width / 2f, 160f, textPaint)
        canvas.drawText("GSTIN: 07AAACH7409R1ZZ | Ph: +91 11 2341 5566", width / 2f, 195f, textPaint)
        canvas.drawText("Tax Invoice No: INV-2026-8941 | Date: 26/10/2026", width / 2f, 230f, textPaint)

        // Divider
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 3f
        }
        canvas.drawLine(40f, 260f, width - 40f, 260f, linePaint)

        // Items Table
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 26f
        canvas.drawText("ITEM DESCRIPTION", 50f, 300f, textPaint)
        canvas.drawText("QTY", 560f, 300f, textPaint)
        canvas.drawText("AMOUNT", 730f, 300f, textPaint)

        canvas.drawLine(40f, 320f, width - 40f, 320f, linePaint)

        val items = listOf(
            Triple("Raj Kachori Special Delight", "1", "₹180.00"),
            Triple("Chole Bhature (2 pcs hot)", "2", "₹320.00"),
            Triple("Gulab Jamun (Plate)", "1", "₹90.00"),
            Triple("Special Masala Chai", "2", "₹80.00"),
            Triple("Mineral Water Bottle 1L", "1", "₹20.00")
        )

        var y = 370f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 25f
        for ((name, qty, amt) in items) {
            canvas.drawText(name, 50f, y, textPaint)
            canvas.drawText(qty, 570f, y, textPaint)
            canvas.drawText(amt, 730f, y, textPaint)
            y += 50f
        }

        canvas.drawLine(40f, y + 20f, width - 40f, y + 20f, linePaint)
        y += 70f

        // Totals
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 26f
        canvas.drawText("Subtotal:  ₹690.00", width - 60f, y, textPaint)
        y += 45f
        canvas.drawText("CGST @ 2.5%:  ₹17.25", width - 60f, y, textPaint)
        y += 45f
        canvas.drawText("SGST @ 2.5%:  ₹17.25", width - 60f, y, textPaint)
        y += 55f

        paint.color = Color.rgb(0, 105, 92)
        paint.textSize = 34f
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL:  ₹724.50", width - 60f, y, paint)

        // Footer note
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 22f
        canvas.drawText("Thank you for your visit! UPI: haldiram@icici", width / 2f, 1300f, textPaint)

        return bitmap
    }

    private fun createSampleAadhaarBitmap(): Bitmap {
        val width = 1200
        val height = 760
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(250, 248, 245))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Tricolor top banner
        paint.color = Color.rgb(255, 153, 51) // Saffron
        canvas.drawRect(0f, 0f, width.toFloat(), 15f, paint)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 15f, width.toFloat(), 30f, paint)
        paint.color = Color.rgb(19, 136, 8) // Green
        canvas.drawRect(0f, 30f, width.toFloat(), 45f, paint)

        // UIDAI Header
        paint.color = Color.rgb(180, 40, 40)
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("भारत सरकार  |  GOVERNMENT OF INDIA", width / 2f, 110f, paint)

        paint.color = Color.rgb(0, 80, 140)
        paint.textSize = 26f
        canvas.drawText("Unique Identification Authority of India", width / 2f, 150f, paint)

        // Photo box
        paint.color = Color.rgb(220, 230, 240)
        canvas.drawRoundRect(RectF(60f, 210f, 280f, 490f), 16f, 16f, paint)
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 24f
        canvas.drawText("PHOTO", 170f, 360f, paint)

        // Holder Details
        paint.color = Color.rgb(30, 30, 30)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("राजेश कुमार शर्मा", 320f, 270f, paint)
        canvas.drawText("Rajesh Kumar Sharma", 320f, 320f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 26f
        paint.color = Color.rgb(70, 70, 70)
        canvas.drawText("जन्म तिथि / DOB: 15/08/1988", 320f, 375f, paint)
        canvas.drawText("लिंग / Gender: पुरुष / Male", 320f, 425f, paint)

        // Aadhaar Number (Sensitive!)
        paint.color = Color.rgb(180, 20, 20)
        paint.textSize = 48f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("4829  3910  8274", width / 2f, 570f, paint)

        // Bottom slogan
        paint.color = Color.rgb(0, 105, 92)
        paint.textSize = 26f
        canvas.drawText("मेरा आधार, मेरी पहचान", width / 2f, 650f, paint)

        // Tricolor bottom
        paint.color = Color.rgb(19, 136, 8)
        canvas.drawRect(0f, height - 15f, width.toFloat(), height.toFloat(), paint)

        return bitmap
    }

    private fun createSamplePanBitmap(): Bitmap {
        val width = 1200
        val height = 760
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(240, 246, 255))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(26, 35, 126)
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("INCOME TAX DEPARTMENT", width / 2f, 90f, paint)
        paint.textSize = 28f
        canvas.drawText("GOVT. OF INDIA", width / 2f, 135f, paint)

        // Photo
        paint.color = Color.rgb(200, 215, 235)
        canvas.drawRoundRect(RectF(60f, 200f, 260f, 460f), 12f, 12f, paint)

        // Details
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 24f
        paint.color = Color.GRAY
        canvas.drawText("Permanent Account Number Card", 300f, 210f, paint)

        paint.color = Color.rgb(180, 0, 0)
        paint.textSize = 44f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ABCDE1234F", 300f, 270f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 28f
        canvas.drawText("Name: PRIYA SUNDARAM", 300f, 335f, paint)
        canvas.drawText("Father's Name: R. SUNDARAM", 300f, 385f, paint)
        canvas.drawText("Date of Birth: 22/04/1992", 300f, 435f, paint)

        return bitmap
    }

    private fun createSampleNotesBitmap(): Bitmap {
        val width = 900
        val height = 1300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(255, 254, 245))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(40, 53, 147)
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Physics Chapter 4: Laws of Motion", 50f, 90f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 50, 50)
            textSize = 26f
        }

        val lines = listOf(
            "1. Newton's First Law (Inertia):",
            "An object remains at rest or in uniform motion unless acted upon",
            "by an external unbalanced force. Known as the law of inertia.",
            "",
            "2. Newton's Second Law (Momentum):",
            "The rate of change of momentum is directly proportional to the applied",
            "force and takes place in the direction of the force: F = m * a.",
            "",
            "3. Newton's Third Law (Action-Reaction):",
            "For every action, there is an equal and opposite reaction.",
            "Forces always occur in matched pairs (Fab = -Fba).",
            "",
            "Key Terms to Remember:",
            "• Momentum (p = mv, SI unit: kg m/s)",
            "• Impulse (J = F * dt, equal to change in momentum)",
            "• Friction: Static friction > Kinetic friction",
            "• Conservation of Linear Momentum in collisions."
        )

        var y = 160f
        for (line in lines) {
            canvas.drawText(line, 50f, y, textPaint)
            y += 45f
        }

        return bitmap
    }

    private fun createSampleLegalBitmap(): Bitmap {
        val width = 900
        val height = 1400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(252, 250, 245))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(136, 14, 79)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("RESIDENTIAL LEASE AGREEMENT", width / 2f, 100f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 25f
        }

        val lines = listOf(
            "This Agreement is executed on this 1st day of November, 2026 at Bengaluru,",
            "BETWEEN: Sri. Ramesh Varma ('Landlord/Lessor')",
            "AND: Smt. Ananya Desai ('Tenant/Lessee')",
            "",
            "1. PREMISES: Flat No. 402, Green Palms, Indiranagar, Bengaluru - 560038.",
            "2. TERM: 11 months commencing from 01/11/2026 to 30/09/2027.",
            "3. RENT: ₹35,000/- per calendar month, payable before 5th of each month.",
            "4. SECURITY DEPOSIT: ₹1,50,000/- refundable at time of vacating.",
            "5. NOTICE PERIOD: Either party may terminate with 1 month written notice.",
            "6. MAINTENANCE: Tenant shall bear monthly society maintenance fees."
        )

        var y = 180f
        for (line in lines) {
            canvas.drawText(line, 50f, y, textPaint)
            y += 50f
        }

        return bitmap
    }
}
