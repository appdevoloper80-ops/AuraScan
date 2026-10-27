package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class PdfQuality(val displayName: String, val maxDimension: Int, val jpegQuality: Int, val estimatedKbPerPage: Int) {
    LOW("Low (<1MB for 10 pgs)", 900, 55, 95),
    MEDIUM("Medium (Standard)", 1300, 80, 240),
    HIGH("High (Print Quality)", 1900, 95, 650)
}

enum class PageSizeOption(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (595 × 842 pt)", 595, 842),
    LETTER("Letter (612 × 792 pt)", 612, 792),
    FIT_TO_IMAGE("Fit to image", 0, 0)
}

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF("Standard PDF", "pdf", "application/pdf"),
    SEARCHABLE_PDF("Searchable PDF (OCR Text)", "pdf", "application/pdf"),
    JPG_IMAGES("JPG Images", "jpg", "image/jpeg"),
    PLAIN_TEXT("Plain Text (.txt)", "txt", "text/plain")
}

object PdfExporter {

    suspend fun exportDocument(
        context: Context,
        pages: List<Bitmap>,
        extractedText: String = "",
        fileName: String = "AuraScan_Document",
        format: ExportFormat = ExportFormat.PDF,
        quality: PdfQuality = PdfQuality.MEDIUM,
        pageSize: PageSizeOption = PageSizeOption.A4,
        onProgress: suspend (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (pages.isEmpty() && format != ExportFormat.PLAIN_TEXT) {
            return@withContext Result.failure(IllegalArgumentException("No pages to export"))
        }

        try {
            val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanName = fileName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val timestamp = System.currentTimeMillis()

            when (format) {
                ExportFormat.PLAIN_TEXT -> {
                    onProgress(0.5f)
                    val txtFile = File(outputDir, "${cleanName}_$timestamp.txt")
                    txtFile.writeText(extractedText.ifBlank { "No extracted text available for this document." })
                    onProgress(1.0f)
                    Result.success(txtFile)
                }

                ExportFormat.JPG_IMAGES -> {
                    val firstPage = pages.first()
                    val compressed = compressBitmapToJpeg(firstPage, quality)
                    val jpgFile = File(outputDir, "${cleanName}_p1_$timestamp.jpg")
                    FileOutputStream(jpgFile).use { fos ->
                        compressed.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, fos)
                    }
                    onProgress(1.0f)
                    Result.success(jpgFile)
                }

                ExportFormat.PDF, ExportFormat.SEARCHABLE_PDF -> {
                    val pdfDocument = PdfDocument()
                    val totalPages = pages.size

                    val textLines = if (format == ExportFormat.SEARCHABLE_PDF) {
                        extractedText.split("\n").filter { it.isNotBlank() }
                    } else {
                        emptyList()
                    }

                    for ((index, pageBitmap) in pages.withIndex()) {
                        val progress = (index.toFloat() / totalPages.toFloat()) * 0.9f
                        onProgress(progress)

                        val compressedPage = compressBitmapToJpeg(pageBitmap, quality)

                        val (pageWidth, pageHeight) = when (pageSize) {
                            PageSizeOption.A4 -> Pair(PageSizeOption.A4.widthPt, PageSizeOption.A4.heightPt)
                            PageSizeOption.LETTER -> Pair(PageSizeOption.LETTER.widthPt, PageSizeOption.LETTER.heightPt)
                            PageSizeOption.FIT_TO_IMAGE -> Pair(compressedPage.width, compressedPage.height)
                        }

                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas: Canvas = page.canvas

                        if (pageSize == PageSizeOption.FIT_TO_IMAGE) {
                            canvas.drawBitmap(compressedPage, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                        } else {
                            val margin = 24
                            val (destW, destH) = calculateFitDimensions(
                                compressedPage.width,
                                compressedPage.height,
                                pageWidth - (margin * 2),
                                pageHeight - (margin * 2)
                            )
                            val left = (pageWidth - destW) / 2
                            val top = (pageHeight - destH) / 2
                            val srcRect = Rect(0, 0, compressedPage.width, compressedPage.height)
                            val destRect = Rect(left, top, left + destW, top + destH)
                            canvas.drawBitmap(compressedPage, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        }

                        // Searchable text layer
                        if (format == ExportFormat.SEARCHABLE_PDF && textLines.isNotEmpty()) {
                            val textPaint = Paint().apply {
                                color = Color.argb(1, 0, 0, 0)
                                textSize = 8f
                                isAntiAlias = true
                            }
                            var yPos = 30f
                            val linesForThisPage = textLines.take(40)
                            for (line in linesForThisPage) {
                                if (yPos < pageHeight - 20) {
                                    canvas.drawText(line, 20f, yPos, textPaint)
                                    yPos += 12f
                                }
                            }
                        }

                        pdfDocument.finishPage(page)
                    }

                    onProgress(0.95f)
                    val pdfFile = File(outputDir, "${cleanName}_$timestamp.pdf")
                    FileOutputStream(pdfFile).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfDocument.close()
                    onProgress(1.0f)
                    Result.success(pdfFile)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToPdf(
        context: Context,
        pages: List<Bitmap>,
        fileName: String = "AuraScan_Document",
        quality: PdfQuality = PdfQuality.MEDIUM
    ): Result<File> {
        return exportDocument(
            context = context,
            pages = pages,
            fileName = fileName,
            format = ExportFormat.PDF,
            quality = quality
        )
    }

    suspend fun combineScansToPdf(
        context: Context,
        pageImagePaths: List<String>,
        outputTitle: String = "AuraScan_Combined",
        quality: PdfQuality = PdfQuality.MEDIUM,
        onProgress: suspend (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (pageImagePaths.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No scans selected to combine"))
        }

        try {
            val pdfDocument = PdfDocument()
            val total = pageImagePaths.size

            for ((index, path) in pageImagePaths.withIndex()) {
                onProgress((index.toFloat() / total.toFloat()) * 0.9f)
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val compressed = compressBitmapToJpeg(bitmap, quality)
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            PageSizeOption.A4.widthPt,
                            PageSizeOption.A4.heightPt,
                            index + 1
                        ).create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas

                        val margin = 24
                        val (destW, destH) = calculateFitDimensions(
                            compressed.width,
                            compressed.height,
                            PageSizeOption.A4.widthPt - (margin * 2),
                            PageSizeOption.A4.heightPt - (margin * 2)
                        )
                        val left = (PageSizeOption.A4.widthPt - destW) / 2
                        val top = (PageSizeOption.A4.heightPt - destH) / 2
                        val destRect = Rect(left, top, left + destW, top + destH)
                        canvas.drawBitmap(compressed, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        pdfDocument.finishPage(page)
                    }
                }
            }

            onProgress(0.95f)
            val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = outputTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val combinedFile = File(outputDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(combinedFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            onProgress(1.0f)
            Result.success(combinedFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun estimateSizeFormatted(pageCount: Int, quality: PdfQuality): String {
        val totalKb = pageCount * quality.estimatedKbPerPage
        return if (totalKb >= 1024) {
            String.format(java.util.Locale.US, "%.1f MB", totalKb / 1024f)
        } else {
            "$totalKb KB"
        }
    }

    fun estimateFileSizeKb(pageCount: Int, quality: PdfQuality): Int {
        return pageCount * quality.estimatedKbPerPage
    }

    private fun compressBitmapToJpeg(original: Bitmap, quality: PdfQuality): Bitmap {
        val maxDim = quality.maxDimension
        val scaled = if (original.width > maxDim || original.height > maxDim) {
            val ratio = original.width.toFloat() / original.height.toFloat()
            val w = if (original.width > original.height) maxDim else (maxDim * ratio).toInt()
            val h = if (original.width > original.height) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(original, w, h, true)
        } else {
            original
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, baos)
        val bytes = baos.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun calculateFitDimensions(srcW: Int, srcH: Int, maxW: Int, maxH: Int): Pair<Int, Int> {
        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val maxRatio = maxW.toFloat() / maxH.toFloat()
        return if (srcRatio > maxRatio) {
            Pair(maxW, (maxW / srcRatio).toInt())
        } else {
            Pair((maxH * srcRatio).toInt(), maxH)
        }
    }
}
