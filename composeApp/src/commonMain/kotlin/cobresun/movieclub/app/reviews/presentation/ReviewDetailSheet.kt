package cobresun.movieclub.app.reviews.presentation

import androidx.compose.ui.focus.onFocusChanged
import cobresun.movieclub.app.core.presentation.dismissKeyboardOnTapOrDrag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.SheetSnackbarHost
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.components.ConfirmSheet
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
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import cobresun.movieclub.app.reviews.domain.ScoredWork
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.core.domain.WorkType
import kotlin.time.Instant

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
    // A comment field has focus (the only text fields in the sheet).
    var typing by remember { mutableStateOf(false) }
    var scoring by remember(review.work.id) { mutableStateOf(false) }
    var poppedAt by remember(review.work.id) { mutableIntStateOf(0) }
    val fullDetails = (detail.details as? AsyncResult.Success)?.data
    val work = if (fullDetails != null) review.work.copy(details = fullDetails) else review.work
    val myScore = review.scoreOf(currentUserId)
    val noun = if (work.type == WorkType.BOOK) "book" else "movie"
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
                .onFocusChanged { typing = it.hasFocus }
                .dismissKeyboardOnTapOrDrag()
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
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = AppMotion.Emphasized)) { -it / 12 } + expandVertically(tween(300)),
                ) {
                    fact?.let { ReviewFactCard(it) }
                }
            }

            work.overview()?.let { SynopsisSection(it, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) }

            val cast = (work.details as? WorkDetails.Movie)?.cast.orEmpty()
            if (cast.isNotEmpty()) {
                CastSection(cast, Modifier.padding(bottom = 20.dp))
            } else if (detail.details is AsyncResult.Loading && work.type == WorkType.MOVIE) {
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
                if (work.type == WorkType.MOVIE) {
                    work.externalId?.let { WatchProvidersSection(it) }
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CommentsSection(
                    comments = detail.thread.comments,
                    currentUserId = currentUserId,
                    isPosting = detail.thread.isPosting,
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
        // While typing a comment the keyboard needs the room; the footer returns after.
        if (currentUserId != null && !typing) {
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
        ConfirmSheet(
            destructive = true,
            title = "Delete review?",
            message = "This removes the whole review for the club — the ${noun} and everyone's scores — and can't be undone. " +
                "To drop just your own score, use Remove my score in the score panel.",
            confirmLabel = "Delete",
            onConfirm = { onAction(ReviewsAction.RemoveReview(work)) },
            onDismiss = { confirmRemove = false },
        )
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
                        onChange(Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date)
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
