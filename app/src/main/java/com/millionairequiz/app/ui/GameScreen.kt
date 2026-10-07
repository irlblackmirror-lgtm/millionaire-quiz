package com.millionairequiz.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.millionairequiz.app.game.GameViewModel
import com.millionairequiz.app.game.LETTERS
import com.millionairequiz.app.game.Ladder
import com.millionairequiz.app.game.Phase
import com.millionairequiz.app.game.Question
import com.millionairequiz.app.game.UiState

@Composable
fun GameScreen(s: UiState, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Header(s, vm)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LifelineButton(
                symbol = "50:50",
                caption = "50:50",
                used = s.usedFifty,
                clickable = !s.usedFifty && s.canAct,
                onClick = vm::useFiftyFifty,
            )
            LifelineButton(
                symbol = "📞", // telephone receiver
                caption = "Phone a Friend",
                used = s.usedPhone,
                clickable = if (s.usedPhone) s.phone != null else s.canAct,
                onClick = vm::usePhone,
            )
            LifelineButton(
                symbol = "👥", // two silhouettes
                caption = "Ask the Audience",
                used = s.usedAudience,
                clickable = if (s.usedAudience) s.audience != null else s.canAct,
                onClick = vm::useAudience,
            )
        }
        Spacer(Modifier.height(24.dp))

        val q = s.question
        when {
            s.loading -> Loading(
                "Writing question ${s.level + 1} for ${Ladder.format(Ladder.amounts[s.level])}…"
            )
            s.error != null -> ErrorBlock(s.error, vm)
            q != null -> QuestionBlock(s, q, vm)
        }
    }
}

@Composable
private fun Header(s: UiState, vm: GameViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Question ${s.level + 1} of 15", color = Palette.Silver, fontSize = 13.sp)
            Text(
                "for ${Ladder.format(Ladder.amounts[s.level])}",
                color = Palette.Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                s.topic,
                color = Palette.Silver.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Pill("Ladder") { vm.setLadderVisible(true) }
        Spacer(Modifier.width(8.dp))
        Pill("Walk away", enabled = s.phase == Phase.CHOOSING || s.phase == Phase.CONFIRMING) {
            vm.requestWalkAway()
        }
    }
}

@Composable
private fun Loading(message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Palette.Gold)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Palette.Silver, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorBlock(error: String, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Couldn't get the next question", color = Palette.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(error, color = Palette.SoftRed, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        GoldButton("Try again", Modifier.fillMaxWidth(), onClick = vm::retryQuestion)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = vm::openSettings) { Text("Settings", color = Palette.Silver) }
    }
}

@Composable
private fun QuestionBlock(s: UiState, q: Question, vm: GameViewModel) {
    Lozenge(border = Palette.Gold, minHeight = 100.dp) {
        Text(
            q.text,
            color = Palette.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(20.dp))
    q.options.forEachIndexed { i, option ->
        AnswerButton(
            letter = LETTERS[i],
            text = option,
            visual = visualFor(s, q, i),
            enabled = s.canAct,
            onClick = { vm.selectAnswer(i) },
        )
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(10.dp))
    ActionArea(s, q, vm)
}

private fun visualFor(s: UiState, q: Question, i: Int): AnswerVisual = when {
    i in s.removed -> AnswerVisual.REMOVED
    s.phase == Phase.REVEALED && i == q.correctIndex -> AnswerVisual.CORRECT
    s.phase == Phase.REVEALED && i == s.selected -> AnswerVisual.WRONG
    i == s.selected -> AnswerVisual.SELECTED
    else -> AnswerVisual.NORMAL
}

@Composable
private fun ActionArea(s: UiState, q: Question, vm: GameViewModel) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (s.phase) {
            Phase.CHOOSING -> Text(
                "Tap an answer. Stuck? Use a lifeline.",
                color = Palette.Silver,
                textAlign = TextAlign.Center,
            )

            Phase.CONFIRMING -> {
                Text(
                    "Is that your final answer?",
                    color = Palette.Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoldButton("Final answer", Modifier.weight(1f), onClick = vm::finalAnswer)
                    SilverButton("Not yet", Modifier.weight(1f), onClick = vm::cancelAnswer)
                }
            }

            Phase.LOCKED -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Palette.Orange, modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Text("Locked in…", color = Palette.Orange, fontSize = 18.sp)
            }

            Phase.REVEALED -> Revealed(s, q, vm)
        }
    }
}

@Composable
private fun Revealed(s: UiState, q: Question, vm: GameViewModel) {
    val correct = s.selected == q.correctIndex
    val isLast = s.level == Ladder.LAST_LEVEL
    if (correct) {
        Text(
            if (isLast) "YOU'VE WON ONE MILLION POUNDS!" else "Correct! You've got ${Ladder.format(Ladder.amounts[s.level])}",
            color = Palette.SoftGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (s.level in Ladder.safeHavens) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Safe haven reached: you can't leave with less than ${Ladder.format(Ladder.amounts[s.level])} now.",
                color = Palette.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Text(
            "Oh no. The answer was ${q.correctLabel}",
            color = Palette.SoftRed,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "You leave with ${Ladder.format(Ladder.guaranteedAfterWrong(s.level))}.",
            color = Palette.White,
            textAlign = TextAlign.Center,
        )
    }
    if (q.explanation.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            q.explanation,
            color = Palette.Silver,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(16.dp))
    GoldButton(
        text = when {
            !correct -> "See how you did"
            isLast -> "Collect your million"
            else -> "Next question: ${Ladder.format(Ladder.amounts[s.level + 1])}"
        },
        modifier = Modifier.fillMaxWidth(),
        onClick = vm::continueAfterReveal,
    )
}
