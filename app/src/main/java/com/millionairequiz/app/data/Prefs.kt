package com.millionairequiz.app.data

import android.content.Context

data class ModelOption(val id: String, val label: String)

val MODEL_OPTIONS = listOf(
    ModelOption("claude-sonnet-5-5", "Claude Sonnet 5.5: balanced (recommended)"),
    ModelOption("claude-haiku-4-5-20251001", "Claude Haiku 4.5: fastest and cheapest"),
    ModelOption("claude-opus-5-5", "Claude Opus 5.5: best questions, slower"),
)

const val DEFAULT_MODEL = "claude-sonnet-5-5"

/** Used for questions 1-5 and lifeline calls when "save money" is on. */
const val CHEAP_MODEL = "claude-haiku-4-5-20251001"

/**
 * Plain SharedPreferences, private to this app. Fine for a personal project; for a
 * published app, proxy calls through your own server rather than shipping user keys around.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "").orEmpty()
        set(value) = sp.edit().putString("api_key", value).apply()

    var model: String
        get() = sp.getString("model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(value) = sp.edit().putString("model", value).apply()

    var cheapEasy: Boolean
        get() = sp.getBoolean("cheap_easy", true)
        set(value) = sp.edit().putBoolean("cheap_easy", value).apply()

    var friendName: String
        get() = sp.getString("friend_name", "Sam").orEmpty().ifBlank { "Sam" }
        set(value) = sp.edit().putString("friend_name", value).apply()

    var lastTopic: String
        get() = sp.getString("last_topic", "").orEmpty()
        set(value) = sp.edit().putString("last_topic", value).apply()
}
