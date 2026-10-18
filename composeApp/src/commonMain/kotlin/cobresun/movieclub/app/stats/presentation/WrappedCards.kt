package cobresun.movieclub.app.stats.presentation

import cobresun.movieclub.app.core.presentation.initialsOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.core.presentation.components.icon
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.stats.domain.VolumeUnit
import cobresun.movieclub.app.stats.domain.WrappedCard
import cobresun.movieclub.app.stats.domain.WrappedCardContent
import cobresun.movieclub.app.stats.domain.WrappedTone
import cobresun.movieclub.app.stats.domain.toFixed1
import cobresun.movieclub.app.theme.WrappedColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * The Club Wrapped story cards, ported from the web's
 * `features/wrapped/components`. Cards are laid out at one fixed size and
 * scaled to fit, so a phone, a tablet and the shared image show one layout.
 */

/** The card's own layout size; at 3× density it renders story-sized, 1080 × 1920. */
internal val CARD_WIDTH = 360.dp
internal val CARD_HEIGHT = 640.dp

private val Ink = WrappedColors.Ink

/** web `--ease-emphasized`. */
internal val EmphasizedEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private fun WrappedTone.gradient(): List<Color> = when (this) {
    WrappedTone.VIOLET -> WrappedColors.Violet
    WrappedTone.BLUE -> WrappedColors.Blue
    WrappedTone.EMERALD -> WrappedColors.Emerald
    WrappedTone.ROSE -> WrappedColors.Rose
    WrappedTone.AMBER -> WrappedColors.Amber
    WrappedTone.SKY -> WrappedColors.Sky
}

/**
 * Whether a card's content has started its entrance. Content rises in the
 * first time its card turns towards the viewer, and stays put after that.
 */
internal data class CardReveal(val started: Boolean, val instant: Boolean)

internal val LocalCardReveal = compositionLocalOf { CardReveal(started = true, instant = true) }

