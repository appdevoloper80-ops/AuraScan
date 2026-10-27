package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DocumentType
import com.example.utils.PdfExporter
import com.example.utils.PdfQuality
import com.example.utils.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AuraScan", appName)
    }

    @Test
    fun `document type parsing identifies Indian documents`() {
        assertEquals(DocumentType.AADHAAR, DocumentType.fromKey("aadhaar"))
        assertEquals(DocumentType.PAN, DocumentType.fromKey("pan_card"))
        assertEquals(DocumentType.BILL_INVOICE, DocumentType.fromKey("restaurant_bill"))
        assertEquals(DocumentType.TEXTBOOK_NOTES, DocumentType.fromKey("notes"))
        assertEquals(DocumentType.LEGAL_DOCUMENT, DocumentType.fromKey("legal_agreement"))
    }

    @Test
    fun `UPI QR code generation succeeds`() {
        val qrBitmap = QrCodeGenerator.generateQrBitmap("upi://pay?pa=test@upi&pn=Store&am=100.00&cu=INR", 256)
        assertNotNull(qrBitmap)
        assertEquals(256, qrBitmap.width)
        assertEquals(256, qrBitmap.height)
    }

    @Test
    fun `pdf file size estimate is accurate`() {
        val est = PdfExporter.estimateFileSizeKb(2, PdfQuality.MEDIUM)
        assertTrue(est >= 150)
    }
}
