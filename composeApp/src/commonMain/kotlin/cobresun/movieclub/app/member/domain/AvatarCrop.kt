package cobresun.movieclub.app.member.domain

/**
 * Square avatar cropping (web `cropGeometry.ts`). A crop is the centre of a
 * square in source-image pixels plus a zoom, where zoom 1 is the largest square
 * the image can hold. Keeping it in image pixels means the preview and the
 * exported photo read the same rectangle however large the preview is drawn.
 */
data class CropState(val centerX: Double, val centerY: Double, val zoom: Double)

data class ImageSize(val width: Int, val height: Int)

data class CropRect(val x: Double, val y: Double, val size: Double)

object AvatarCrop {
    const val MIN_ZOOM = 1.0
    const val MAX_ZOOM = 4.0

    /** Side of the square photo that is uploaded (web `AVATAR_SIZE`). */
    const val AVATAR_SIZE = 512

    private fun cropSide(image: ImageSize, zoom: Double) = minOf(image.width, image.height) / zoom

    fun centered(image: ImageSize) = CropState(image.width / 2.0, image.height / 2.0, MIN_ZOOM)

    /** Pulls a crop back inside the image, so the square never shows past an edge. */
    fun clamp(image: ImageSize, crop: CropState): CropState {
        val zoom = crop.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val half = cropSide(image, zoom) / 2
        return CropState(
            centerX = crop.centerX.coerceIn(half, image.width - half),
            centerY = crop.centerY.coerceIn(half, image.height - half),
            zoom = zoom,
        )
    }

    /**
     * Moves the crop as though the photo were dragged [dx], [dy] pixels across a
     * preview [previewSize] pixels wide — the square travels the opposite way.
     */
    fun pan(image: ImageSize, crop: CropState, dx: Double, dy: Double, previewSize: Double): CropState {
        if (previewSize <= 0) return crop
        val imagePixelsPerPreviewPixel = cropSide(image, crop.zoom) / previewSize
        return clamp(
            image,
            crop.copy(
                centerX = crop.centerX - dx * imagePixelsPerPreviewPixel,
                centerY = crop.centerY - dy * imagePixelsPerPreviewPixel,
            ),
        )
    }

    fun zoomTo(image: ImageSize, crop: CropState, zoom: Double): CropState = clamp(image, crop.copy(zoom = zoom))

    fun rect(image: ImageSize, crop: CropState): CropRect {
        val size = cropSide(image, crop.zoom)
        return CropRect(crop.centerX - size / 2, crop.centerY - size / 2, size)
    }
}
