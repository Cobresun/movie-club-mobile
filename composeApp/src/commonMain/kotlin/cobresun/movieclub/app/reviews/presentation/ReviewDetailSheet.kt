package cobresun.movieclub.app.reviews.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import cobresun.movieclub.app.core.presentation.components.rememberAppSheetState
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import cobresun.movieclub.app.core.platform.HapticFeedback
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.SheetSnackbarHost
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.components.WorkHero
import cobresun.movieclub.app.providers.presentation.WatchProvidersSection
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewFact
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.outlined.Visibility
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import cobresun.movieclub.app.reviews.domain.ScoredWork

/** Everything about one reviewed work: scores, your score, cast, comments. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailSheet(
    review: Review,
    detail: ReviewDetailState,
    members: List<Member>,
    currentUserId: String?,
    discussionQuestionsEnabled: Boolean,
    scoredWorks: List<ScoredWork>,
    clubSlug: String?,
    /** False hides everyone else's scores and the average until you score or reveal. */
    revealed: Boolean,
    /** Offer "Reveal club scores": hidden, with something to hide. */
    canReveal: Boolean,
    /** Spotlight fact; the caller passes null until the club's scores are revealed. */
    fact: ReviewFact?,
    onAction: (ReviewsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberAppSheetState()
    var confirmRemove by remember { mutableStateOf(false) }
    var scoring by remember(review.work.id) { mutableStateOf(false) }
    var poppedAt by remember(review.work.id) { mutableIntStateOf(0) }
    val fullDetails = (detail.details as? AsyncResult.Success)?.data
    val work = if (fullDetails != null) review.work.copy(details = fullDetails) else review.work
    val myScore = review.scoreOf(currentUserId)
    val noun = if (work.type == cobresun.movieclub.app.core.domain.WorkType.BOOK) "book" else "movie"

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        transparentHandle = true,
    ) {
        Box(Modifier.fillMaxHeight().imePadding()) {
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .then(if (currentUserId == null) Modifier.navigationBarsPadding() else Modifier)
                .padding(bottom = 24.dp),
        ) {
            var tmdbTapped by remember(work.id) { mutableStateOf(false) }
            // Like web, the hero carries the meta line; the TMDB rating lives under Details.
            WorkHero(work, facts = work.reviewMetaLine()) {
                work.addedDate?.let { date ->
                    ReviewedDate(date) { onAction(ReviewsAction.SetReviewedDate(work.id, it)) }
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ClubScores(
                    review = review,
                    members = members,
                    currentUserId = currentUserId,
                    revealed = revealed,
                    canReveal = canReveal,
                    poppedAt = poppedAt,
                    onReveal = { onAction(ReviewsAction.RevealScores(work.id)) },
                )

                // Like the web drawer: the fact sits between the verdict and the reference
                // material, fading in after the sheet opens.
                var factReady by remember(work.id) { mutableStateOf(false) }
                LaunchedEffect(work.id) {
                    // Wait out the sheet's opening slide, like web's idle-callback gate.
                    delay(300)
                    factReady = true
                }
                AnimatedVisibility(
                    visible = factReady && fact != null,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = EmphasizedEasing)) { -it / 12 } + expandVertically(tween(300)),
                ) {
                    fact?.let { ReviewFactCard(it) }
                }
            }

            work.overview()?.let { SynopsisSection(it, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) }

            val cast = (work.details as? WorkDetails.Movie)?.cast.orEmpty()
            if (cast.isNotEmpty()) {
                CastSection(cast, Modifier.padding(bottom = 20.dp))
            } else if (detail.details is AsyncResult.Loading && work.type == cobresun.movieclub.app.core.domain.WorkType.MOVIE) {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { ShimmerBox(Modifier.size(64.dp), MaterialTheme.shapes.extraLarge) }
                }
                Spacer(Modifier.height(20.dp))
            }

            DetailsSection(
                work = work,
                tmdbHidden = ScoreReveal.isTmdbRatingHidden(review, currentUserId, tmdbTapped),
                onRevealTmdb = { tmdbTapped = true },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            ) {
                if (work.type == cobresun.movieclub.app.core.domain.WorkType.MOVIE) {
                    work.externalId?.let { WatchProvidersSection(it) }
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CommentsSection(
                    comments = detail.comments,
                    currentUserId = currentUserId,
                    isPosting = detail.isPostingComment,
                    onAdd = { text, spoiler -> onAction(ReviewsAction.AddComment(text, spoiler)) },
                    onEdit = { id, text, spoiler -> onAction(ReviewsAction.EditComment(id, text, spoiler)) },
                    onDelete = { onAction(ReviewsAction.DeleteComment(it)) },
                    onRetry = { onAction(ReviewsAction.RetryComments) },
                )

                if (discussionQuestionsEnabled) {
                    DiscussionQuestions(detail.questions, noun) { onAction(ReviewsAction.GenerateQuestions) }
                }

                // Deliberately tucked at the end: rare and destructive, so no sticky prominence.
                TextButton(
                    onClick = { confirmRemove = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete review")
                }
            }
        }
        if (currentUserId != null) {
            ScoreFooter(
                noun = noun,
                hasScore = myScore != null,
                onRate = { scoring = true },
                onShare = { onAction(ReviewsAction.Share(work)) },
            )
        }
        }
        SheetSnackbarHost()
        }
    }

    if (scoring && currentUserId != null) {
        ScoreEntrySheet(
            workId = work.id,
            noun = noun,
            existing = myScore?.value,
            assist = remember(work.id, scoredWorks) {
                val target = ScoreAssist.Target(
                    workId = work.id,
                    type = work.type,
                    externalId = work.externalId,
                    genres = (work.details as? WorkDetails.Movie)?.genres.orEmpty(),
                )
                ScoreAssistInput(target, work.title, work.imageUrl, ScoreAssist.buildCandidatePool(scoredWorks, target))
            },
            currentClubSlug = clubSlug,
            onSave = {
                onAction(ReviewsAction.SaveScore(work.id, it))
                poppedAt++
            },
            onRemove = { onAction(ReviewsAction.DeleteScore(work.id)) },
            onDismiss = { scoring = false },
        )
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Delete review?",
            message = "This removes the whole review for the club — the ${noun} and everyone's scores — and can't be undone. " +
                "To drop just your own score, use Remove my score in the score panel.",
            confirmLabel = "Delete",
            onConfirm = { onAction(ReviewsAction.RemoveReview(work)) },
            onDismiss = { confirmRemove = false },
        )
    }
}

