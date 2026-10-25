package com.project.hissab.data.local

import com.project.hissab.domain.model.Expense
import com.project.hissab.domain.model.PaymentMethod
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

// The date is stored as the wall-clock time you picked, so changing the phone's time zone never shifts old entries.
fun Expense.toEntity() = ExpenseEntity(
    id = id,
    title = title,
    amount = amount,
    paymentMethod = paymentMethod.name,
    categoryId = categoryId,
    dateTime = dateTime.toEpochSecond(ZoneOffset.UTC),
    isUnnecessary = isUnnecessary
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    title = title,
    amount = amount,
    paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.UPI),
    categoryId = categoryId,
    dateTime = LocalDateTime.ofEpochSecond(dateTime, 0, ZoneOffset.UTC),
    isUnnecessary = isUnnecessary
)

fun YearMonth.toEpochSecondRange(): Pair<Long, Long> = Pair(
    atDay(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC),
    plusMonths(1).atDay(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
)