package com.millionairequiz.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The show's signature shape: a panel with clipped corners and a metallic edge. */
@Composable
fun Lozenge(
    modifier: Modifier = Modifier,
    fill: Color = Palette.Panel,
    border: Color = Palette.Silver,
    minHeight: Dp = 52.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = CutCornerShape(18.dp)
    var m = modifier
        .fillMaxWidth()
        .heightIn(min = minHeight)
        .clip(shape)
        .background(fill)
        .border(2.dp, border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(horizontal = 24.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
        content()
    }
}

enum class AnswerVisual { NORMAL, SELECTED, CORRECT, WRONG, REMOVED }

@Composable
fun AnswerButton(
    letter: String,
    text: String,
    visual: AnswerVisual,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val fill by animateColorAsState(
        when (visual) {
            AnswerVisual.NORMAL -> Palette.Panel
            AnswerVisual.SELECTED -> Palette.Orange
            AnswerVisual.CORRECT -> Palette.Green
            AnswerVisual.WRONG -> Palette.Red
            AnswerVisual.REMOVED -> Palette.Panel.copy(alpha = 0.35f)
        },
        animationSpec = tween(350),
        label = "answerFill",
    )
    val border = when (visual) {
        AnswerVisual.NORMAL -> Palette.Silver.copy(alpha = 0.8f)
        AnswerVisual.SELECTED -> Color(0xFFFFD08A)
        AnswerVisual.CORRECT -> Palette.SoftGreen
        AnswerVisual.WRONG -> Palette.SoftRed
        AnswerVisual.REMOVED -> Palette.Silver.copy(alpha = 0.25f)
    }
    val letterColor = when (visual) {
        AnswerVisual.NORMAL -> Palette.Gold
        AnswerVisual.SELECTED -> Palette.Navy
        else -> Palette.White
    }
    val textColor = if (visual == AnswerVisual.SELECTED) Palette.Navy else Palette.White

    Lozenge(
        fill = fill,
        border = border,
        onClick = if (enabled && visual != AnswerVisual.REMOVED) onClick else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$letter:",
                color = letterColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.width(30.dp),
            )
            Text(
                text = if (visual == AnswerVisual.REMOVED) "" else text,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = if (visual == AnswerVisual.NORMAL) FontWeight.Normal else FontWeight.SemiBold,
            )
        }
    }
}

/** An oval lifeline badge; a red cross marks it as spent. */
@Composable
fun LifelineButton(
    symbol: String,
    caption: String,
    used: Boolean,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var m = Modifier
            .size(width = 74.dp, height = 46.dp)
            .clip(shape)
            .background(Palette.Panel)
            .border(2.dp, if (used) Palette.Silver.copy(alpha = 0.3f) else Palette.Gold, shape)
        if (clickable) m = m.clickable(onClick = onClick)
        Box(
            m.drawWithContent {
                drawContent()
                if (used) {
                    val inset = 10f
                    drawLine(Palette.Red, Offset(inset, inset), Offset(size.width - inset, size.height - inset), strokeWidth = 6f)
                    drawLine(Palette.Red, Offset(size.width - inset, inset), Offset(inset, size.height - inset), strokeWidth = 6f)
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                symbol,
                color = Palette.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.alpha(if (used) 0.45f else 1f),
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(caption, color = Palette.Silver, fontSize = 12.sp)
    }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = CutCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Gold,
            contentColor = Palette.Navy,
            disabledContainerColor = Palette.Gold.copy(alpha = 0.3f),
            disabledContentColor = Palette.Navy.copy(alpha = 0.6f),
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SilverButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = CutCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (enabled) Palette.Silver else Palette.Silver.copy(alpha = 0.3f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.White),
    ) {
        Text(text, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

/** Small rounded chip used for header actions and topic suggestions. */
@Composable
fun Pill(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    var m = Modifier
        .clip(shape)
        .border(1.dp, Palette.Silver.copy(alpha = if (enabled) 0.7f else 0.25f), shape)
    if (enabled) m = m.clickable(onClick = onClick)
    Box(m.padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(text, color = if (enabled) Palette.White else Palette.Silver.copy(alpha = 0.4f), fontSize = 13.sp)
    }
}

/** Nested gold diamonds, echoing the app icon. */
@Composable
fun DiamondLogo(logoSize: Dp = 88.dp) {
    Canvas(Modifier.size(logoSize)) {
        val c = size.width / 2
        fun diamond(scale: Float): Path = Path().apply {
            val r = c * scale
            moveTo(c, c - r); lineTo(c + r, c); lineTo(c, c + r); lineTo(c - r, c); close()
        }
        drawPath(diamond(1f), Palette.Gold)
        drawPath(diamond(0.72f), Palette.Navy)
        drawPath(diamond(0.72f), Palette.Silver, style = Stroke(width = 3f))
        drawPath(diamond(0.36f), Palette.Gold)
    }
}

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.White,
    unfocusedTextColor = Palette.White,
    focusedBorderColor = Palette.Gold,
    unfocusedBorderColor = Palette.Silver.copy(alpha = 0.5f),
    focusedLabelColor = Palette.Gold,
    unfocusedLabelColor = Palette.Silver,
    cursorColor = Palette.Gold,
    focusedPlaceholderColor = Palette.Silver.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = Palette.Silver.copy(alpha = 0.5f),
)
