package com.project.hissab.domain.usecase

import java.time.YearMonth

/** The month whose budget applies to [month]: itself if set, otherwise the latest earlier one. */
internal fun Map<YearMonth, Double>.sourceMonthFor(month: YearMonth): YearMonth? =
    keys.filter { it <= month }.maxOrNull()

internal fun Map<YearMonth, Double>.budgetFor(month: YearMonth): Double? =
    sourceMonthFor(month)?.let { this[it] }