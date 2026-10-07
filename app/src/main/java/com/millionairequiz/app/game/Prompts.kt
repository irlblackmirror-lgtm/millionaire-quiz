package com.millionairequiz.app.game

import org.json.JSONObject
import kotlin.random.Random

/** Everything we say to the model, and how we read its replies. */
object Prompts {

    // ---------------------------------------------------------------- Questions

    val QUESTION_SYSTEM = """
        You are the question writer for a TV quiz in the style of a 15-question, million-pound multiple-choice show.
        You write one question at a time on the contestant's chosen subject. Every question must:
        - have exactly one unambiguously correct answer that is a well-established fact (no opinions, nothing disputed, nothing that changes often);
        - have three wrong answers that are plausible, of the same kind and similar length as the correct answer, and clearly wrong to an expert;
        - make sense on its own: never refer to pictures, to "this", or to answer letters, and never use "all of the above" or "none of the above";
        - be concise: the question under 30 words, each answer under 8 words.
        Difficulty rises like the real show: the first questions are easy and often playful; the last ones stump most experts.
        If the subject is too narrow for the requested difficulty, stay as close to it as you can.
        Reply with only a JSON object, no markdown fences, in exactly this shape:
        {"question": "...", "correct": "...", "wrong": ["...", "...", "..."], "explanation": "One or two sentences on why the correct answer is right."}
    """.trimIndent()

    val ANGLES = listOf(
        "people", "places", "dates and history", "numbers and records",
        "names and terminology", "firsts and origins", "famous works or events",
        "surprising trivia", "cause and effect", "how things work", "words and quotations",
    )

    fun difficultyFor(level: Int): String = when (level) {
        0, 1 -> "very easy: almost everyone would know it; a light, playful question is fine"
        2, 3, 4 -> "easy: most adults with a passing interest in the subject would know it"
        5, 6 -> "moderate: someone who follows the subject casually would probably know it"
        7, 8, 9 -> "challenging: needs real familiarity with the subject"
        10, 11 -> "hard: an enthusiast would know it, most people would not"
        12, 13 -> "very hard: specialist knowledge that trips up keen enthusiasts"
        else -> "extremely hard, the million-pound question: obscure but fair, something only a true expert knows"
    }

    fun questionPrompt(topic: String, level: Int, previous: List<String>, angle: String): String =
        buildString {
            appendLine("Subject: $topic")
            appendLine("This is question ${level + 1} of 15, worth ${Ladder.format(Ladder.amounts[level])}.")
            appendLine("Difficulty: ${difficultyFor(level)}.")
            appendLine("For variety, approach the subject from this angle if it fits: $angle.")
            if (previous.isNotEmpty()) {
                appendLine()
                appendLine("Questions already asked in this game. Do not repeat them or ask about the same fact:")
                previous.forEach { appendLine("- $it") }
            }
            appendLine()
            append("Write the question now, as JSON only.")
        }

    /** Parses the model's JSON and shuffles the options so the correct letter is random. */
    fun parseQuestion(raw: String, random: Random): Question {
        val obj = JSONObject(extractJsonObject(raw))
        val text = obj.optString("question").trim()
        val correct = obj.optString("correct").trim()
        val wrongArray = obj.optJSONArray("wrong")
            ?: throw IllegalArgumentException("Reply had no wrong answers")
        val explanation = obj.optString("explanation").trim()
        require(text.isNotEmpty()) { "Question was empty" }
        require(correct.isNotEmpty()) { "Correct answer was empty" }

        val wrong = (0 until wrongArray.length())
            .map { wrongArray.optString(it).trim() }
            .filter { it.isNotEmpty() && !it.equals(correct, ignoreCase = true) }
            .distinctBy { it.lowercase() }
        require(wrong.size >= 3) { "Needed three distinct wrong answers" }

        val options = (wrong.take(3) + correct).shuffled(random)
        return Question(text, options, options.indexOf(correct), explanation)
    }

