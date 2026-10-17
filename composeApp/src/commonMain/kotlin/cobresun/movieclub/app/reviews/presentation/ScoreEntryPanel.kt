package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.ScoredWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.round

/** What Score Assist needs to compare the work being scored against the user's other scores. */
data class ScoreAssistInput(
    val target: ScoreAssist.Target,
    val title: String,
    val imageUrl: String?,
    val pool: List<ScoredWork>,
)

private enum class ScoreEntryMode { ENTRY, ASSIST }

/**
 * Score entry in its own bottom sheet, opened from the detail sheet's sticky
 * footer (web `ScoreEntryModal` / `ScoreEntryDock`). One surface with swappable
 * content: "Not sure?" replaces the dial with Score Assist in place, and its
 * suggestion comes back pre-filled on the dial for the user to save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEntrySheet(
    workId: String,
    /** "movie" or "book". */
    noun: String,
    existing: Double?,
    assist: ScoreAssistInput?,
    currentClubSlug: String?,
    /** Persist [score]; only called when it differs from [existing]. */
    onSave: (Double) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(ScoreEntryMode.ENTRY) }
    var suggested by remember { mutableStateOf<Double?>(null) }
    var text by remember(workId, existing) { mutableStateOf(existing?.let(ScoreScale::format) ?: "") }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    modifier = Modifier.weight(1f),
                    label = "score-entry-header",
                ) { m ->
                    if (m == ScoreEntryMode.ASSIST) {
                        Row {
                            TextButton(onClick = { mode = ScoreEntryMode.ENTRY }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Back")
                            }
                        }
                    } else {
                        // The title is already in the detail sheet's hero, so name the action.
                        Text(
                            if (existing != null) "Edit your score" else "Score this $noun",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = ::close) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close score entry")
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(150, delayMillis = 60)) togetherWith fadeOut(tween(150)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(300) })
                },
                label = "score-entry-mode",
            ) { m ->
                when (m) {
                    ScoreEntryMode.ENTRY -> ScoreEntryContent(
                        text = text,
                        onTextChange = { text = it },
                        existing = existing,
                        suggested = suggested,
                        showAssist = assist != null && ScoreAssist.isEligible(assist.pool),
                        onAssist = { mode = ScoreEntryMode.ASSIST },
                        onSave = { score ->
                            if (score != existing) onSave(score)
                            close()
                        },
                        onRemove = {
                            onRemove()
                            close()
                        },
                    )

                    ScoreEntryMode.ASSIST -> if (assist != null) {
                        ScoreAssistFlow(
                            input = assist,
                            currentClubSlug = currentClubSlug,
                            onSuggest = { score ->
                                suggested = score
                                text = ScoreScale.format(score)
                                mode = ScoreEntryMode.ENTRY
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The dial, the Score Assist entry point, "Save score" (always visible, enabled
 * once the readout parses to a valid score) and an inline remove confirm —
 * inline because the panel already sits in a sheet above another sheet.
 */
@Composable
private fun ScoreEntryContent(
    text: String,
    onTextChange: (String) -> Unit,
    existing: Double?,
    suggested: Double?,
    showAssist: Boolean,
    onAssist: () -> Unit,
    onSave: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    val focusManager = LocalFocusManager.current
    var confirmingRemove by remember { mutableStateOf(false) }
    val parsed = text.toDoubleOrNull()
    val canSave = ScoreScale.isValid(parsed)

    fun save() {
        val score = parsed?.takeIf { ScoreScale.isValid(it) } ?: return
        focusManager.clearFocus()
        haptics.performConfirm()
        // Typed input allows two decimals at most; persist exactly that.
        onSave(ScoreScale.clamp(round(score * 100) / 100))
    }

    // "We picked 7.5/10": announce Score Assist's pick for a moment above the dial.
    var showPick by remember(suggested) { mutableStateOf(suggested != null) }
    LaunchedEffect(suggested) {
        if (suggested != null) {
            delay(3_000)
            showPick = false
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = showPick && suggested != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                "We picked ${suggested?.let(ScoreScale::format)}/10",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        ScoreDial(text = text, onTextChange = onTextChange, onSave = ::save)

        if (showAssist) {
            Surface(
                onClick = onAssist,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Not sure? Compare to decide", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Button(
            onClick = ::save,
            enabled = canSave,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Save score", fontWeight = FontWeight.Bold)
        }

        if (existing != null) {
            AnimatedContent(
                targetState = confirmingRemove,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "remove-score",
            ) { confirming ->
                if (!confirming) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(
                            onClick = { confirmingRemove = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remove my score")
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Remove your score? You can always rate it again.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { confirmingRemove = false }, modifier = Modifier.weight(1f)) {
                                Text("Keep it")
                            }
                            Button(
                                onClick = {
                                    haptics.performConfirm()
                                    onRemove()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
