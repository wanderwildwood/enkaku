package com.atharok.btremote.ui.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.max

// ---- Adaptive ----

@Composable
fun AdaptiveText(
    text: String,
    @FloatRange(from = 0.0, to = 1.0) percent: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign = TextAlign.Unspecified
) {
    require(percent in 0f..1f) { "Percentage must be between 0 and 1" }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val targetFontSize = with(density) {
            containerWidth.times(percent).coerceAtMost(containerHeight.times(percent)).toSp()
        }
        // Never below 14 sp, unless the box itself is smaller than that.
        val minFontSize = max(targetFontSize.value / 10f, minOf(targetFontSize.value, 14f)).sp
        val stepSize = max(minFontSize.value, 0.0001f).sp
        BasicText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            // Over the theme's body style, so a key's label is in Lato like every other word.
            style = MaterialTheme.typography.bodyLarge.merge(
                color = if (color.isSpecified) color else LocalContentColor.current,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                fontSize = targetFontSize,
                textAlign = textAlign
            ),
            overflow = TextOverflow.Clip,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                stepSize = stepSize,
                minFontSize = minFontSize,
                maxFontSize = targetFontSize
            )
        )
    }
}

// ---- Large ----

@Composable
fun TextLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        textAlign = textAlign
    )
}

// ---- Medium ----

@Composable
fun TextMedium(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

// ---- Normal ----

@Composable
fun TextNormal(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun TextNormalSecondary(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE
) {
    TextNormal(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

// ---- Small ----

@Composable
fun TextSmall(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE
) {
    TextNormal(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}