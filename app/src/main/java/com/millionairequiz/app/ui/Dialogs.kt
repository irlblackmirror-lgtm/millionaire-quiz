package com.millionairequiz.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.millionairequiz.app.data.MODEL_OPTIONS
import com.millionairequiz.app.game.AudienceState
import com.millionairequiz.app.game.LETTERS
import com.millionairequiz.app.game.Ladder
import com.millionairequiz.app.game.PhoneState

@Composable
fun PhoneDialog(phone: PhoneState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!phone.loading) onDismiss() },
        containerColor = Palette.DialogBg,
        title = { Text("📞  ${phone.friendName}", color = Palette.Gold) },
        text = {
            if (phone.loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Palette.Gold, modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Ringing… you've got 30 seconds, starting now.", color = Palette.Silver)
                }
            } else {
                Text(
                    "“${phone.text}”",
                    color = Palette.White,
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                )
            }
        },
        confirmButton = {
            if (!phone.loading) {
                TextButton(onClick = onDismiss) { Text("Thanks, ${phone.friendName}", color = Palette.Gold) }
            }
        },
    )
}

@Composable
fun AudienceDialog(audience: AudienceState, removed: Set<Int>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!audience.loading) onDismiss() },
        containerColor = Palette.DialogBg,
        title = { Text("Ask the Audience", color = Palette.Gold) },
        text = {
            Column {
                Text(
                    if (audience.loading) "The audience is voting…" else "Here's how the studio audience voted:",
                    color = Palette.Silver,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (i in 0..3) {
                        val pct = audience.votes?.get(i) ?: 0
                        val fraction by animateFloatAsState(
                            targetValue = pct / 100f,
                            animationSpec = tween(durationMillis = 1100),
                            label = "bar$i",
                        )
                        Column(
                            Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                if (audience.loading || i in removed) "" else "$pct%",
                                color = Palette.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .width(34.dp)
                                    .height((150 * fraction).dp + 2.dp)
                                    .background(if (i in removed) Palette.Silver.copy(alpha = 0.2f) else Palette.Gold)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(LETTERS[i], color = Palette.Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!audience.loading) {
                TextButton(onClick = onDismiss) { Text("Thank you, audience", color = Palette.Gold) }
            }
        },
    )
}

@Composable
fun LadderDialog(level: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .clip(shape)
                .background(Palette.DialogBg)
                .border(1.5.dp, Palette.Gold.copy(alpha = 0.6f), shape)
                .padding(16.dp),
        ) {
            for (i in Ladder.LAST_LEVEL downTo 0) {
                val current = i == level
                val won = i < level
                val haven = i in Ladder.safeHavens
                val color = when {
                    current -> Palette.Navy
                    haven -> Palette.White
                    won -> Palette.Gold
                    else -> Palette.Silver.copy(alpha = 0.75f)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (current) Palette.Orange else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("${i + 1}", color = color, modifier = Modifier.width(36.dp), fontSize = 15.sp)
                    Text(if (won) "♦" else "", color = color, modifier = Modifier.width(20.dp))
                    Text(
                        Ladder.format(Ladder.amounts[i]),
                        color = color,
                        fontSize = 15.sp,
                        fontWeight = if (haven || current) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "White amounts are safe havens.",
                color = Palette.Silver,
                fontSize = 12.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close", color = Palette.Gold) }
            }
        }
    }
}

@Composable
fun WalkAwayDialog(level: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val amount = Ladder.format(Ladder.walkAwayAmount(level))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.DialogBg,
        title = { Text("Walk away?", color = Palette.Gold) },
        text = {
            Text(
                "You can leave now with $amount, or play on for ${Ladder.format(Ladder.amounts[level])}. " +
                    "Get it wrong and you'll drop to ${Ladder.format(Ladder.guaranteedAfterWrong(level))}.",
                color = Palette.White,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Take the $amount", color = Palette.Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep playing", color = Palette.Silver) } },
    )
}

@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.DialogBg,
        title = { Text(title, color = Palette.Gold) },
        text = { Text(message, color = Palette.White) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = Palette.Gold) } },
    )
}

@Composable
fun SettingsDialog(
    currentKey: String,
    currentModel: String,
    currentCheapEasy: Boolean,
    onSave: (apiKey: String, model: String, cheapEasy: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf(currentKey) }
    var model by remember { mutableStateOf(currentModel) }
    var showKey by remember { mutableStateOf(false) }
    var cheapEasy by remember { mutableStateOf(currentCheapEasy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.DialogBg,
        title = { Text("Settings", color = Palette.Gold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("Claude API key") },
                    placeholder = { Text("sk-ant-…") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show", color = Palette.Silver, fontSize = 12.sp)
                        }
                    },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Create a key in the Claude Console. It's stored only on this device, and each game costs a few pence in API usage.",
                    color = Palette.Silver,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text("Model", color = Palette.White, fontWeight = FontWeight.Bold)
                MODEL_OPTIONS.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = model == option.id,
                                onClick = { model = option.id },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = model == option.id,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Palette.Gold,
                                unselectedColor = Palette.Silver,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.label, color = Palette.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.trim() },
                    label = { Text("Model ID (or type any other)") },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = cheapEasy,
                            onValueChange = { cheapEasy = it },
                            role = Role.Switch,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Save money", color = Palette.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Use Claude Haiku 4.5 for questions 1-5 and the lifelines. Questions 6-15 still use the model above.",
                            color = Palette.Silver,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = cheapEasy,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Palette.Navy,
                            checkedTrackColor = Palette.Gold,
                            uncheckedThumbColor = Palette.Silver,
                            uncheckedTrackColor = Palette.Panel,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key, model, cheapEasy) }, enabled = key.isNotBlank()) {
                Text("Save", color = if (key.isNotBlank()) Palette.Gold else Palette.Silver.copy(alpha = 0.4f))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Silver) } },
    )
}
