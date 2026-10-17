package cobresun.movieclub.app.member

import cobresun.movieclub.app.member.domain.AvatarCrop
import cobresun.movieclub.app.member.domain.CropRect
import cobresun.movieclub.app.member.domain.CropState
import cobresun.movieclub.app.member.domain.ImageSize
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from the web's cropGeometry.test.ts. */
class AvatarCropTest {
    private val landscape = ImageSize(1600, 1200)
    private val portrait = ImageSize(900, 1600)

    @Test
    fun startsAsTheLargestSquareCentredOnThePhoto() {
        assertEquals(CropRect(200.0, 0.0, 1200.0), AvatarCrop.rect(landscape, AvatarCrop.centered(landscape)))
        assertEquals(CropRect(0.0, 350.0, 900.0), AvatarCrop.rect(portrait, AvatarCrop.centered(portrait)))
    }

    @Test
    fun shrinksTheSquareAroundItsCentreAsTheZoomGrows() {
        val crop = AvatarCrop.centered(landscape).copy(zoom = 2.0)
        assertEquals(CropRect(500.0, 300.0, 600.0), AvatarCrop.rect(landscape, crop))
    }

    @Test
    fun draggingThePhotoRightShowsMoreOfItsLeftSide() {
        // A 300px preview of a 1200px square: each preview pixel is four photo pixels.
        val moved = AvatarCrop.pan(landscape, AvatarCrop.centered(landscape), 50.0, 0.0, 300.0)
        assertEquals(0.0, AvatarCrop.rect(landscape, moved).x)
    }

    @Test
    fun stopsAtThePhotosEdge() {
        val moved = AvatarCrop.pan(landscape, AvatarCrop.centered(landscape), -1000.0, 1000.0, 300.0)
        assertEquals(CropRect(400.0, 0.0, 1200.0), AvatarCrop.rect(landscape, moved))
    }

    @Test
    fun movesAShorterWayPerPreviewPixelWhenZoomedIn() {
        val zoomedIn = AvatarCrop.centered(landscape).copy(zoom = 4.0)
        val moved = AvatarCrop.pan(landscape, zoomedIn, 0.0, -30.0, 300.0)
        assertEquals(450.0 + 30, AvatarCrop.rect(landscape, moved).y)
    }

    @Test
    fun keepsTheZoomBetweenTheWholeShortSideAndTheMaximum() {
        assertEquals(1.0, AvatarCrop.clamp(landscape, AvatarCrop.centered(landscape).copy(zoom = 0.2)).zoom)
        assertEquals(AvatarCrop.MAX_ZOOM, AvatarCrop.clamp(landscape, AvatarCrop.centered(landscape).copy(zoom = 99.0)).zoom)
    }

    @Test
    fun pullsAZoomedInCornerCropBackInsideAsItZoomsOut() {
        val corner = CropState(150.0, 150.0, 4.0)
        val zoomedOut = AvatarCrop.clamp(landscape, corner.copy(zoom = 1.0))
        assertEquals(CropRect(0.0, 0.0, 1200.0), AvatarCrop.rect(landscape, zoomedOut))
    }
}
