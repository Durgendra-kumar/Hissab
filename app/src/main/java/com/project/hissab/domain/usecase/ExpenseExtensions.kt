package com.project.hissab.domain.usecase

import com.project.hissab.domain.model.Expense
import java.time.YearMonth

internal fun List<Expense>.inMonth(month: YearMonth): List<Expense> =
    filter { YearMonth.from(it.dateTime) == month }