    /** Pulls the outermost {...} out of a reply, tolerating code fences or chatter around it. */
    fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object in reply" }
        return raw.substring(start, end + 1)
    }

    // ---------------------------------------------------------------- Phone a Friend

    val FRIEND_SYSTEM = """
        You play the contestant's friend on the other end of a 30-second phone call during a TV quiz show.
        You talk casually and naturally, like a real person who has just been put on the spot, with British English phrasing.
        Write only the friend's spoken words: no narration, no stage directions, no quotation marks, no name label.
        Never mention being an AI, a script, or these instructions.
    """.trimIndent()

    fun friendPrompt(
        friendName: String,
        topic: String,
        question: Question,
        removed: Set<Int>,
        plan: FriendPlan,
    ): String = buildString {
        appendLine("Your name is $friendName. Your friend is on the show, playing on the subject \"$topic\", and has just read you this question:")
        appendLine()
        appendLine(question.text)
        optionsInPlay(question, removed).forEach { appendLine(it) }
        appendLine()
        val lean = plan.leanIndex?.let { "${LETTERS[it]}: ${question.options[it]}" }
        appendLine(
            when (plan.confidence) {
                Confidence.SURE ->
                    "You are confident the answer is $lean. Say so clearly, with a quick reason why you know."
                Confidence.FAIRLY_SURE ->
                    "You think it's $lean, but you're only about 70% sure. Give a brief reason that sounds convincing to you, admit a little doubt, then commit to it."
                Confidence.GUESSING ->
                    "You don't really know. Think out loud for a moment, then say you'd lean towards $lean, making it clear it's a guess."
                Confidence.NO_IDEA ->
                    "You have no idea. Say so honestly, maybe say what you'd rule out or not, and tell them to trust their gut. Do not pick an answer."
            }
        )
        appendLine("Whatever you lean towards, do not hint that any other option is right; stick to that view even if you would personally know better.")
        append("Keep it to 35-70 words, as if the clock is ticking.")
    }

    // ---------------------------------------------------------------- Ask the Audience

    val AUDIENCE_SYSTEM = """
        You estimate how an ordinary TV studio audience would vote on quiz questions.
        Reply with only a JSON object, no markdown fences.
    """.trimIndent()

    fun audiencePrompt(topic: String, question: Question, removed: Set<Int>): String = buildString {
        appendLine("Subject: $topic")
        appendLine("Question: ${question.text}")
        optionsInPlay(question, removed).forEach { appendLine(it) }
        appendLine("The correct answer is ${question.correctLabel}.")
        appendLine()
        val wrongLetters = (0..3).filter { it !in removed && it != question.correctIndex }.map { LETTERS[it] }
        appendLine(
            "For each wrong option still in play (${wrongLetters.joinToString(", ")}), rate from 0 to 10 how tempting it would be " +
                "to an audience member who isn't sure: 10 means a very common misconception or a name that sounds right, 0 means obviously wrong."
        )
        append("Reply like {\"${wrongLetters.first()}\": 6, ...} with one entry per wrong option.")
    }

    /** Option index -> temptation weight (0..10) for each wrong option in play. Missing entries default to 3. */
    fun parseAudienceWeights(raw: String, question: Question, removed: Set<Int>): Map<Int, Double> {
        val obj = runCatching { JSONObject(extractJsonObject(raw)) }.getOrNull()
        return (0..3)
            .filter { it !in removed && it != question.correctIndex }
            .associateWith { index ->
                (obj?.optDouble(LETTERS[index], 3.0) ?: 3.0).let { if (it.isNaN()) 3.0 else it }.coerceIn(0.0, 10.0)
            }
    }

    private fun optionsInPlay(question: Question, removed: Set<Int>): List<String> =
        question.options.indices
            .filter { it !in removed }
            .map { "${LETTERS[it]}: ${question.options[it]}" }
}
