package com.millionairequiz.app.game

import kotlin.math.floor
import kotlin.random.Random

enum class Confidence { SURE, FAIRLY_SURE, GUESSING, NO_IDEA }

/** What the friend will say: which option they lean to (null = no idea) and how sure they are. */
data class FriendPlan(val leanIndex: Int?, val confidence: Confidence)

/**
 * The game decides locally how reliable each lifeline is, so they behave like the show
 * (helpful early on, shaky near the top). The model only supplies the words and the
 * plausibility of wrong answers; it never decides whether a lifeline gives the game away.
 */
object Lifelines {

    /** Removes two wrong answers at random. */
    fun fiftyFifty(question: Question, random: Random): Set<Int> =
        (0..3).filter { it != question.correctIndex }.shuffled(random).take(2).toSet()

    /** Chance the friend genuinely knows the answer: ~95% on question 1, ~46% on question 15. */
    fun friendKnowsProbability(level: Int): Double = (0.95 - level * 0.035).coerceIn(0.3, 0.95)

    fun planFriend(question: Question, removed: Set<Int>, level: Int, random: Random): FriendPlan {
        if (random.nextDouble() < friendKnowsProbability(level)) {
            val confidence = if (random.nextDouble() < 0.65) Confidence.SURE else Confidence.FAIRLY_SURE
            return FriendPlan(question.correctIndex, confidence)
        }
        if (random.nextDouble() < 0.15) return FriendPlan(null, Confidence.NO_IDEA)
        // A guess among whatever is still on the board (could be lucky).
        val pick = (0..3).filter { it !in removed }.random(random)
        val confidence = if (random.nextDouble() < 0.75) Confidence.GUESSING else Confidence.FAIRLY_SURE
        return FriendPlan(pick, confidence)
    }

    /** Share of the audience that votes correctly: ~78% at question 1 down to ~29% at question 15, with noise. */
    fun audienceCorrectShare(level: Int, optionsInPlay: Int, random: Random): Double {
        var share = 0.78 - level * 0.035 + (random.nextDouble() - 0.5) * 0.2
        if (optionsInPlay == 2) share += (1 - share) * 0.35 // fewer options, more people find it
        return share.coerceIn(0.12, 0.95)
    }

    /**
     * Percentages for A-D (removed options get 0), summing to 100.
     * [weights] maps each wrong option in play to how tempting the model thinks it is (0-10).
     */
    fun audienceVotes(
        question: Question,
        removed: Set<Int>,
        level: Int,
        weights: Map<Int, Double>,
        random: Random,
    ): List<Int> {
        val inPlay = (0..3).filter { it !in removed }
        val wrongInPlay = inPlay.filter { it != question.correctIndex }
        val correctShare = audienceCorrectShare(level, inPlay.size, random)

        val raw = DoubleArray(4)
        raw[question.correctIndex] = correctShare
        val w = wrongInPlay.associateWith { (weights[it] ?: 3.0) + 1.0 + random.nextDouble() * 1.5 }
        val total = w.values.sum()
        wrongInPlay.forEach { raw[it] = (1 - correctShare) * w.getValue(it) / total }
        return toPercentages(raw)
    }

    /** Rounds shares to whole percentages that add up to exactly 100 (largest-remainder method). */
    fun toPercentages(raw: DoubleArray): List<Int> {
        val sum = raw.sum()
        if (sum <= 0) return raw.map { 0 }
        val scaled = raw.map { it / sum * 100 }
        val floors = scaled.map { floor(it).toInt() }.toMutableList()
        var remaining = 100 - floors.sum()
        scaled.indices
            .sortedByDescending { scaled[it] - floors[it] }
            .forEach { i ->
                if (remaining > 0 && raw[i] > 0) {
                    floors[i]++
                    remaining--
                }
            }
        return floors
    }
}
