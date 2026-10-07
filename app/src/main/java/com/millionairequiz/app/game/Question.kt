package com.millionairequiz.app.game

val LETTERS = listOf("A", "B", "C", "D")

data class Question(
    val text: String,
    /** Always four options, already shuffled. */
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
) {
    val correctLabel: String get() = "${LETTERS[correctIndex]}: ${options[correctIndex]}"
}
