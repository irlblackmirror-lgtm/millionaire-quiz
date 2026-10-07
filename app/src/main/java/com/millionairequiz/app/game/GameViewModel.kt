package com.millionairequiz.app.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.millionairequiz.app.data.ClaudeClient
import com.millionairequiz.app.data.ClaudeException
import com.millionairequiz.app.data.DEFAULT_MODEL
import com.millionairequiz.app.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class Screen { SETUP, GAME, GAME_OVER }

/** Choosing -> Confirming ("Is that your final answer?") -> Locked (suspense) -> Revealed. */
enum class Phase { CHOOSING, CONFIRMING, LOCKED, REVEALED }

enum class EndReason { WON, WRONG, WALKED }

data class PhoneState(val friendName: String, val text: String? = null) {
    val loading: Boolean get() = text == null
}

data class AudienceState(val votes: List<Int>? = null) {
    val loading: Boolean get() = votes == null
}

data class GameResult(
    val reason: EndReason,
    val amount: Int,
    val question: Question?,
    val chosen: Int?,
    val level: Int,
)

data class UiState(
    val screen: Screen = Screen.SETUP,
    // Setup + settings
    val topic: String = "",
    val friendName: String = "Sam",
    val apiKey: String = "",
    val model: String = "",
    val showSettings: Boolean = false,
    // Current question
    val level: Int = 0,
    val question: Question? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val phase: Phase = Phase.CHOOSING,
    val selected: Int? = null,
    val removed: Set<Int> = emptySet(),
    // Lifelines
    val usedFifty: Boolean = false,
    val usedPhone: Boolean = false,
    val usedAudience: Boolean = false,
    val phone: PhoneState? = null,
    val showPhone: Boolean = false,
    val audience: AudienceState? = null,
    val showAudience: Boolean = false,
    val lifelineError: String? = null,
    // Overlays
    val showLadder: Boolean = false,
    val confirmWalkAway: Boolean = false,
    // End
    val result: GameResult? = null,
) {
    val canAct: Boolean
        get() = screen == Screen.GAME && question != null && !loading &&
            (phase == Phase.CHOOSING || phase == Phase.CONFIRMING)
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val random = Random.Default

    private val _state = MutableStateFlow(
        UiState(
            topic = prefs.lastTopic,
            friendName = prefs.friendName,
            apiKey = prefs.apiKey,
            model = prefs.model,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Texts of questions already shown this game, so the model doesn't repeat itself. Main thread only. */
    private val askedQuestions = mutableListOf<String>()
    private var prefetch: Deferred<Question>? = null
    private var prefetchLevel = -1
    private var questionJob: Job? = null
    private var revealJob: Job? = null

    // ------------------------------------------------------------------ Setup & settings

    fun setTopic(value: String) = _state.update { it.copy(topic = value) }
    fun setFriendName(value: String) = _state.update { it.copy(friendName = value) }
    fun openSettings() = _state.update { it.copy(showSettings = true) }
    fun closeSettings() = _state.update { it.copy(showSettings = false) }

    fun saveSettings(apiKey: String, model: String) {
        val key = apiKey.trim()
        val m = model.trim().ifEmpty { DEFAULT_MODEL }
        prefs.apiKey = key
        prefs.model = m
        _state.update { it.copy(apiKey = key, model = m, showSettings = false) }
    }

    fun startGame() {
        val s = _state.value
        val topic = s.topic.trim()
        if (topic.isEmpty()) return
        if (s.apiKey.isBlank()) {
            openSettings()
            return
        }
        val friend = s.friendName.trim().ifEmpty { "Sam" }
        prefs.lastTopic = topic
        prefs.friendName = friend

        cancelBackgroundWork()
        askedQuestions.clear()
        _state.value = UiState(
            screen = Screen.GAME,
            topic = topic,
            friendName = friend,
            apiKey = s.apiKey,
            model = s.model,
        )
        loadQuestion(0)
    }

    fun newTopic() {
        cancelBackgroundWork()
        _state.update { it.copy(screen = Screen.SETUP, result = null) }
    }

    // ------------------------------------------------------------------ Questions

    /** Overridable for tests against a local fake server. */
    internal var apiBaseUrl = ClaudeClient.DEFAULT_BASE_URL

    private fun client() = ClaudeClient(_state.value.apiKey, _state.value.model, apiBaseUrl)

    /** Blocking; run on Dispatchers.IO. Re-asks if the model's JSON is unusable. */
    private fun generateQuestion(client: ClaudeClient, topic: String, level: Int, previous: List<String>): Question {
        var lastProblem: String? = null
        repeat(3) {
            val raw = client.complete(
                Prompts.QUESTION_SYSTEM,
                Prompts.questionPrompt(topic, level, previous, Prompts.ANGLES.random(random)),
                maxTokens = 700,
            )
            try {
                return Prompts.parseQuestion(raw, random)
            } catch (e: Exception) {
                lastProblem = e.message
            }
        }
        throw ClaudeException("Couldn't get a usable question from the model ($lastProblem). Try again.")
    }

    private fun startPrefetch(level: Int) {
        if (level > Ladder.LAST_LEVEL) return
        val client = client()
        val topic = _state.value.topic
        val previous = askedQuestions.toList()
        prefetchLevel = level
        prefetch = viewModelScope.async(Dispatchers.IO) { generateQuestion(client, topic, level, previous) }
    }

    private fun loadQuestion(level: Int) {
        questionJob?.cancel()
        _state.update {
            it.copy(
                level = level, question = null, loading = true, error = null,
                phase = Phase.CHOOSING, selected = null, removed = emptySet(),
                phone = null, showPhone = false, audience = null, showAudience = false,
            )
        }
        val pending = prefetch.takeIf { prefetchLevel == level }
        if (pending == null) prefetch?.cancel()
        prefetch = null
        val client = client()
        val topic = _state.value.topic
        val previous = askedQuestions.toList()
        questionJob = viewModelScope.launch {
            try {
                val prefetched = pending?.let { runCatching { it.await() }.getOrNull() }
                ensureActive()
                val q = prefetched ?: withContext(Dispatchers.IO) {
                    generateQuestion(client, topic, level, previous)
                }
                askedQuestions += q.text
                _state.update { it.copy(question = q, loading = false) }
                startPrefetch(level + 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Something went wrong.") }
            }
        }
    }

    fun retryQuestion() = loadQuestion(_state.value.level)

    // ------------------------------------------------------------------ Answering

    fun selectAnswer(index: Int) {
        val s = _state.value
        if (!s.canAct || index in s.removed) return
        _state.update { it.copy(selected = index, phase = Phase.CONFIRMING) }
    }

    fun cancelAnswer() {
        if (_state.value.phase != Phase.CONFIRMING) return
        _state.update { it.copy(selected = null, phase = Phase.CHOOSING) }
    }

    fun finalAnswer() {
        val s = _state.value
        if (s.phase != Phase.CONFIRMING || s.selected == null) return
        _state.update { it.copy(phase = Phase.LOCKED) }
        revealJob = viewModelScope.launch {
            delay(1200L + s.level * 130L) // longer suspense the higher you go
            _state.update { it.copy(phase = Phase.REVEALED) }
        }
    }

    fun continueAfterReveal() {
        val s = _state.value
        val q = s.question ?: return
        if (s.phase != Phase.REVEALED) return
        when {
            s.selected != q.correctIndex -> endGame(EndReason.WRONG, Ladder.guaranteedAfterWrong(s.level))
            s.level == Ladder.LAST_LEVEL -> endGame(EndReason.WON, Ladder.amounts[Ladder.LAST_LEVEL])
            else -> loadQuestion(s.level + 1)
        }
    }

    fun requestWalkAway() {
        val s = _state.value
        if (s.screen != Screen.GAME || s.phase == Phase.LOCKED || s.phase == Phase.REVEALED) return
        _state.update { it.copy(confirmWalkAway = true) }
    }

    fun dismissWalkAway() = _state.update { it.copy(confirmWalkAway = false) }

    fun walkAway() {
        val s = _state.value
        endGame(EndReason.WALKED, Ladder.walkAwayAmount(s.level))
    }

    private fun endGame(reason: EndReason, amount: Int) {
        cancelBackgroundWork()
        _state.update {
            it.copy(
                screen = Screen.GAME_OVER,
                confirmWalkAway = false,
                showPhone = false,
                showAudience = false,
                showLadder = false,
                result = GameResult(reason, amount, it.question, it.selected, it.level),
            )
        }
    }

    private fun cancelBackgroundWork() {
        questionJob?.cancel()
        revealJob?.cancel()
        prefetch?.cancel()
        prefetch = null
        prefetchLevel = -1
    }

    // ------------------------------------------------------------------ Lifelines

    fun useFiftyFifty() {
        val s = _state.value
        val q = s.question ?: return
        if (s.usedFifty || !s.canAct) return
        val removed = Lifelines.fiftyFifty(q, random)
        val keepSelection = s.selected != null && s.selected !in removed
        _state.update {
            it.copy(
                usedFifty = true,
                removed = removed,
                selected = if (keepSelection) it.selected else null,
                phase = if (keepSelection) it.phase else Phase.CHOOSING,
            )
        }
    }

    fun usePhone() {
        val s = _state.value
        val q = s.question ?: return
        if (s.usedPhone) {
            if (s.phone != null) _state.update { it.copy(showPhone = true) }
            return
        }
        if (!s.canAct) return
        val plan = Lifelines.planFriend(q, s.removed, s.level, random)
        val prompt = Prompts.friendPrompt(s.friendName, s.topic, q, s.removed, plan)
        val client = client()
        _state.update { it.copy(usedPhone = true, phone = PhoneState(s.friendName), showPhone = true) }
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    client.complete(Prompts.FRIEND_SYSTEM, prompt, maxTokens = 300)
                }.trim().trim('"')
                _state.update { cur ->
                    if (cur.question != q) cur else cur.copy(phone = PhoneState(s.friendName, text))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { cur ->
                    if (cur.question != q) cur else cur.copy(
                        usedPhone = false, phone = null, showPhone = false,
                        lifelineError = "The line went dead: ${e.message}\n\nDon't worry, you still have Phone a Friend.",
                    )
                }
            }
        }
    }

    fun useAudience() {
        val s = _state.value
        val q = s.question ?: return
        if (s.usedAudience) {
            if (s.audience != null) _state.update { it.copy(showAudience = true) }
            return
        }
        if (!s.canAct) return
        val removed = s.removed
        val prompt = Prompts.audiencePrompt(s.topic, q, removed)
        val client = client()
        _state.update { it.copy(usedAudience = true, audience = AudienceState(), showAudience = true) }
        viewModelScope.launch {
            // If the model can't be reached, the audience still votes, just with flat guesses.
            val weights: Map<Int, Double> = try {
                withContext(Dispatchers.IO) {
                    Prompts.parseAudienceWeights(
                        client.complete(Prompts.AUDIENCE_SYSTEM, prompt, maxTokens = 150), q, removed
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyMap()
            }
            val votes = Lifelines.audienceVotes(q, removed, s.level, weights, random)
            _state.update { cur ->
                if (cur.question != q) cur else cur.copy(audience = AudienceState(votes))
            }
        }
    }

    fun dismissPhone() = _state.update { it.copy(showPhone = false) }
    fun dismissAudience() = _state.update { it.copy(showAudience = false) }
    fun dismissLifelineError() = _state.update { it.copy(lifelineError = null) }
    fun setLadderVisible(visible: Boolean) = _state.update { it.copy(showLadder = visible) }
}
