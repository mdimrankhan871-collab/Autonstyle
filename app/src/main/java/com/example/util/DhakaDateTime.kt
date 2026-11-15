package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DhakaDateTime {
    val ZONE: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")
    private const val DATE_PATTERN = "dd-MM-yyyy"
    private const val DATE_TIME_PATTERN = "dd-MM-yyyy hh:mm a"
    private const val INVOICE_DAY_PATTERN = "yyyyMMdd"

    fun todayString(nowMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        sdf.timeZone = ZONE
        return sdf.format(Date(nowMs))
    }

    fun formatDateTime(epochMs: Long): String {
        if (epochMs <= 0L) return "-"
        val sdf = SimpleDateFormat(DATE_TIME_PATTERN, Locale.US)
        sdf.timeZone = ZONE
        return sdf.format(Date(epochMs))
    }

    fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return "-"
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        sdf.timeZone = ZONE
        return sdf.format(Date(epochMs))
    }

    fun parseDateOrNow(dateStr: String): Long {
        val trimmed = dateStr.trim()
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            timeZone = ZONE
            isLenient = false
        }
        return try {
            val parsed = sdf.parse(trimmed) ?: return System.currentTimeMillis()
            val cal = Calendar.getInstance(ZONE).apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun isValidDateFormat(dateStr: String): Boolean {
        val trimmed = dateStr.trim()
        if (!Regex("^\\d{2}-\\d{2}-\\d{4}$").matches(trimmed)) return false
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            timeZone = ZONE
            isLenient = false
        }
        return try {
            sdf.parse(trimmed) != null
        } catch (_: Exception) {
            false
        }
    }

    fun generateInvoiceNumber(nowMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat(INVOICE_DAY_PATTERN, Locale.US).apply { timeZone = ZONE }
        val suffix = ((nowMs / 100L) % 10000L).toString().padStart(4, '0')
        return "AS-${sdf.format(Date(nowMs))}-$suffix"
    }

    fun startOfTodayMs(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(ZONE).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfWeekMs(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(ZONE).apply {
            timeInMillis = nowMs
            firstDayOfWeek = Calendar.SATURDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > nowMs) {
            cal.add(Calendar.DAY_OF_YEAR, -7)
        }
        return cal.timeInMillis
    }

    fun startOfMonthMs(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(ZONE).apply {
            timeInMillis = nowMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun monthYearLabel(epochMs: Long): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.US).apply { timeZone = ZONE }
        return sdf.format(Date(epochMs))
    }

    fun monthDisplayName(monthYear: String): String {
        return try {
            val inFmt = SimpleDateFormat("MM-yyyy", Locale.US).apply { timeZone = ZONE }
            val outFmt = SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = ZONE }
            val date = inFmt.parse(monthYear) ?: return monthYear
            outFmt.format(date)
        } catch (_: Exception) {
            monthYear
        }
    }
}

object BdtFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val wholeFormat = DecimalFormat("#,##0", symbols)
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    fun format(amount: Double, showDecimals: Boolean = false): String {
        val neg = amount < 0.0
        val absVal = kotlin.math.abs(amount)
        val formatted = if (showDecimals || (absVal % 1.0) > 0.004) {
            decimalFormat.format(absVal)
        } else {
            wholeFormat.format(absVal)
        }
        return if (neg) "-৳$formatted" else "৳$formatted"
    }

    fun formatPlain(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
