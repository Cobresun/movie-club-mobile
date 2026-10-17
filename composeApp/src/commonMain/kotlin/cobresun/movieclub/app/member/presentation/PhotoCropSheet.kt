package cobresun.movieclub.app.member.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.member.domain.AvatarCrop
import cobresun.movieclub.app.member.domain.CropRect
import cobresun.movieclub.app.member.domain.ImageSize
import cobresun.movieclub.app.member.presentation.platform.encodeToJpeg
import kotlin.math.roundToInt

/**
 * Crops a picked photo to a circle before it's uploaded (web `PhotoCropModal`):
 * drag to reposition, pinch or slide to zoom. Shown as a full-height sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCropSheet(
    image: ImageBitmap,
    onCancel: () -> Unit,
    /** JPEG bytes of the square avatar, or null when it couldn't be encoded. */
    onSave: (ByteArray?) -> Unit,
) {
    val size = remember(image) { ImageSize(image.width, image.height) }
    var crop by remember(image) { mutableStateOf(AvatarCrop.centered(size)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        // The photo takes the drags: the sheet closes with Cancel, back, or a scrim tap.
        sheetGesturesEnabled = false,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Crop photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.scrim),
            ) {
                val latestCrop by rememberUpdatedState(crop)
                val dim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)
                val ring = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                Canvas(
                    Modifier
                        .matchParentSize()
                        .semantics { contentDescription = "Crop preview" }
                        .pointerInput(image) {
                            // One finger pans; two pinch to zoom.
                            detectTransformGestures { _, pan, zoom, _ ->
                                var next = latestCrop
                                if (zoom != 1f) next = AvatarCrop.zoomTo(size, next, next.zoom * zoom)
                                next = AvatarCrop.pan(size, next, pan.x.toDouble(), pan.y.toDouble(), this.size.width.toDouble())
                                crop = next
                            }
                        },
                ) {
                    val rect = AvatarCrop.rect(size, crop)
                    drawImage(
                        image,
                        srcOffset = IntOffset(rect.x.roundToInt(), rect.y.roundToInt()),
                        srcSize = IntSize(rect.size.roundToInt(), rect.size.roundToInt()),
                        dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
                        filterQuality = FilterQuality.High,
                    )
                    // Dims the corners the round avatar will cut off.
                    val bounds = Rect(Offset.Zero, this.size)
                    val mask = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(bounds)
                        addOval(bounds)
                    }
                    drawPath(mask, dim)
                    drawCircle(ring, radius = this.size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                }
            }
            Text(
                "Drag to reposition, pinch to zoom",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zoom", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = crop.zoom.toFloat(),
                    onValueChange = { crop = AvatarCrop.zoomTo(size, crop, it.toDouble()) },
                    valueRange = AvatarCrop.MIN_ZOOM.toFloat()..AvatarCrop.MAX_ZOOM.toFloat(),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Zoom" },
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Cancel") }
                Button(
                    onClick = { onSave(runCatching { exportCrop(image, AvatarCrop.rect(size, crop)) }.getOrNull()) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Save photo", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** The [rect] of [image], scaled to the uploaded avatar's size and encoded as JPEG. */
private fun exportCrop(image: ImageBitmap, rect: CropRect): ByteArray {
    val side = AvatarCrop.AVATAR_SIZE
    val out = ImageBitmap(side, side)
    val canvas = GraphicsCanvas(out)
    // JPEG has no transparency: transparent photos sit on black, like the preview.
    canvas.drawRect(Rect(0f, 0f, side.toFloat(), side.toFloat()), Paint().apply { color = Color.Black })
    canvas.drawImageRect(
        image,
        srcOffset = IntOffset(rect.x.roundToInt(), rect.y.roundToInt()),
        srcSize = IntSize(rect.size.roundToInt(), rect.size.roundToInt()),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(side, side),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return out.encodeToJpeg(90)
}
