package com.example.business.budget.api.model

data class BudgetSetting(
    val totalBudgetInCents: Long,
    val categoryBudgets: Map<String, Long>,
    val reminderEnabled: Boolean
)

data class CategoryBudgetUsage(
    val name: String,
    val budgetInCents: Long,
    val usedInCents: Long
) {
    val usagePercent: Int
        get() = if (budgetInCents == 0L) {
            0
        } else {
            (
                usedInCents * 100 / budgetInCents
            ).toInt()
        }
}

data class BudgetOverview(
    val totalBudgetInCents: Long = 0,
    val usedInCents: Long = 0,
    val remainingInCents: Long = 0,
    val reminderEnabled: Boolean = true,
    val categories: List<CategoryBudgetUsage> = emptyList()
) {
    val usagePercent: Int
        get() = if (totalBudgetInCents == 0L) {
            0
        } else {
            (usedInCents * 100 / totalBudgetInCents).toInt()
        }
}
