package com.project.hissab.domain.model

/** The categories every user gets. They live in code, so they can't be lost or edited. */
object BuiltInCategories {
    val ALL: List<Category> = listOf(
        Category.NONE,                                                         // always first
        Category("groceries_food", "Groceries & Food", "🛒", 0xFF2F8F5B.toInt()),
        Category("transportation", "Transportation", "🚗", 0xFF3B82F6.toInt()),
        Category("personal_care_clothing", "Personal Care & Clothing", "👕", 0xFFEC4899.toInt()),
        Category("online_food", "Online Food", "🍔", 0xFFD946EF.toInt()),
        Category("quick_delivery", "10 Min Delivery", "⚡", 0xFF06B6D4.toInt()),
        Category("dining", "Dining", "🍴", 0xFF64748B.toInt()),
        Category("entertainment", "Entertainment", "🎬", 0xFF8B5CF6.toInt()),
        Category("health_care", "Health Care", "🩺", 0xFFEF4444.toInt()),
        Category("bills",  "Bills", "🧾", 0xFFF59E0B.toInt()),
        Category("family", "Family", "👨‍👩‍👧", 0xFFF97316.toInt()),
        Category("savings_misc", "Savings & Misc", "🐖", 0xFF14B8A6.toInt()),
        Category("housing", "Housing", "🏠", 0xFF6366F1.toInt()),
    )
}