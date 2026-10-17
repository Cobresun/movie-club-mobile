package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.reviews.domain.ScoreBands
import cobresun.movieclub.app.reviews.domain.ScoreDialGeometry
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Arc-gauge score slider (port of web `ScoreDial.vue`): a semicircle from 0
 * (left) over 5 (top) to 10 (right) that snaps to half steps, with a center
 * readout that also takes precise decimals typed by hand. Drag and type share
 * [text], which stays a string so partial typing ("8.") round-trips.
 *
 * Dragging works anywhere on the dial by angle around its center, so a finger
 * can leave the track; below the baseline it clamps to 0 or 10. Each half-step
 * detent crossed ticks once, seeded from the current score on touch-down so
 * grabbing the handle where it already sits stays silent.
 */
@Composable
fun ScoreDial(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val haptics = koinInject<HapticFeedback>()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val currentText by rememberUpdatedState(text)
    val currentOnTextChange by rememberUpdatedState(onTextChange)

    val parsed = text.toDoubleOrNull()
    val hasScore = ScoreScale.isValid(parsed)
    val band = ScoreBands.of(if (hasScore) parsed!! else 0.0)
    val targetFraction = if (hasScore) ScoreDialGeometry.scoreToFraction(parsed!!) else 0f
    var dragging by remember { mutableStateOf(false) }

    // Tracks the finger 1:1 while dragging; eases (web --motion-fast) when the
    // value changes any other way, e.g. typing or a Score Assist suggestion.
    val fraction by animateFloatAsState(
        targetFraction,
        animationSpec = if (dragging) snap() else tween(150, easing = FastOutSlowInEasing),
        label = "dial-fraction",
    )
    val bandColor by animateColorAsState(
        Color(band.argb),
        animationSpec = if (dragging) snap() else tween(150),
        label = "dial-color",
    )

    var clampNotice by remember { mutableStateOf<String?>(null) }
    var noticeKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(noticeKey) {
        if (clampNotice != null) {
            delay(2_500)
            clampNotice = null
        }
    }
    val shake = remember { Animatable(0f) }
    fun rejectInput() {
        haptics.performWarning()
        scope.launch {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 300
                -6f at 60
                6f at 120
                -6f at 180
                6f at 240
            })
        }
    }

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val shadowColor = MaterialTheme.colorScheme.scrim

    Box(modifier.widthIn(max = 260.dp).fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ScoreDialGeometry.VIEWPORT_WIDTH / ScoreDialGeometry.VIEWPORT_HEIGHT)
                .semantics {
                    contentDescription = "Score"
                    stateDescription = if (hasScore) "${ScoreScale.format(parsed!!)}, ${band.label}" else "Not scored"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = (if (hasScore) parsed!! else 0.0).toFloat(),
                        range = ScoreScale.MIN.toFloat()..ScoreScale.MAX.toFloat(),
                        steps = ((ScoreScale.MAX - ScoreScale.MIN) / ScoreScale.STEP).toInt() - 1,
                    )
                    if (enabled) {
                        setProgress { value ->
                            val snapped = ScoreScale.snap(value.toDouble())
                            if (snapped == currentText.toDoubleOrNull()) return@setProgress false
                            currentOnTextChange(ScoreScale.format(snapped))
                            true
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        focusManager.clearFocus()
                        dragging = true
                        var lastDetent = currentText.toDoubleOrNull()?.takeIf { ScoreScale.isValid(it) }
                        val scale = ScoreDialGeometry.VIEWPORT_WIDTH / size.width
                        fun apply(position: Offset) {
                            val score = ScoreDialGeometry.scoreFromPoint(position.x * scale, position.y * scale)
                            if (score != lastDetent) {
                                lastDetent = score
                                haptics.performTick()
                            }
                            val formatted = ScoreScale.format(score)
                            if (formatted != currentText) currentOnTextChange(formatted)
                        }
                        apply(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            apply(change.position)
                            change.consume()
                        }
                        dragging = false
                    }
                },
        ) {
            val scale = size.width / ScoreDialGeometry.VIEWPORT_WIDTH
            val radius = ScoreDialGeometry.RADIUS * scale
            val stroke = ScoreDialGeometry.TRACK_WIDTH * scale
            val topLeft = Offset(
                (ScoreDialGeometry.CENTER_X - ScoreDialGeometry.RADIUS) * scale,
                (ScoreDialGeometry.CENTER_Y - ScoreDialGeometry.RADIUS) * scale,
            )
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(trackColor, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fraction > 0f) {
                drawArc(bandColor, 180f, 180f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val handle = ScoreDialGeometry.handlePosition(fraction)
            val center = Offset(handle.x * scale, handle.y * scale)
            val handleRadius = ScoreDialGeometry.HANDLE_RADIUS * scale
            // Soft drop shadow under the white handle, like web's `drop-shadow`.
            drawCircle(shadowColor.copy(alpha = 0.18f), handleRadius + 3.dp.toPx(), center + Offset(0f, 1.5.dp.toPx()))
            drawCircle(shadowColor.copy(alpha = 0.22f), handleRadius + 1.dp.toPx(), center + Offset(0f, 1.dp.toPx()))
            drawCircle(White, handleRadius, center)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val idleColor = MaterialTheme.colorScheme.onSurfaceVariant
            val underline = MaterialTheme.colorScheme.outline
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val result = ScoreScale.sanitize(raw)
                    onTextChange(result.value)
                    if (result.corrected) rejectInput()
                    when (result.clampedTo) {
                        ScoreScale.Bound.MAX -> "Max is ${ScoreScale.format(ScoreScale.MAX)} — set to ${ScoreScale.MAX.toOneDecimal()}"
                        ScoreScale.Bound.MIN -> "Min is ${ScoreScale.format(ScoreScale.MIN)} — set to ${ScoreScale.MIN.toOneDecimal()}"
                        null -> null
                    }?.let {
                        clampNotice = it
                        noticeKey++
                    }
                },
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (hasScore) bandColor else idleColor,
                    fontFeatureSettings = "tnum",
                ),
                cursorBrush = SolidColor(if (hasScore) bandColor else idleColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSave()
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) {
                            Text(
                                "8.5",
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                                color = idleColor.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .width(104.dp)
                    .focusRequester(focusRequester)
                    .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                    .semantics { contentDescription = "Score" }
                    .drawBehind {
                        // Dotted underline marks the readout as editable.
                        val y = size.height - 1.dp.toPx()
                        drawLine(
                            underline,
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
                        )
                    },
            )
            Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = clampNotice ?: band.label.takeIf { hasScore }?.uppercase(),
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    label = "dial-label",
                ) { label ->
                    when {
                        label == null -> Box(Modifier)
                        label == clampNotice -> Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.scrim,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(bandColor)
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                        else -> Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.em,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun Double.toOneDecimal(): String = ((this * 10).roundToInt() / 10.0).let {
    if (it == it.toLong().toDouble()) "${it.toLong()}.0" else it.toString()
}
