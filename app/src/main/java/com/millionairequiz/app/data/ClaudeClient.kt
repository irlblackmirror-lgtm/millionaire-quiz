package com.millionairequiz.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/** A failure the player should see. [retryable] errors are retried automatically first. */
class ClaudeException(message: String, val retryable: Boolean = false) : Exception(message)

/**
 * Minimal client for the Claude Messages API, using only what ships with Android
 * (HttpURLConnection + org.json), so there are no networking dependencies to manage.
 *
 * Calls are blocking: always run them off the main thread.
 */
class ClaudeClient(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    fun complete(system: String, user: String, maxTokens: Int = 800): String {
        var attempt = 0
        while (true) {
            try {
                return completeOnce(system, user, maxTokens)
            } catch (e: ClaudeException) {
                attempt++
                if (!e.retryable || attempt >= MAX_ATTEMPTS) throw e
                Thread.sleep(1500L * attempt)
            }
        }
    }

    private fun completeOnce(system: String, user: String, maxTokens: Int): String {
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", maxTokens)
            .put("system", system)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", user))
            )

        var conn: HttpURLConnection? = null
        try {
            conn = URL("$baseUrl/v1/messages").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 90_000
            conn.doOutput = true
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", API_VERSION)
            conn.setRequestProperty("content-type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw errorFor(code, text)
            return extractText(text)
        } catch (e: SocketTimeoutException) {
            throw ClaudeException("The request timed out.", retryable = true)
        } catch (e: IOException) {
            throw ClaudeException(
                "Network problem: ${e.message ?: "couldn't reach the Claude API"}",
                retryable = true
            )
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"
        const val API_VERSION = "2023-06-01"
        private const val MAX_ATTEMPTS = 3

        /** Joins the text blocks of a Messages API response. */
        fun extractText(responseJson: String): String {
            val obj = try {
                JSONObject(responseJson)
            } catch (e: Exception) {
                throw ClaudeException("Unreadable response from the API.", retryable = true)
            }
            val content = obj.optJSONArray("content")
                ?: throw ClaudeException("Unexpected response from the API.", retryable = true)
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                if (block.optString("type") == "text") sb.append(block.optString("text"))
            }
            if (sb.isBlank()) {
                if (obj.optString("stop_reason") == "refusal") {
                    throw ClaudeException("The model declined to write that one. Try a different subject.")
                }
                throw ClaudeException("The API returned an empty reply.", retryable = true)
            }
            return sb.toString()
        }

        fun errorFor(code: Int, body: String): ClaudeException {
            val apiMessage = runCatching {
                JSONObject(body).getJSONObject("error").optString("message")
            }.getOrNull().orEmpty()
            val detail = if (apiMessage.isNotBlank()) " ($apiMessage)" else ""
            return when (code) {
                401 -> ClaudeException("Your API key was rejected. Check it in Settings.$detail")
                403 -> ClaudeException("This API key isn't allowed to do that.$detail")
                404 -> ClaudeException("Model not found — choose another model in Settings.$detail")
                400 -> ClaudeException("The API rejected the request.$detail")
                429 -> ClaudeException("Rate limited by the API.$detail", retryable = true)
                500, 502, 503, 504, 529 ->
                    ClaudeException("Claude is busy right now (error $code). Try again.", retryable = true)
                else -> ClaudeException("API error $code.$detail")
            }
        }
    }
}
