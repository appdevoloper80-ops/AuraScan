package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object QrCodeGenerator {

    /**
     * Generates a 2D matrix QR-style bitmap for UPI payment links.
     * Includes standard QR finder patterns at top-left, top-right, bottom-left corners,
     * timing tracks, and pseudo-random hashed data cells determined by the input content.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val matrixSize = 29 // 29x29 standard QR grid
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }

        // Draw Finder Patterns (7x7) at (0,0), (0, matrixSize-7), (matrixSize-7, 0)
        fun drawFinder(startX: Int, startY: Int) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                    val isCenter = x in 2..4 && y in 2..4
                    grid[startY + y][startX + x] = isBorder || isCenter
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(matrixSize - 7, 0)
        drawFinder(0, matrixSize - 7)

        // Draw Timing patterns
        for (i in 8 until matrixSize - 8) {
            if (i % 2 == 0) {
                grid[6][i] = true
                grid[i][6] = true
            }
        }

        // Fill remaining cells using deterministic hash of content
        val bytes = content.toByteArray(Charsets.UTF_8)
        var byteIdx = 0
        var bitIdx = 0

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                // Skip finder areas
                val inTopLeft = x < 8 && y < 8
                val inTopRight = x >= matrixSize - 8 && y < 8
                val inBottomLeft = x < 8 && y >= matrixSize - 8
                val inTiming = (y == 6 && x >= 8 && x < matrixSize - 8) || (x == 6 && y >= 8 && y < matrixSize - 8)

                if (inTopLeft || inTopRight || inBottomLeft || inTiming) continue

                val currentByte = bytes[byteIdx % bytes.size].toInt()
                val isBitSet = ((currentByte shr (bitIdx % 8)) and 1) == 1
                grid[y][x] = isBitSet xor ((x + y) % 3 == 0)

                bitIdx++
                if (bitIdx % 8 == 0) byteIdx++
            }
        }

        // Render to Bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val padding = size * 0.08f
        val innerSize = size - (padding * 2f)
        val cellSize = innerSize / matrixSize

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (grid[y][x]) {
                    val left = padding + (x * cellSize)
                    val top = padding + (y * cellSize)
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, paint)
                }
            }
        }

        return bitmap
    }
}