/** Staggered fade-up entrance: the [order]th element of a card waits its turn. */
@Composable
private fun Modifier.rise(order: Int): Modifier {
    val reveal = LocalCardReveal.current
    val progress = remember { Animatable(if (reveal.instant) 1f else 0f) }
    LaunchedEffect(reveal) {
        if (reveal.instant) progress.snapTo(1f)
        else if (reveal.started && progress.value < 1f) {
            delay(80L + order * 70L)
            progress.animateTo(1f, tween(420, easing = EmphasizedEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}

/** Circular avatar with initials sized to the circle (web `v-avatar`). */
@Composable
private fun CardAvatar(name: String, imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    val initials = initialsOf(name)
    Box(modifier.size(size).clip(CircleShape).background(Ink.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
        Text(initials, style = TextStyle(color = Ink, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold))
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun Member.firstName(): String = name.trim().substringBefore(' ').ifBlank { name }

private fun signedGap(gap: Double): String = (if (gap > 0) "+" else "−") + abs(gap).toFixed1()

private fun thousands(n: Long): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
private fun CardText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f,
    maxLines: Int = Int.MAX_VALUE,
    tracking: TextUnit = TextUnit.Unspecified,
    lineHeight: Float? = null,
    tabular: Boolean = false,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = Ink.copy(alpha = alpha),
            fontSize = size,
            fontWeight = weight,
            letterSpacing = tracking,
            lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
            fontFeatureSettings = if (tabular) "tnum" else null,
            textAlign = align ?: TextAlign.Unspecified,
        ),
    )
}

/** Small uppercase label used for headers and units (web `tracking-widest`). */
@Composable
private fun Eyebrow(text: String, size: TextUnit, alpha: Float, modifier: Modifier = Modifier) {
    CardText(text.uppercase(), size, modifier, FontWeight.SemiBold, alpha, maxLines = 1, tracking = 0.1.em)
}

/**
 * One card at its fixed layout size: tone gradient, club header, title and
 * "Movie Club" footer around [card]'s content. Type ignores the system font
 * scale so the layout never reflows out of its frame.
 */
@Composable
internal fun WrappedCardFrame(card: WrappedCard, clubName: String, year: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
        Column(
            modifier
                .fillMaxSize()
                .background(Brush.linearGradient(card.tone.gradient()))
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow(clubName, 11.sp, 0.7f, Modifier.weight(1f))
                Eyebrow("Wrapped $year", 11.sp, 0.7f)
            }
            CardText(
                card.label.uppercase(), 14.sp, Modifier.padding(top = 20.dp),
                weight = FontWeight.Bold, alpha = 0.9f, tracking = 0.1.em,
            )
            Column(Modifier.padding(top = 16.dp).weight(1f).fillMaxWidth()) {
                CardContent(card.content)
            }
            Eyebrow("Movie Club", 11.sp, 0.5f, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun CardContent(content: WrappedCardContent) {
    when (content) {
        is WrappedCardContent.Intro -> IntroCard(content)
        is WrappedCardContent.Volume -> VolumeCard(content)
        is WrappedCardContent.RankedWorks -> RankedWorksCard(content)
        is WrappedCardContent.Divisive -> DivisiveCard(content)
        is WrappedCardContent.Person -> PersonCard(content)
        is WrappedCardContent.HotTakes -> HotTakesCard(content)
        is WrappedCardContent.TasteTwins -> TasteTwinsCard(content)
        is WrappedCardContent.ClosestMatches -> ClosestMatchesCard(content)
    }
}

@Composable
private fun CenteredColumn(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    spacing: Arrangement.Vertical = Arrangement.Center,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = spacing, horizontalAlignment = horizontalAlignment) { content() }
}

@Composable
private fun IntroCard(c: WrappedCardContent.Intro) = CenteredColumn {
    Icon(c.clubType.icon(), null, tint = Ink.copy(alpha = 0.8f), modifier = Modifier.size(44.dp).rise(0))
    CardText(
        "${c.year}", 72.sp, Modifier.padding(top = 16.dp).rise(1),
        weight = FontWeight.Bold, lineHeight = 1f, tracking = (-0.025).em, tabular = true,
    )
    CardText("wrapped", 30.sp, Modifier.padding(top = 8.dp).rise(2), weight = FontWeight.SemiBold)
    Row(Modifier.padding(top = 40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IntroTile(c.workCount, c.countLabel, Modifier.weight(1f).rise(3))
        IntroTile(c.memberCount, "${if (c.memberCount == 1) "member" else "members"} scoring", Modifier.weight(1f).rise(4))
    }
}

@Composable
private fun IntroTile(value: Int, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Ink.copy(alpha = 0.1f)).padding(12.dp)) {
        CardText("$value", 36.sp, weight = FontWeight.Bold, tabular = true, maxLines = 1)
        CardText(label, 14.sp, alpha = 0.75f)
    }
}

@Composable
private fun VolumeCard(c: WrappedCardContent.Volume) = CenteredColumn {
    val icon = if (c.unit == VolumeUnit.HOURS) Icons.Outlined.Schedule else Icons.Outlined.Description
    Icon(icon, null, tint = Ink.copy(alpha = 0.8f), modifier = Modifier.size(44.dp).rise(0))
    CardText(
        thousands(c.value), 72.sp, Modifier.padding(top = 16.dp).rise(1),
        weight = FontWeight.Bold, lineHeight = 1f, tracking = (-0.025).em, tabular = true, maxLines = 1,
    )
    CardText(c.unitLabel, 30.sp, Modifier.padding(top = 8.dp).rise(2), weight = FontWeight.SemiBold)
    CardText(c.detail, 18.sp, Modifier.padding(top = 24.dp).rise(3), alpha = 0.8f)
}

@Composable
private fun CardPoster(imageUrl: String?, title: String, width: Dp, radius: Dp) {
    val shape = RoundedCornerShape(radius)
    if (imageUrl.isNullOrBlank()) {
        Box(Modifier.width(width).height(width * 1.5f).clip(shape).background(Ink.copy(alpha = 0.1f)))
    } else {
        Poster(imageUrl, title, Modifier.width(width), shape = shape)
    }
}

@Composable
private fun RankedWorksCard(c: WrappedCardContent.RankedWorks) =
    CenteredColumn(spacing = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        c.works.forEachIndexed { index, review ->
            Row(Modifier.fillMaxWidth().rise(index), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardText("${index + 1}", 24.sp, Modifier.width(20.dp), weight = FontWeight.Bold, alpha = 0.6f)
                CardPoster(review.work.imageUrl, review.work.title, 64.dp, 6.dp)
                CardText(review.work.title, 18.sp, Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 3, lineHeight = 1.25f)
                Column(horizontalAlignment = Alignment.End) {
                    CardText((review.average ?: 0.0).toFixed1(), 30.sp, weight = FontWeight.Bold, tabular = true)
                    Eyebrow("avg", 10.sp, 0.6f)
                }
            }
        }
    }

private const val MAX_DIVISIVE_SCORES = 8

@Composable
private fun DivisiveCard(c: WrappedCardContent.Divisive) = CenteredColumn {
    Row(Modifier.rise(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!c.review.work.imageUrl.isNullOrBlank()) CardPoster(c.review.work.imageUrl, c.review.work.title, 96.dp, 8.dp)
        Column(Modifier.weight(1f)) {
            CardText(c.review.work.title, 24.sp, weight = FontWeight.Bold, maxLines = 3, lineHeight = 1.25f)
            CardText(
                "±${c.spread.toFixed1()} spread · ${c.average.toFixed1()} avg", 14.sp,
                Modifier.padding(top = 8.dp), alpha = 0.8f,
            )
        }
    }
    val shown = c.scores.take(MAX_DIVISIVE_SCORES)
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shown.forEachIndexed { index, entry ->
            Row(Modifier.rise(index + 1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardText(entry.member.firstName(), 14.sp, Modifier.width(80.dp), weight = FontWeight.Medium, maxLines = 1)
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Ink.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((entry.score / 10).toFloat().coerceIn(0f, 1f)).clip(CircleShape).background(Ink))
                }
                CardText(ScoreScale.format(entry.score), 14.sp, Modifier.width(40.dp), maxLines = 1, weight = FontWeight.Bold, tabular = true, align = TextAlign.End)
            }
        }
    }
    val hidden = c.scores.size - shown.size
    if (hidden > 0) CardText("+$hidden more", 12.sp, Modifier.padding(top = 8.dp), alpha = 0.7f)
}

private const val MAX_PERSON_WORKS = 5

@Composable
private fun PersonCard(c: WrappedCardContent.Person) = CenteredColumn(horizontalAlignment = Alignment.CenterHorizontally) {
    CardAvatar(c.name, c.imageUrl, 128.dp, Modifier.rise(0))
    CardText(c.name, 30.sp, Modifier.padding(top = 20.dp).rise(1), weight = FontWeight.Bold, lineHeight = 1.25f, align = TextAlign.Center)
    CardText(
        "${c.workCount} ${c.countNoun} · ${c.averageScore.toFixed1()} avg", 16.sp,
        Modifier.padding(top = 4.dp).rise(2), alpha = 0.8f, align = TextAlign.Center,
    )
    val shown = c.works.take(MAX_PERSON_WORKS)
    Column(Modifier.padding(top = 24.dp).rise(3), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.forEach { CardText(it, 14.sp, alpha = 0.8f, maxLines = 1, align = TextAlign.Center) }
        val hidden = c.works.size - shown.size
        if (hidden > 0) CardText("+$hidden more", 14.sp, alpha = 0.6f)
    }
}

/** "Alice loved" / "Alice is closest to Bob": names bold, the verb softened. */
@Composable
private fun NamesSentence(parts: List<Pair<String, Boolean>>, modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        parts.forEachIndexed { i, (part, isName) ->
            if (i > 0) append(" ")
            if (isName) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) }
            else withStyle(SpanStyle(color = Ink.copy(alpha = 0.75f))) { append(part) }
        }
    }
    Text(text, modifier, style = TextStyle(color = Ink, fontSize = 14.sp, lineHeight = 1.375.em))
}

