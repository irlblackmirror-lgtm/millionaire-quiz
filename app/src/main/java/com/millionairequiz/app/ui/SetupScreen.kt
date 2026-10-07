package com.millionairequiz.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.millionairequiz.app.game.GameViewModel
import com.millionairequiz.app.game.UiState

private val SUGGESTIONS = listOf(
    "Ancient Rome", "Premier League football", "The solar system", "British TV comedy",
    "World geography", "Classic films", "Dinosaurs", "80s pop music", "Cooking", "Mythology",
)

@Composable
fun SetupScreen(s: UiState, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = vm::openSettings) { Text("Settings", color = Palette.Silver) }
        }
        Spacer(Modifier.height(8.dp))
        DiamondLogo()
        Spacer(Modifier.height(16.dp))
        Text("MILLIONAIRE", color = Palette.Gold, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
        Text("Q U I Z", color = Palette.Silver, fontSize = 15.sp, letterSpacing = 6.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "15 questions on any subject you like, written live by Claude. Three lifelines. One million pounds.",
            color = Palette.Silver,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = s.topic,
            onValueChange = vm::setTopic,
            label = { Text("Your subject") },
            placeholder = { Text("e.g. Roman history, Formula 1, Jane Austen") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { vm.startGame() }),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SUGGESTIONS.forEach { Pill(it) { vm.setTopic(it) } }
        }
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = s.friendName,
            onValueChange = vm::setFriendName,
            label = { Text("Who's your Phone-a-Friend?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(28.dp))

        GoldButton(
            text = "Let's play",
            enabled = s.topic.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = vm::startGame,
        )
        if (s.apiKey.isBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "You'll need a Claude API key first. Tap Settings (or Let's play) to add one.",
                color = Palette.Orange,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
