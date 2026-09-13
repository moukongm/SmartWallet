package com.example.business.budget.api.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetModelsTest {

    @Test
    fun usagePercent_isCalculatedFromCents() {
        val usage = CategoryBudgetUsage(
            name = "餐饮",
            budgetInCents = 150_000L,
            usedInCents = 105_000L
        )

        assertEquals(70, usage.usagePercent)
    }

    @Test
    fun usagePercent_isZeroWhenBudgetIsZero() {
        val overview = BudgetOverview(
            totalBudgetInCents = 0L,
            usedInCents = 35_000L
        )

        assertEquals(0, overview.usagePercent)
    }

    @Test
    fun usagePercent_canRepresentOverspending() {
        val overview = BudgetOverview(
            totalBudgetInCents = 500_000L,
            usedInCents = 600_000L,
            remainingInCents = -100_000L
        )

        assertEquals(120, overview.usagePercent)
        assertEquals(-100_000L, overview.remainingInCents)
    }
}