@Composable
private fun HotTakesCard(c: WrappedCardContent.HotTakes) =
    CenteredColumn(spacing = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        c.takes.forEachIndexed { index, take ->
            Row(Modifier.fillMaxWidth().rise(index), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardAvatar(take.member.name, take.member.imageUrl, 40.dp)
                Column(Modifier.weight(1f)) {
                    NamesSentence(listOf(take.member.firstName() to true, (if (take.gap > 0) "loved" else "panned") to false))
                    CardText(take.review.work.title, 16.sp, weight = FontWeight.Bold, maxLines = 1, lineHeight = 1.375f)
                    CardText(
                        "Gave it ${ScoreScale.format(take.memberScore)} · others ${take.othersAverage.toFixed1()}",
                        12.sp, alpha = 0.75f,
                    )
                }
                CardText(
                    signedGap(take.gap), 14.sp,
                    Modifier.clip(CircleShape).background(Ink.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    weight = FontWeight.Bold, tabular = true,
                )
            }
        }
    }

@Composable
private fun AvatarPair(a: Member, b: Member, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CardAvatar(a.name, a.imageUrl, size)
        CardAvatar(b.name, b.imageUrl, size)
    }
}

@Composable
private fun TasteTwinsCard(c: WrappedCardContent.TasteTwins) =
    CenteredColumn(spacing = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)) {
        Column(Modifier.rise(0)) {
            Eyebrow("Taste twins", 12.sp, 0.7f)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarPair(c.twins.memberA, c.twins.memberB, 56.dp)
                CardText(
                    "${c.twins.memberA.firstName()} & ${c.twins.memberB.firstName()}", 18.sp,
                    weight = FontWeight.SemiBold, lineHeight = 1.25f,
                )
            }
            CardText(
                "${c.twins.similarityPercent.roundToInt()}%", 60.sp, Modifier.padding(top = 12.dp).rise(1),
                weight = FontWeight.Bold, lineHeight = 1f, tabular = true,
            )
            CardText("in sync across ${c.twins.sharedCount} ${c.countNoun}", 14.sp, Modifier.padding(top = 4.dp), alpha = 0.8f)
        }
        c.opposites?.let { odds ->
            Column(Modifier.rise(2)) {
                Eyebrow("Most at odds", 12.sp, 0.7f)
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarPair(odds.memberA, odds.memberB, 40.dp)
                    CardText(
                        "${odds.memberA.firstName()} & ${odds.memberB.firstName()}", 16.sp, Modifier.weight(1f),
                        weight = FontWeight.SemiBold, lineHeight = 1.25f,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        CardText("${odds.similarityPercent.roundToInt()}%", 24.sp, weight = FontWeight.Bold, tabular = true)
                        Eyebrow("in sync", 10.sp, 0.6f)
                    }
                }
            }
        }
    }

@Composable
private fun ClosestMatchesCard(c: WrappedCardContent.ClosestMatches) =
    CenteredColumn(spacing = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        c.matches.forEachIndexed { index, match ->
            Row(Modifier.fillMaxWidth().rise(index), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarPair(match.member, match.match, 40.dp)
                NamesSentence(
                    listOf(match.member.firstName() to true, "is closest to" to false, match.match.firstName() to true),
                    Modifier.weight(1f),
                )
                CardText("${match.similarityPercent.roundToInt()}%", 20.sp, weight = FontWeight.Bold, tabular = true)
            }
        }
    }
