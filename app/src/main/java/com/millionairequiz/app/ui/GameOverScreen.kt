package com.millionairequiz.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.millionairequiz.app.game.EndReason
import com.millionairequiz.app.game.GameResult
import com.millionairequiz.app.game.GameViewModel
import com.millionairequiz.app.game.LETTERS
import com.millionairequiz.app.game.Ladder

@Composable
fun GameOverScreen(result: GameResult, topic: String, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        DiamondLogo(64.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            when (result.reason) {
                EndReason.WON -> "MILLIONAIRE!"
                EndReason.WALKED -> "You walked away"
                EndReason.WRONG -> if (result.amount > 0) "Saved by the safe haven" else "Game over"
            },
            color = Palette.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text("You're going home with", color = Palette.Silver)
        Text(
            Ladder.format(result.amount),
            color = Palette.Gold,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            when (result.reason) {
                EndReason.WON -> "All 15 questions on $topic. Remarkable."
                EndReason.WALKED -> "You answered ${result.level} of 15 correctly."
                EndReason.WRONG -> "You fell at question ${result.level + 1} of 15."
            },
            color = Palette.Silver,
            textAlign = TextAlign.Center,
        )

        val q = result.question
        if (q != null && result.reason != EndReason.WON) {
            Spacer(Modifier.height(24.dp))
            Lozenge(border = Palette.Gold.copy(alpha = 0.6f)) {
                Column {
                    Text("Question ${result.level + 1}", color = Palette.Gold, fontSize = 13.sp)
                    Text(q.text, color = Palette.White, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    if (result.reason == EndReason.WRONG && result.chosen != null) {
                        Text(
                            "You said ${LETTERS[result.chosen]}: ${q.options[result.chosen]}",
                            color = Palette.SoftRed,
                            fontSize = 14.sp,
                        )
                    }
                    Text("Answer: ${q.correctLabel}", color = Palette.SoftGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (q.explanation.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(q.explanation, color = Palette.Silver, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        GoldButton("Play again: $topic", Modifier.fillMaxWidth(), onClick = vm::startGame)
        Spacer(Modifier.height(12.dp))
        SilverButton("Choose a new subject", Modifier.fillMaxWidth(), onClick = vm::newTopic)
        Spacer(Modifier.height(24.dp))
    }
}
