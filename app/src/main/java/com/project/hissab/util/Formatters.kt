package com.project.hissab.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDate

object Formatters {
    private val india = Locale("en", "IN")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd MMM, h:mm a", Locale.ENGLISH)
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val oneDecimal = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH))

    private val dayFormat = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH)
    private val dayMonthFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

    fun rupees(value: Double, decimals: Boolean = true): String =
        "₹" + NumberFormat.getNumberInstance(india).apply {
            minimumFractionDigits = if (decimals) 2 else 0
            maximumFractionDigits = if (decimals) 2 else 0
        }.format(value)

    /** Short form for chart labels: ₹850, ₹12.5K, ₹1.2L. Empty for zero. */
    fun compactRupees(value: Double): String = when {
        value <= 0.0 -> ""
        value >= 100_000 -> "₹" + oneDecimal.format(value / 100_000) + "L"
        value >= 1_000 -> "₹" + oneDecimal.format(value / 1_000) + "K"
        else -> "₹" + value.toLong()
    }

    fun dateTime(t: LocalDateTime): String = t.format(dateTimeFormat)
    fun month(m: YearMonth): String = m.format(monthFormat)
    fun shortMonth(m: YearMonth): String = m.format(shortMonthFormat)

    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today, ${date.format(dayMonthFormat)}"
        today.minusDays(1) -> "Yesterday, ${date.format(dayMonthFormat)}"
        else -> date.format(dayFormat)
    }
}