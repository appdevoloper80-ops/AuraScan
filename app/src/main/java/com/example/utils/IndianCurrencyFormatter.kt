package com.example.utils

import java.text.DecimalFormat
import java.util.Locale

object IndianCurrencyFormatter {

    /**
     * Formats an amount into the Indian number system:
     * e.g. 123456.78 -> "₹1,23,456.78"
     * e.g. 5000 -> "₹5,000.00"
     */
    fun formatRupees(amount: Double, includeDecimals: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        
        val wholePart = absAmount.toLong()
        val decimalPart = String.format(Locale.US, "%.2f", absAmount - wholePart).substring(2)

        val wholeStr = wholePart.toString()
        val formattedWhole = if (wholeStr.length <= 3) {
            wholeStr
        } else {
            val lastThree = wholeStr.substring(wholeStr.length - 3)
            val rest = wholeStr.substring(0, wholeStr.length - 3)
            val chunked = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                chunked.insert(0, rest[i])
                count++
                if (count == 2 && i > 0) {
                    chunked.insert(0, ",")
                    count = 0
                }
            }
            "$chunked,$lastThree"
        }

        val sign = if (isNegative) "-" else ""
        return if (includeDecimals) {
            "${sign}₹$formattedWhole.$decimalPart"
        } else {
            "${sign}₹$formattedWhole"
        }
    }
}
