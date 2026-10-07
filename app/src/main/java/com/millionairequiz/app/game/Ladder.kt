package com.millionairequiz.app.game

import java.util.Locale

/** The classic UK 15-question money ladder. Levels are 0-based (level 0 = question 1). */
object Ladder {
    val amounts = listOf(
        100, 200, 300, 500, 1_000,
        2_000, 4_000, 8_000, 16_000, 32_000,
        64_000, 125_000, 250_000, 500_000, 1_000_000,
    )

    const val LAST_LEVEL = 14

    /** Questions 5 (£1,000) and 10 (£32,000) are the safe havens. */
    val safeHavens = setOf(4, 9)

    fun format(amount: Int): String = String.format(Locale.UK, "£%,d", amount)

    /** What you leave with if you get [level] wrong. */
    fun guaranteedAfterWrong(level: Int): Int = when {
        level > 9 -> amounts[9]
        level > 4 -> amounts[4]
        else -> 0
    }

    /** What you leave with if you walk away instead of answering [level]. */
    fun walkAwayAmount(level: Int): Int = if (level <= 0) 0 else amounts[level - 1]
}
