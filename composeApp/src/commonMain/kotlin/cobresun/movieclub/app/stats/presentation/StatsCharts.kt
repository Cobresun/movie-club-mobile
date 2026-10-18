package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.stats.domain.TimePoint
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Instant

/*
 * Small hand-drawn Canvas charts. They're deliberately minimal: faint
 * horizontal gridlines, axis labels in the subdued text color, and a short
 * grow-in animation. No tooltips; widgets list the notable works beneath.
 */

private val AXIS_GUTTER = 28.dp
private val BOTTOM_GUTTER = 20.dp
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

internal fun monthLabel(month: Int, year: Int, withYear: Boolean = true): String =
    MONTHS[month - 1] + if (withYear) " '" + (year % 100).toString().padStart(2, '0') else ""

private fun epochLabel(millis: Long): String {
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC)
    return monthLabel(d.month.ordinal + 1, d.year)
}

/**
 * The chart's grow-in progress, read only inside the Canvas draw block: the
 * chart redraws for 600ms instead of recomposing every frame. Skipped when the
 * widget isn't animating in (scrolled back into view, reduced motion).
 */
@Composable
private fun rememberGrow(key: Any?): State<Float> {
    val animateIn = LocalAnimateIn.current
    val anim = remember(key) { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(key) { if (anim.value < 1f) anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    return anim.asState()
}

private class ChartColors(val grid: Color, val label: Color)

@Composable
private fun chartColors() = ChartColors(
    grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    label = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Whole-number steps when the range is small (e.g. 6..10), else quarters. */
internal fun gridSteps(lo: Double, hi: Double): Int = (hi - lo).toInt().takeIf { it in 2..6 && (hi - lo) % 1.0 == 0.0 } ?: 4

/** Smallest multiple of 4 ≥ [value] (min 4), so the 4 gridline steps land on whole numbers. */
internal fun niceMax(value: Double): Double = max(1.0, ceil(value / 4)) * 4

private fun DrawScope.yGrid(
    measurer: TextMeasurer,
    colors: ChartColors,
    min: Double,
    max: Double,
    steps: Int,
    plotLeft: Float,
    plotBottom: Float,
    plotTop: Float,
    format: (Double) -> String,
) {
    val style = TextStyle(fontSize = 10.sp, color = colors.label)
    for (i in 0..steps) {
        val value = min + (max - min) * i / steps
        val y = plotBottom - (plotBottom - plotTop) * i / steps
        drawLine(
            colors.grid, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1f,
            pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
        val layout = measurer.measure(format(value), style)
        drawText(layout, topLeft = Offset(plotLeft - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
    }
}

/** Score histogram: one column per whole score 0..10, stacked by member. */
@Composable
fun StackedScoreBars(
    binsBySeries: List<Pair<Color, List<Int>>>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    // Room for every axis label, so they aren't re-laid out on each draw.
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val colors = chartColors()
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val grow = rememberGrow(binsBySeries)
    val yMax = remember(binsBySeries) {
        val totals = (0..10).map { b -> binsBySeries.sumOf { it.second[b] } }
        niceMax(max(1, totals.max()).toDouble())
    }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val left = AXIS_GUTTER.toPx()
        val bottom = size.height - BOTTOM_GUTTER.toPx()
        val top = 6.dp.toPx()
        yGrid(measurer, colors, 0.0, yMax, 4, left, bottom, top) { it.toInt().toString() }
        val slot = (size.width - left) / 11f
        val barW = slot * 0.62f
        val labelStyle = TextStyle(fontSize = 10.sp, color = colors.label)
        for (bin in 0..10) {
            val x = left + slot * bin + (slot - barW) / 2
            var y = bottom
            binsBySeries.forEachIndexed { i, (color, bins) ->
                val count = bins[bin]
                if (count == 0) return@forEachIndexed
                val h = ((bottom - top) * count / yMax * grow.value).toFloat()
                val isTop = binsBySeries.drop(i + 1).none { it.second[bin] > 0 }
                drawRoundRect(
                    color, Offset(x, y - h), Size(barW, h),
                    cornerRadius = CornerRadius(if (isTop) 4.dp.toPx() else 0f),
                )
                // Surface-colored seam keeps stacked segments separable.
                drawLine(surface, Offset(x, y), Offset(x + barW, y), strokeWidth = 1.dp.toPx())
                y -= h
            }
            val layout = measurer.measure(bin.toString(), labelStyle)
            drawText(layout, topLeft = Offset(left + slot * bin + (slot - layout.size.width) / 2, bottom + 4.dp.toPx()))
        }
    }
}

/** Monthly counts as slim rounded columns, with a handful of month labels. */
@Composable
fun MonthlyBars(
    counts: List<Int>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
) {
    // Room for every axis label, so they aren't re-laid out on each draw.
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val colors = chartColors()
    val grow = rememberGrow(counts)
    val yMax = niceMax(max(1, counts.maxOrNull() ?: 1).toDouble())
    Canvas(modifier.fillMaxWidth().height(height)) {
        val left = AXIS_GUTTER.toPx()
        val bottom = size.height - BOTTOM_GUTTER.toPx()
        val top = 6.dp.toPx()
        yGrid(measurer, colors, 0.0, yMax, 4, left, bottom, top) { it.toInt().toString() }
        if (counts.isEmpty()) return@Canvas
        val slot = (size.width - left) / counts.size
        val barW = (slot * 0.7f).coerceAtMost(22.dp.toPx())
        counts.forEachIndexed { i, c ->
            if (c == 0) return@forEachIndexed
            val h = ((bottom - top) * c / yMax * grow.value).toFloat()
            drawRoundRect(
                Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f)), startY = bottom - h, endY = bottom),
                Offset(left + slot * i + (slot - barW) / 2, bottom - h), Size(barW, h),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
        val style = TextStyle(fontSize = 10.sp, color = colors.label)
        val every = max(1, ceil(counts.size / 4.0).toInt())
        for (i in counts.indices step every) {
            val layout = measurer.measure(labels[i], style)
            val cx = left + slot * i + slot / 2
            drawText(layout, topLeft = Offset((cx - layout.size.width / 2).coerceIn(left, size.width - layout.size.width), bottom + 4.dp.toPx()))
        }
    }
}

/** Time-series lines sharing one time axis. Single-series charts get an area fill. */
@Composable
fun TimeLines(
    series: List<Pair<Color, List<TimePoint>>>,
    modifier: Modifier = Modifier,
    yMin: Double? = null,
    yMax: Double? = null,
    height: Dp = 190.dp,
    format: (Double) -> String = { if (it % 1.0 == 0.0) it.toInt().toString() else ((it * 10).toInt() / 10.0).toString() },
) {
    // Room for every axis label, so they aren't re-laid out on each draw.
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val colors = chartColors()
    val grow = rememberGrow(series)
    val all = series.flatMap { it.second }
    if (all.isEmpty()) return
    val minX = all.minOf { it.epochMillis }
    val maxX = max(all.maxOf { it.epochMillis }, minX + 1)
    val lo = yMin ?: 0.0
    val hi = yMax ?: niceMax(all.maxOf { it.value })
    Canvas(modifier.fillMaxWidth().height(height)) {
        val left = AXIS_GUTTER.toPx()
        val bottom = size.height - BOTTOM_GUTTER.toPx()
        val top = 6.dp.toPx()
        val right = size.width - 4.dp.toPx()
        yGrid(measurer, colors, lo, hi, gridSteps(lo, hi), left, bottom, top, format)
        fun px(p: TimePoint) = Offset(
            left + (right - left) * ((p.epochMillis - minX).toFloat() / (maxX - minX)),
            (bottom - (bottom - top) * ((p.value - lo) / (hi - lo)).coerceIn(0.0, 1.0)).toFloat(),
        )
        clipRectRevealing(left, right, grow.value) {
            series.forEach { (color, points) ->
                if (points.isEmpty()) return@forEach
                val offsets = points.map(::px)
                val line = Path().apply {
                    moveTo(offsets[0].x, offsets[0].y)
                    for (i in 1 until offsets.size) {
                        val p0 = offsets[i - 1]
                        val p1 = offsets[i]
                        // Same-day points would make the curve loop; draw those straight.
                        if (p1.x - p0.x < 4f) lineTo(p1.x, p1.y)
                        else {
                            val mx = (p0.x + p1.x) / 2
                            cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
                        }
                    }
                }
                if (series.size == 1) {
                    val area = Path().apply {
                        addPath(line)
                        lineTo(offsets.last().x, bottom)
                        lineTo(offsets.first().x, bottom)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), startY = top, endY = bottom))
                }
                drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (offsets.size == 1) drawCircle(color, 4.dp.toPx(), offsets[0])
                drawCircle(color, 3.5.dp.toPx(), offsets.last())
            }
        }
        val style = TextStyle(fontSize = 10.sp, color = colors.label)
        val first = measurer.measure(epochLabel(minX), style)
        drawText(first, topLeft = Offset(left, bottom + 4.dp.toPx()))
        if (maxX - minX > 1) {
            val last = measurer.measure(epochLabel(maxX), style)
            drawText(last, topLeft = Offset(right - last.size.width, bottom + 4.dp.toPx()))
        }
    }
}

private inline fun DrawScope.clipRectRevealing(left: Float, right: Float, fraction: Float, block: DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(0f, 0f, left + (right - left + 8f) * fraction, size.height)
    block()
    drawContext.canvas.restore()
}
