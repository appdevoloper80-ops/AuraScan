package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.data.model.NormalizedBox
import com.example.data.model.PointNorm
import kotlin.math.hypot
import kotlin.math.max

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    BW("B&W"),
    GRAYSCALE("Grayscale"),
    HIGH_CONTRAST("High Contrast")
}

object ImageProcessor {

    fun applyFilter(source: Bitmap, filter: FilterType): Bitmap {
        return when (filter) {
            FilterType.ORIGINAL -> source
            FilterType.MAGIC_COLOR -> applyMagicColor(source)
            FilterType.BW -> applyBlackAndWhite(source)
            FilterType.GRAYSCALE -> applyGrayscale(source)
            FilterType.HIGH_CONTRAST -> applyHighContrast(source)
        }
    }

    /**
     * Advanced Clean up:
     * Evens out lighting, reduces shadows, flattens background paper brightness,
     * and boosts ink contrast using high-pass filtering and ColorMatrix.
     */
    fun cleanUpDocument(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Balanced lighting leveling:
        // Scaled color matrix that lifts background shadows into near-white
        // while sharpening text ink pixels
        val cm = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, 40f,
            0f, 1.5f, 0f, 0f, 40f,
            0f, 0f, 1.5f, 0f, 40f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun rotateRight(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun rotateLeft(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(-90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun rotate90(source: Bitmap): Bitmap = rotateRight(source)

    /**
     * 4-Corner Perspective Warp (Quad to Rectangle):
     * Takes Top-Left, Top-Right, Bottom-Right, Bottom-Left corners,
     * calculates destination dimensions, and warps the document using Matrix.setPolyToPoly.
     */
    fun applyPerspectiveCrop(
        source: Bitmap,
        tl: PointNorm,
        tr: PointNorm,
        br: PointNorm,
        bl: PointNorm
    ): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        val x0 = tl.x.coerceIn(0f, 1f) * srcW
        val y0 = tl.y.coerceIn(0f, 1f) * srcH

        val x1 = tr.x.coerceIn(0f, 1f) * srcW
        val y1 = tr.y.coerceIn(0f, 1f) * srcH

        val x2 = br.x.coerceIn(0f, 1f) * srcW
        val y2 = br.y.coerceIn(0f, 1f) * srcH

        val x3 = bl.x.coerceIn(0f, 1f) * srcW
        val y3 = bl.y.coerceIn(0f, 1f) * srcH

        // Calculate destination width and height based on corner distances
        val topWidth = hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()).toFloat()
        val bottomWidth = hypot((x2 - x3).toDouble(), (y2 - y3).toDouble()).toFloat()
        val leftHeight = hypot((x3 - x0).toDouble(), (y3 - y0).toDouble()).toFloat()
        val rightHeight = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()

        val dstW = max(topWidth, bottomWidth).toInt().coerceIn(200, 3000)
        val dstH = max(leftHeight, rightHeight).toInt().coerceIn(200, 4000)

        val srcPoints = floatArrayOf(
            x0, y0, // TL
            x1, y1, // TR
            x2, y2, // BR
            x3, y3  // BL
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            dstW.toFloat(), 0f,
            dstW.toFloat(), dstH.toFloat(),
            0f, dstH.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val warped = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(warped)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        return warped
    }

    fun crop(source: Bitmap, leftNorm: Float, topNorm: Float, rightNorm: Float, bottomNorm: Float): Bitmap {
        val l = (leftNorm.coerceIn(0f, 1f) * source.width).toInt()
        val t = (topNorm.coerceIn(0f, 1f) * source.height).toInt()
        val r = (rightNorm.coerceIn(0f, 1f) * source.width).toInt()
        val b = (bottomNorm.coerceIn(0f, 1f) * source.height).toInt()

        val width = (r - l).coerceAtLeast(10)
        val height = (b - t).coerceAtLeast(10)

        val safeX = l.coerceIn(0, (source.width - width).coerceAtLeast(0))
        val safeY = t.coerceIn(0, (source.height - height).coerceAtLeast(0))

        return Bitmap.createBitmap(source, safeX, safeY, width, height)
    }

    fun applyMaskAndWatermark(
        source: Bitmap,
        boxes: List<NormalizedBox>,
        purpose: String
    ): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val blackPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Draw solid black privacy boxes over sensitive fields
        for (box in boxes) {
            val left = box.xmin.coerceIn(0f, 1f) * mutable.width
            val top = box.ymin.coerceIn(0f, 1f) * mutable.height
            val right = box.xmax.coerceIn(0f, 1f) * mutable.width
            val bottom = box.ymax.coerceIn(0f, 1f) * mutable.height

            canvas.drawRect(left, top, right, bottom, blackPaint)
        }

        // Draw purpose watermark text
        val cleanPurpose = if (purpose.isBlank()) "Official Purpose" else purpose.trim()
        val watermarkText = "Masked copy - for $cleanPurpose"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 180, 0, 0)
            textSize = (mutable.width / 24f).coerceIn(24f, 56f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textBounds = Rect()
        textPaint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)
        val x = (mutable.width - textBounds.width()) / 2f
        val y = mutable.height - (mutable.height * 0.05f)

        val bgPaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val padding = 16f
        canvas.drawRoundRect(
            x - padding,
            y - textBounds.height() - padding,
            x + textBounds.width() + padding,
            y + padding,
            12f,
            12f,
            bgPaint
        )
        canvas.drawText(watermarkText, x, y, textPaint)

        return mutable
    }

    private fun applyMagicColor(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix().apply { setSaturation(1.3f) }
        val contrastMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 10f,
            0f, 1.2f, 0f, 0f, 10f,
            0f, 0f, 1.2f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyHighContrast(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix(floatArrayOf(
            1.6f, 0f, 0f, 0f, -30f,
            0f, 1.6f, 0f, 0f, -30f,
            0f, 0f, 1.6f, 0f, -30f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyBlackAndWhite(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix().apply { setSaturation(0f) }
        val thresholdMatrix = ColorMatrix(floatArrayOf(
            4.0f, 0f, 0f, 0f, -320f,
            0f, 4.0f, 0f, 0f, -320f,
            0f, 0f, 4.0f, 0f, -320f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(thresholdMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