/** One tile in the club-scores grid: a member's score, or the club average (memberId null). */
private data class ScoreTile(val id: String, val memberId: String?, val name: String, val imageUrl: String?, val value: Double)

/**
 * Club scores as web's tile grid: members who scored, in club order, then the
 * average spanning both columns. Hidden scores show an eye until revealed, then
 * settle in one after another (80ms apart). [poppedAt] changes each time you
 * save a new score, which pops your own tile.
 */
@Composable
private fun ClubScores(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
    revealed: Boolean,
    canReveal: Boolean,
    poppedAt: Int,
    onReveal: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Club scores")
        // Like web's reveal pill: collapses away once scores are revealed (or you score).
        AnimatedVisibility(visible = canReveal, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalButton(onClick = onReveal) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reveal club scores")
                }
            }
        }
        val tiles = buildList {
            members.forEach { m ->
                review.scores[m.id]?.let { add(ScoreTile("member_${m.id}", m.id, m.name.trim().substringBefore(' ').ifBlank { m.name }, m.imageUrl, it.value)) }
            }
            review.average?.let { add(ScoreTile("average", null, "Average", null, it)) }
        }
        if (tiles.isEmpty()) {
            Text(
                "No scores yet — be the first to rate it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        tiles.filter { it.memberId != null }.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (index, tile) ->
                    ScoreTileView(
                        tile = tile,
                        index = index,
                        hidden = ScoreReveal.isHidden(tile.memberId, currentUserId, revealed),
                        poppedAt = if (tile.memberId == currentUserId) poppedAt else 0,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        tiles.firstOrNull { it.memberId == null }?.let { avg ->
            ScoreTileView(
                tile = avg,
                index = tiles.size - 1,
                hidden = ScoreReveal.isHidden(null, currentUserId, revealed),
                poppedAt = 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScoreTileView(tile: ScoreTile, index: Int, hidden: Boolean, poppedAt: Int, modifier: Modifier = Modifier) {
    val haptics = koinInject<HapticFeedback>()
    val isAverage = tile.memberId == null
    val primary = MaterialTheme.colorScheme.primary

    // Reveal: hidden → shown settles from a slight shrink, staggered by position.
    val reveal = remember { Animatable(if (hidden) 0f else 1f) }
    LaunchedEffect(hidden) {
        if (hidden) reveal.snapTo(0f)
        else if (reveal.value < 1f) {
            delay(index * 80L)
            reveal.animateTo(1f, tween(500, easing = EmphasizedEasing))
        }
    }

    // Score pop (web `score-pop`): springy overshoot with a wobble and a fading primary ring.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(poppedAt) {
        if (poppedAt == 0) return@LaunchedEffect
        delay(250) // let the score sheet slide away first
        haptics.performSuccess()
        pop.snapTo(0f)
        pop.animateTo(1f, tween(700, easing = EmphasizedEasing))
    }
    val p = pop.value
    val popScale = keyframe(p, 1f, 1.12f, 0.97f, 1.05f, 1f)
    val popRotation = keyframe(p, 0f, -2.5f, 2f, -1f, 0f)
    val ringAlpha = keyframe(p, 0f, 0.35f, 0.25f, 0.15f, 0f)
    val ringWidth = keyframe(p, 0f, 5f, 4f, 3f, 0f)

    val shape = MaterialTheme.shapes.large
    Row(
        modifier
            .zIndex(if (p < 1f) 1f else 0f)
            .graphicsLayer {
                scaleX = popScale
                scaleY = popScale
                rotationZ = popRotation
            }
            .drawBehind {
                if (ringAlpha > 0f) {
                    val w = ringWidth.dp.toPx()
                    drawRoundRect(
                        primary.copy(alpha = ringAlpha),
                        topLeft = androidx.compose.ui.geometry.Offset(-w, -w),
                        size = androidx.compose.ui.geometry.Size(size.width + 2 * w, size.height + 2 * w),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() + w),
                    )
                }
            }
            .clip(shape)
            .background(if (isAverage) primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (isAverage) Modifier.border(1.dp, primary.copy(alpha = 0.25f), shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isAverage) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            }
        } else {
            Avatar(tile.name, tile.imageUrl, size = 28.dp)
        }
        Text(
            tile.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (hidden) {
            HiddenScoreIcon(Modifier.size(20.dp))
        } else {
            Text(
                ScoreScale.format(tile.value),
                style = if (isAverage) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (isAverage) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isAverage) primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    val s = 0.9f + 0.1f * reveal.value
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}

/** Piecewise-linear value at [t] across keyframes at 0, 25%, 50%, 70% and 100% (web `score-pop`). */
private fun keyframe(t: Float, v0: Float, v25: Float, v50: Float, v70: Float, v100: Float): Float {
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
    return when {
        t <= 0.25f -> lerp(v0, v25, t / 0.25f)
        t <= 0.5f -> lerp(v25, v50, (t - 0.25f) / 0.25f)
        t <= 0.7f -> lerp(v50, v70, (t - 0.5f) / 0.2f)
        else -> lerp(v70, v100, (t - 0.7f) / 0.3f)
    }
}

/**
 * Web `DiscussionQuestions`: a generate button until there are questions, then a
 * card whose numbered questions deal in one after another (70ms apart). A
 * regenerate dims the current set; if it fails, they stay with a note.
 */
@Composable
private fun DiscussionQuestions(state: DiscussionQuestionsState, noun: String, onGenerate: () -> Unit) {
    val questions = state.questions
    val phase = when {
        !questions.isNullOrEmpty() -> "loaded"
        questions != null -> "unrecognized"
        state.failed -> "error"
        else -> "idle"
    }
    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        modifier = Modifier.fillMaxWidth(),
        label = "discussion-questions",
    ) { p ->
        when (p) {
            "loaded" -> Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text("Discussion questions", style = MaterialTheme.typography.titleSmall)
                }
                Column(
                    Modifier.graphicsLayer { alpha = if (state.isLoading) 0.5f else 1f },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    questions.orEmpty().forEachIndexed { i, q ->
                        // Keyed by text so a regenerated set remounts and deals in again.
                        key("$i-$q") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .fadeUpOnEnter(delayMillis = i * 70)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text(q, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (state.failed) {
                    Text(
                        "Couldn't regenerate. Showing the previous questions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onGenerate, enabled = !state.isLoading) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (state.isLoading) "Regenerating…" else "Regenerate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            "unrecognized" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "We couldn't recognize this $noun, so we couldn't generate discussion questions for it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                GenerateQuestionsButton("Generate discussion questions", loading = false, enabled = false, onClick = onGenerate)
            }

            "error" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Couldn't generate questions. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                GenerateQuestionsButton("Try again", loading = state.isLoading, enabled = !state.isLoading, onClick = onGenerate)
            }

            else -> GenerateQuestionsButton(
                if (state.isLoading) "Generating questions…" else "Generate discussion questions",
                loading = state.isLoading,
                enabled = !state.isLoading,
                onClick = onGenerate,
            )
        }
    }
}

/** The AI button: a slowly turning gradient ring (web `ai-shimmer-button`), faster while loading. */
@Composable
private fun GenerateQuestionsButton(label: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ai-shimmer")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (loading) 1_200 else 4_000, easing = LinearEasing)),
        label = "ai-angle",
    )
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )
    val shape = MaterialTheme.shapes.medium
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled || loading) 1f else 0.5f }
            .clip(shape)
            .drawBehind {
                rotate(angle) {
                    drawCircle(Brush.sweepGradient(colors), radius = size.maxDimension)
                }
            }
            .padding(1.5.dp),
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "Reviewed <date>" with a pencil; tapping opens a date picker to correct it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewedDate(date: LocalDate, onChange: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = "Change reviewed date") { picking = true }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Reviewed ${date.formatShort()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
        )
    }
    if (picking) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    pickerState.selectedDateMillis?.let { ms ->
                        onChange(kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date)
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState, title = { Text("Reviewed on", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

/**
 * The sheet's primary action, always one tap away: "Rate this movie" while
 * unrated; once scored it turns secondary ("Edit score") and Share becomes the
 * primary button beside it.
 */
@Composable
private fun ScoreFooter(noun: String, hasScore: Boolean, onRate: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRate,
                shape = MaterialTheme.shapes.medium,
                colors = if (hasScore) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                } else ButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(if (hasScore) Icons.Outlined.Edit else Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (hasScore) "Edit score" else "Rate this $noun", fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(visible = hasScore, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                Button(onClick = onShare, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
