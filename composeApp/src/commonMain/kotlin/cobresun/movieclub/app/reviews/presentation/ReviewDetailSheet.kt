package cobresun.movieclub.app.reviews.presentation

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import cobresun.movieclub.app.core.presentation.components.CastRow
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.components.WorkAbout
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
    /** False hides everyone else's scores and the average until you score or reveal. */
    revealed: Boolean,
    /** Offer "Reveal club scores": hidden, with something to hide. */
    canReveal: Boolean,
    /** Spotlight fact; the caller passes null until the club's scores are revealed. */
    fact: ReviewFact?,
    onAction: (ReviewsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmRemove by remember { mutableStateOf(false) }
    val fullDetails = (detail.details as? AsyncResult.Success)?.data
    val work = if (fullDetails != null) review.work.copy(details = fullDetails) else review.work
    val myScore = review.scoreOf(currentUserId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null,
    ) {
        Box {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            WorkHero(work) {
                work.addedDate?.let { date ->
                    ReviewedDate(date) { onAction(ReviewsAction.SetReviewedDate(work.id, it)) }
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ScoreEntryPanel(
                    workId = work.id,
                    existing = myScore?.value,
                    isSaving = detail.isSavingScore,
                    enabled = currentUserId != null,
                    onSave = { onAction(ReviewsAction.SaveScore(work.id, it)) },
                    onDelete = { onAction(ReviewsAction.DeleteScore(work.id)) },
                    assist = remember(work.id, scoredWorks) {
                        val target = ScoreAssist.Target(
                            workId = work.id,
                            type = work.type,
                            externalId = work.externalId,
                            genres = (work.details as? WorkDetails.Movie)?.genres.orEmpty(),
                        )
                        ScoreAssistInput(target, work.title, work.imageUrl, ScoreAssist.buildCandidatePool(scoredWorks, target))
                    },
                )

                ClubScores(
                    review = review,
                    members = members,
                    currentUserId = currentUserId,
                    revealed = revealed,
                    canReveal = canReveal,
                    onReveal = { onAction(ReviewsAction.RevealScores(work.id)) },
                )

                // Like the web drawer: the fact sits between the verdict and the reference
                // material, fading in after the sheet opens.
                AnimatedVisibility(visible = fact != null, enter = fadeIn() + expandVertically()) {
                    fact?.let { ReviewFactCard(it) }
                }
            }

            if (work.type == cobresun.movieclub.app.core.domain.WorkType.MOVIE) {
                work.externalId?.let {
                    WatchProvidersSection(it, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp))
                }
            }

            val cast = (work.details as? WorkDetails.Movie)?.cast.orEmpty()
            if (cast.isNotEmpty()) {
                SectionLabel("Cast", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
                CastRow(cast)
                Spacer(Modifier.height(20.dp))
            } else if (detail.details is AsyncResult.Loading && work.type == cobresun.movieclub.app.core.domain.WorkType.MOVIE) {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { ShimmerBox(Modifier.size(64.dp), MaterialTheme.shapes.extraLarge) }
                }
                Spacer(Modifier.height(20.dp))
            }

            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (work.details != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("About")
                        WorkAbout(work)
                    }
                }

                if (discussionQuestionsEnabled) {
                    DiscussionQuestions(detail.questions) { onAction(ReviewsAction.GenerateQuestions) }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CommentsSection(
                    comments = detail.comments,
                    currentUserId = currentUserId,
                    isPosting = detail.isPostingComment,
                    onAction = onAction,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onAction(ReviewsAction.Share(work)) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                    OutlinedButton(
                        onClick = { confirmRemove = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove")
                    }
                }
            }
        }
        SheetSnackbarHost()
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove ${work.title} from reviews?",
            message = "Every member's score for it will be removed too. This can't be undone.",
            confirmLabel = "Remove",
            onConfirm = { onAction(ReviewsAction.RemoveReview(work)) },
            onDismiss = { confirmRemove = false },
        )
    }
}

/** Club average plus a bar per member, highest first; unscored members trail. */
@Composable
private fun ClubScores(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
    revealed: Boolean,
    canReveal: Boolean,
    onReveal: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Club scores", Modifier.weight(1f))
            review.average?.let {
                ScoreBadge(it.takeUnless { ScoreReveal.isHidden(null, currentUserId, revealed) }, large = false)
            }
        }
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
        val known = members.associateBy { it.id }
        val memberOrder = (members.map { it.id } + review.scores.keys).distinct()
            .map { id -> Triple(id, known[id], review.scores[id]?.value) }
        // Ranking by score would give hidden scores away, so keep club order until revealed.
        val rows = if (!revealed) memberOrder.sortedByDescending { it.third != null }
        else memberOrder.sortedWith(compareByDescending<Triple<String, Member?, Double?>> { it.third != null }.thenByDescending { it.third })
        if (rows.isEmpty()) {
            Text("No scores yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rows.forEach { (id, member, score) ->
            val name = member?.name ?: "Former member"
            val hidden = score != null && ScoreReveal.isHidden(id, currentUserId, revealed)
            val value = score.takeUnless { hidden }
            val fraction by animateFloatAsState(((value ?: 0.0) / ScoreScale.MAX).toFloat())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(name, member?.imageUrl, size = 28.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (id == currentUserId) "$name (you)" else name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    if (id == currentUserId) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
                if (hidden) {
                    Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) {
                        HiddenScoreIcon(Modifier.size(20.dp))
                    }
                } else {
                    Text(
                        value?.let(ScoreScale::format) ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(44.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscussionQuestions(questions: AsyncResult<List<String>>?, onGenerate: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text("Discussion questions", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        when (questions) {
            null -> FilledTonalButton(onClick = onGenerate) { Text("Generate questions") }
            is AsyncResult.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Thinking up questions…", style = MaterialTheme.typography.bodyMedium)
            }

            is AsyncResult.Error -> TextButton(onClick = onGenerate) { Text("Try again") }
            is AsyncResult.Success -> {
                questions.data.forEachIndexed { i, q ->
                    Text("${i + 1}. $q", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onGenerate) { Text("Regenerate") }
            }
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
