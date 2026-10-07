package com.millionairequiz.app.game

import com.millionairequiz.app.data.ClaudeClient
import com.millionairequiz.app.data.ClaudeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    private fun q(text: String, correct: String, vararg wrong: String) =
        """{"question": "$text", "correct": "$correct", "wrong": [${wrong.joinToString { "\"$it\"" }}], "explanation": "Because."}"""

    private val sampleBatch = """
        Sure! ```json
        {"questions": [
          ${q("Which planet is known as the Red Planet?", "Mars", "Venus", "Jupiter", "Saturn")},
          ${q("Which is the largest planet?", "Jupiter", "Mars", "Earth", "Neptune")}
        ]}
        ```
    """.trimIndent()

    @Test
    fun parsesBatchThroughFencesAndShufflesCorrectAnswer() {
        val positions = mutableSetOf<Int>()
        repeat(40) { seed ->
            val batch = Prompts.parseBatch(sampleBatch, Random(seed))
            assertEquals(2, batch.size)
            val first = batch[0]
            assertEquals("Which planet is known as the Red Planet?", first.text)
            assertEquals("Mars", first.options[first.correctIndex])
            assertEquals(4, first.options.toSet().size)
            assertEquals("Jupiter", batch[1].options[batch[1].correctIndex])
            positions += first.correctIndex
        }
        assertEquals("correct answer should land in every slot", setOf(0, 1, 2, 3), positions)
    }

    @Test
    fun batchSkipsBadItemsAndAcceptsBareArrays() {
        val mixed = """{"questions": [
            ${q("Good one?", "Yes", "No", "Maybe", "Never")},
            ${q("Bad one?", "Mars", "mars", "Venus", "venus")},
            ${q("Good one?", "Yes", "No", "Maybe", "Never")},
            ${q("Another?", "A", "B", "C", "D")}
        ]}"""
        assertEquals(listOf("Good one?", "Another?"), Prompts.parseBatch(mixed, Random(1)).map { it.text })
        val bare = "[${q("Bare?", "Yes", "No", "Maybe", "Never")}]"
        assertEquals(1, Prompts.parseBatch(bare, Random(1)).size)
        try {
            Prompts.parseBatch("no json at all", Random(1))
            fail("should have thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun batchPromptListsEachLevelAndPreviousQuestions() {
        val p = Prompts.batchPrompt("Space", 5, 5, listOf("Old question?"), listOf("people", "places"))
        assertTrue("questions 6 to 10" in p)
        assertTrue("6. Worth £2,000" in p && "10. Worth £32,000" in p)
        assertFalse("11. Worth" in p)
        assertTrue("Old question?" in p && "exactly 5 questions" in p)
    }

    @Test
    fun ladderMoneyRules() {
        assertEquals(15, Ladder.amounts.size)
        assertEquals("£1,000,000", Ladder.format(1_000_000))
        assertEquals(0, Ladder.guaranteedAfterWrong(0))
        assertEquals(0, Ladder.guaranteedAfterWrong(4))     // wrong on Q5
        assertEquals(1_000, Ladder.guaranteedAfterWrong(5)) // wrong on Q6
        assertEquals(1_000, Ladder.guaranteedAfterWrong(9))
        assertEquals(32_000, Ladder.guaranteedAfterWrong(10))
        assertEquals(32_000, Ladder.guaranteedAfterWrong(14))
        assertEquals(0, Ladder.walkAwayAmount(0))
        assertEquals(500_000, Ladder.walkAwayAmount(14))
    }

    @Test
    fun fiftyFiftyKeepsCorrectAnswer() {
        val q = Question("Q?", listOf("a", "b", "c", "d"), 2, "")
        repeat(50) { seed ->
            val removed = Lifelines.fiftyFifty(q, Random(seed))
            assertEquals(2, removed.size)
            assertFalse(2 in removed)
        }
    }

    @Test
    fun audienceVotesSumTo100AndSkipRemovedOptions() {
        val q = Question("Q?", listOf("a", "b", "c", "d"), 1, "")
        repeat(200) { seed ->
            val level = seed % 15
            val removed = if (seed % 2 == 0) setOf(0, 3) else emptySet()
            val votes = Lifelines.audienceVotes(q, removed, level, mapOf(0 to 8.0, 2 to 1.0), Random(seed))
            assertEquals(100, votes.sum())
            removed.forEach { assertEquals(0, votes[it]) }
            assertTrue(votes.all { it >= 0 })
        }
    }

    @Test
    fun audienceIsMoreReliableEarlyOn() {
        val q = Question("Q?", listOf("a", "b", "c", "d"), 0, "")
        fun avgCorrect(level: Int) = (0 until 500)
            .map { Lifelines.audienceVotes(q, emptySet(), level, emptyMap(), Random(it))[0] }
            .average()
        assertTrue(avgCorrect(0) > 65)
        assertTrue(avgCorrect(14) < 40)
    }

    @Test
    fun friendIsRightMoreOftenOnEasyQuestions() {
        val q = Question("Q?", listOf("a", "b", "c", "d"), 3, "")
        fun rate(level: Int) = (0 until 1000)
            .count { Lifelines.planFriend(q, emptySet(), level, Random(it)).leanIndex == 3 } / 1000.0
        assertTrue(rate(0) > 0.9)
        assertTrue(rate(14) < 0.7)
        val noIdea = (0 until 1000).map { Lifelines.planFriend(q, emptySet(), 14, Random(it)) }
            .filter { it.confidence == Confidence.NO_IDEA }
        assertTrue(noIdea.isNotEmpty())
        noIdea.forEach { assertNull(it.leanIndex) }
    }

    @Test
    fun audienceWeightsParseAndDefault() {
        val q = Question("Q?", listOf("a", "b", "c", "d"), 1, "")
        val w = Prompts.parseAudienceWeights("""{"A": 9, "C": "oops", "D": 42}""", q, emptySet())
        assertEquals(setOf(0, 2, 3), w.keys)
        assertEquals(9.0, w.getValue(0), 0.0)
        assertEquals(3.0, w.getValue(2), 0.0)  // unparseable -> default
        assertEquals(10.0, w.getValue(3), 0.0) // clamped
        assertEquals(3.0, Prompts.parseAudienceWeights("no json here", q, setOf(0, 2)).getValue(3), 0.0)
    }

    @Test
    fun lifelinePromptsMentionWhatTheyShould() {
        val q = Question("Capital of France?", listOf("Paris", "Lyon", "Nice", "Lille"), 0, "")
        val fp = Prompts.friendPrompt("Sam", "Geography", q, setOf(1, 2), FriendPlan(3, Confidence.GUESSING))
        assertTrue("D: Lille" in fp)
        assertFalse("B: Lyon" in fp)
        val ap = Prompts.audiencePrompt("Geography", q, setOf(1, 2))
        assertTrue("correct answer is A: Paris" in ap && "(D)" in ap)
    }

    @Test
    fun claudeResponseParsing() {
        val ok = """{"content":[{"type":"thinking","thinking":"x"},{"type":"text","text":"Hello "},{"type":"text","text":"there"}],"stop_reason":"end_turn"}"""
        assertEquals("Hello there", ClaudeClient.extractText(ok))
        try {
            ClaudeClient.extractText("""{"content":[],"stop_reason":"refusal"}""")
            fail("should throw")
        } catch (e: ClaudeException) {
            assertFalse(e.retryable)
        }
        val err = ClaudeClient.errorFor(400, """{"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}""")
        assertTrue(err.message!!.contains("credit balance"))
        assertTrue(ClaudeClient.errorFor(529, "").retryable)
        assertFalse(ClaudeClient.errorFor(401, "not json").retryable)
    }
}
