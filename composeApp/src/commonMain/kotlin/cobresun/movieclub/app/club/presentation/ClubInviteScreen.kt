package cobresun.movieclub.app.club.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.core.presentation.components.AppSnackbarHost
import cobresun.movieclub.app.core.presentation.fadeUpOnAppear
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import cobresun.movieclub.app.member.presentation.platform.rememberShareSheet
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Web `clubTypeInvite(type).shareText`. */
private fun ClubType.inviteShareText() = when (this) {
    ClubType.MOVIE -> "Join my club and score the movies we watch together."
    ClubType.BOOK -> "Join my club and score the books we read together."
}

/**
 * Shown right after creating a club (web `/club/:slug/invite`): "{club} needs
 * people", with the invite link ready to copy or share before going in.
 */
@Composable
fun ClubInviteScreenRoot(
    onGoToClub: () -> Unit,
    viewModel: ClubInviteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = koinInject<HapticFeedback>()
    val share = rememberShareSheet()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DrawInCheck(Modifier.size(56.dp))
            Text(
                "${state.clubName} needs people",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fadeUpOnAppear(),
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Invite link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    when (val link = state.inviteLink) {
                        is AsyncResult.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Couldn't create an invite link.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::loadLink) { Text("Try again") }
                        }
                        else -> {
                            val value = (link as? AsyncResult.Success)?.data
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = value ?: "Creating link…",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        scope.launch {
                                            if (viewModel.copyLink()) {
                                                haptics.performConfirm()
                                                copied = true
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar("Invite link copied")
                                            } else {
                                                snackbarHostState.showSnackbar("Couldn't copy the link")
                                            }
                                        }
                                    },
                                    enabled = value != null,
                                    modifier = Modifier.size(52.dp),
                                ) {
                                    AnimatedContent(
                                        targetState = copied,
                                        transitionSpec = {
                                            (fadeIn(tween(AppMotion.BASE)) + scaleIn(tween(AppMotion.BASE), initialScale = 0.6f))
                                                .togetherWith(fadeOut(tween(AppMotion.FAST)))
                                        },
                                        label = "copy-icon",
                                    ) { done ->
                                        Icon(
                                            if (done) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = if (done) "Invite link copied" else "Copy invite link",
                                        )
                                    }
                                }
                            }
                            FilledTonalButton(
                                onClick = { value?.let { share("${state.clubType.inviteShareText()}\n$it", "Join ${state.clubName}") } },
                                enabled = value != null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share link")
                            }
                        }
                    }
                    Text(
                        "Anyone with the link can join. It stays in Club settings, so you can send it again later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onGoToClub,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Go to the club", fontWeight = FontWeight.SemiBold) }
                Text(
                    "Your first review is waiting on the other side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * A circle and check that draw themselves in (web `.draw-in`: 500ms stroke
 * each, the check starting 350ms after the circle).
 */
@Composable
private fun DrawInCheck(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val circle = remember { Animatable(if (reduced) 1f else 0f) }
    val check = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { circle.animateTo(1f, tween(500, easing = AppMotion.Emphasized)) }
        launch { check.animateTo(1f, tween(500, delayMillis = 350, easing = AppMotion.Emphasized)) }
    }
    val color = MaterialTheme.colorScheme.secondary
    Canvas(modifier.clearAndSetSemantics {}) {
        // The web SVG's 52-unit viewBox, scaled to this canvas.
        val unit = size.minDimension / 52f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * circle.value,
            useCenter = false,
            topLeft = Offset(2f * unit, 2f * unit),
            size = androidx.compose.ui.geometry.Size(48f * unit, 48f * unit),
            style = Stroke(width = 3f * unit),
        )
        val full = Path().apply {
            moveTo(15f * unit, 27f * unit)
            relativeLineTo(7f * unit, 7f * unit)
            relativeLineTo(15f * unit, -15f * unit)
        }
        val measure = PathMeasure().apply { setPath(full, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * check.value, partial, true)
        drawPath(
            partial,
            color = color,
            style = Stroke(width = 4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
