package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AverageIconVector: ImageVector
    get() {
        if (_Average != null) {
            return _Average!!
        }
        _Average = ImageVector.Builder(
            name = "Average",
            defaultWidth = 1111.dp,
            defaultHeight = 589.5.dp,
            viewportWidth = 1111f,
            viewportHeight = 589.5f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF9FC2F6)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(47.1f, 80.6f)
                curveToRelative(1.8f, 11.4f, 6.4f, 22.5f, 6.4f, 34f)
                curveToRelative(0.2f, 37.7f, -45.6f, 60.9f, -52.7f, 98f)
                curveToRelative(-4.2f, 22f, 6.6f, 44.5f, 21.8f, 60.9f)
                reflectiveCurveTo(57.3f, 301.8f, 75.1f, 315.3f)
                arcTo(335.7f, 335.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 185.1f, 460.8f)
                curveToRelative(4.5f, 11.6f, 8.4f, 23.6f, 9.7f, 35.9f)
                curveToRelative(0.9f, 9f, 0.5f, 18.2f, 2.6f, 27f)
                curveToRelative(8.4f, 34.9f, 50.7f, 48.4f, 86.5f, 50.7f)
                arcToRelative(379.3f, 379.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 168.7f, -28.2f)
                curveToRelative(12f, -5f, 24.4f, -10.7f, 37.3f, -9.3f)
                curveToRelative(10.3f, 1.1f, 19.5f, 6.7f, 28.5f, 11.8f)
                arcToRelative(378.4f, 378.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 84.4f, 34.7f)
                curveToRelative(16.3f, 4.5f, 33.4f, 7.9f, 50.1f, 5f)
                curveToRelative(50f, -8.8f, 76.6f, -69.5f, 125.6f, -82.7f)
                curveToRelative(33.9f, -9.2f, 69.3f, 6.8f, 99.7f, 24.4f)
                reflectiveCurveToRelative(61.9f, 37.8f, 97f, 37.9f)
                curveToRelative(46.9f, 0.1f, 85.4f, -35.4f, 117.2f, -69.9f)
                curveToRelative(6.7f, -7.2f, 13.4f, -14.8f, 16.6f, -24.1f)
                curveToRelative(6.4f, -18.9f, -4f, -40.1f, -19.5f, -52.8f)
                reflectiveCurveToRelative(-34.9f, -19f, -53.5f, -26.3f)
                arcToRelative(464.3f, 464.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -89.8f, -46.9f)
                curveToRelative(-18.2f, -12.2f, -35.8f, -26f, -48.3f, -44f)
                curveToRelative(-10.7f, -15.5f, -17.1f, -33.5f, -24.3f, -51f)
                curveToRelative(-15.7f, -38.2f, -38f, -77f, -75.2f, -94.9f)
                curveToRelative(-30.4f, -14.6f, -65.6f, -13f, -99.3f, -13.4f)
                curveToRelative(-49.6f, -0.6f, -100.5f, -6.5f, -144.6f, -29.5f)
                curveToRelative(-37.2f, -19.4f, -67.7f, -50f, -105.4f, -68.3f)
                curveToRelative(-60f, -29.1f, -128.6f, -23.4f, -193.2f, -22.3f)
                curveToRelative(-31.1f, 0.5f, -68f, 3.6f, -97.8f, -6.6f)
                curveToRelative(-22.1f, -7.5f, -33.4f, -22.9f, -58.8f, -16.7f)
                curveTo(64.7f, 9.9f, 41.6f, 45.4f, 47.1f, 80.6f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(555f, 528.9f),
                    end = Offset(555f, 67.9f)
                )
            ) {
                moveTo(966.3f, 355.5f)
                arcToRelative(25f, 25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.5f, -15.9f)
                curveToRelative(3f, -7.8f, 10.3f, -13.4f, 12.9f, -21.3f)
                curveToRelative(4.8f, -14.7f, -8.1f, -28.5f, -16f, -41.9f)
                arcToRelative(70.3f, 70.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -9.4f, -41f)
                curveToRelative(1.2f, -15.4f, 7.5f, -30f, 8f, -45.5f)
                curveToRelative(0.4f, -14.4f, -4.2f, -28.3f, -8.8f, -41.9f)
                curveToRelative(-3.8f, -11.2f, -7.7f, -22.6f, -14.1f, -32.6f)
                curveToRelative(-12.1f, -18.9f, -33.7f, -30.7f, -55.8f, -32.7f)
                curveToRelative(-10.4f, -0.9f, -20.7f, 2.5f, -30.9f, 1.1f)
                curveToRelative(-14.3f, -1.8f, -27.7f, -5.5f, -42.3f, -6f)
                curveToRelative(-13.8f, -0.4f, -29.1f, 1.3f, -38.5f, 11.5f)
                curveToRelative(-7.2f, 7.8f, -9.5f, 18.9f, -16.1f, 27.2f)
                curveToRelative(-9.3f, 11.8f, -25.7f, 15.8f, -36f, 26.7f)
                arcToRelative(37.8f, 37.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -8.5f, 36.4f)
                curveToRelative(1.4f, 4.6f, 3.8f, 8.9f, 5.3f, 13.6f)
                curveToRelative(6.4f, 19.3f, -2f, 39.9f, -7.1f, 59.5f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.3f, 5f)
                curveToRelative(0.8f, 2.4f, 3.3f, 3.7f, 5.4f, 5.1f)
                curveToRelative(6.8f, 4.7f, 10.3f, 13.1f, 11f, 21.4f)
                reflectiveCurveToRelative(-1.2f, 16.5f, -3.1f, 24.5f)
                curveToRelative(-4.8f, 20.5f, -10.8f, 42.4f, -26.6f, 56.1f)
                arcToRelative(144.3f, 144.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -10.1f, -19.8f)
                curveToRelative(-9.7f, -15.7f, -16.8f, -35f, -16.9f, -53.5f)
                arcToRelative(0.7f, 0.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -0.2f)
                curveToRelative(0f, -1f, 0.1f, -2f, 0.1f, -3f)
                curveToRelative(1.3f, -21.8f, 10.7f, -42.9f, 10.5f, -64.7f)
                quadToRelative(0f, -2.6f, -0.2f, -5.3f)
                curveToRelative(-1.2f, -18.6f, -10.2f, -35.8f, -20.4f, -51.4f)
                curveToRelative(-7.7f, -11.6f, -16.3f, -22.9f, -27.3f, -31.3f)
                curveTo(614.5f, 122.8f, 592.8f, 117.1f, 571.5f, 114.2f)
                reflectiveCurveToRelative(-40.7f, -5.8f, -62.2f, -7.2f)
                curveToRelative(-23.1f, -1.5f, -48.5f, -2.4f, -66.8f, 11.8f)
                curveToRelative(-23.8f, 18.5f, -25.1f, 54.3f, -43.3f, 78.4f)
                curveToRelative(-10.8f, 14.3f, -28f, 25.4f, -33.7f, 41.7f)
                arcToRelative(100.7f, 100.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.8f, -12.3f)
                curveToRelative(-0.1f, -0.3f, -0.1f, -0.6f, -0.2f, -0.8f)
                arcToRelative(119.2f, 119.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.6f, -61.2f)
                arcToRelative(75.3f, 75.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.7f, -10f)
                arcTo(39.4f, 39.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 365.7f, 143.8f)
                curveToRelative(-2.6f, -9.7f, -9.7f, -17.5f, -16.5f, -24.9f)
                arcToRelative(73.1f, 73.1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -8.1f, -7.8f)
                arcToRelative(118.7f, 118.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -202.4f, 118f)
                curveToRelative(-0.9f, 6.8f, -3.5f, 13.4f, -8.4f, 18.3f)
                curveToRelative(-2.2f, 2.2f, -5.1f, 4.9f, -4.6f, 7.7f)
                arcToRelative(3.8f, 3.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.3f, 2.3f)
                arcToRelative(6.6f, 6.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2.2f)
                arcTo(36f, 36f, 0f, isMoreThanHalf = false, isPositiveArc = false, 152.5f, 266.8f)
                arcTo(102.4f, 102.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 193.8f, 319.8f)
                quadToRelative(0f, 1f, -0.1f, 1.9f)
                curveToRelative(0f, 1.1f, -0.1f, 2.2f, -0.1f, 3.3f)
                curveToRelative(0f, 0.3f, 0f, 0.5f, 0f, 0.8f)
                curveToRelative(0f, -0.1f, 0f, -0.2f, 0f, -0.3f)
                reflectiveCurveToRelative(-34.8f, 45.4f, -67f, 47.1f)
                curveToRelative(-17.2f, 0.9f, -32.2f, 12.5f, -43.5f, 25.7f)
                curveToRelative(-13.6f, 15.9f, -22.1f, 34.2f, -22.8f, 39.2f)
                curveToRelative(-1.3f, 9.3f, -10.6f, 54.4f, -10.6f, 54.4f)
                lineTo(317f, 491.9f)
                curveToRelative(-3.2f, 16.2f, -7.4f, 36.9f, -7.4f, 36.9f)
                lineTo(742.3f, 528.9f)
                curveToRelative(-1f, -13.9f, -2.1f, -26.8f, -3.1f, -37.7f)
                horizontalLineToRelative(54.4f)
                lineToRelative(-0.1f, 0.3f)
                horizontalLineToRelative(251f)
                lineToRelative(15.9f, -23.9f)
                curveTo(1056.5f, 398f, 1015.4f, 359.8f, 966.3f, 355.5f)
                close()
                moveTo(341.4f, 277.8f)
                arcToRelative(64.8f, 64.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 6.5f)
                curveToRelative(2.7f, 0.6f, 5.8f, 0.9f, 8.1f, -0.6f)
                arcToRelative(9.3f, 9.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.3f, -4.6f)
                lineToRelative(0.1f, 0.3f)
                curveToRelative(6.8f, 17f, 13f, 34.5f, 14.2f, 52.8f)
                curveToRelative(0.1f, 1.7f, 0.2f, 3.4f, 0.2f, 5.2f)
                arcToRelative(74.8f, 74.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, -6.3f, 30.3f)
                arcToRelative(583.3f, 583.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -65.3f, -40f)
                lineToRelative(-1.4f, -0.6f)
                curveToRelative(0f, 0.1f, 0.5f, 1.4f, 1.1f, 3.5f)
                curveToRelative(0.1f, 0.5f, 0.3f, 1.1f, 0.4f, 1.7f)
                curveToRelative(-1.4f, -4.6f, -2.7f, -9.3f, -3.9f, -14.1f)
                curveToRelative(-0.1f, -0.6f, -0.3f, -1.1f, -0.4f, -1.7f)
                arcTo(102.5f, 102.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 341.4f, 277.8f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(193.5f, 325.5f),
                    end = Offset(193.5f, 324.8f)
                )
            ) {
                moveTo(193.5f, 325.5f)
                lineToRelative(0f, -0.6f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(193.4f, 324.8f),
                    end = Offset(193.4f, 324.2f)
                )
            ) {
                moveTo(193.4f, 324.8f)
                lineToRelative(0f, -0.7f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(193.4f, 323.8f),
                    end = Offset(193.4f, 323.8f)
                )
            ) {
                moveTo(193.4f, 323.8f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(193.4f, 324.2f),
                    end = Offset(193.4f, 324f)
                )
            ) {
                moveTo(193.4f, 324.2f)
                verticalLineToRelative(0f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0x3F808080),
                        0.5f to Color(0x1E808080),
                        1f to Color(0x19808080)
                    ),
                    start = Offset(193.4f, 324f),
                    end = Offset(193.4f, 323.8f)
                )
            ) {
                moveTo(193.4f, 324f)
                verticalLineToRelative(0f)
                close()
            }
            path(fill = SolidColor(Color(0xFFF7D6C8))) {
                moveTo(806.4f, 81.6f)
                curveToRelative(-13.6f, -0.4f, -28.6f, 1.3f, -37.9f, 11.3f)
                curveToRelative(-7.1f, 7.6f, -9.3f, 18.6f, -15.8f, 26.8f)
                curveToRelative(-9.2f, 11.6f, -25.3f, 15.5f, -35.4f, 26.2f)
                arcToRelative(37.2f, 37.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -8.4f, 35.9f)
                curveToRelative(1.4f, 4.6f, 3.7f, 8.8f, 5.2f, 13.3f)
                curveToRelative(6.3f, 19f, -1.9f, 39.2f, -7f, 58.6f)
                arcToRelative(8.8f, 8.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.3f, 4.9f)
                curveToRelative(0.8f, 2.4f, 3.3f, 3.6f, 5.3f, 5f)
                curveToRelative(6.7f, 4.6f, 10.2f, 12.9f, 10.8f, 21f)
                reflectiveCurveToRelative(-1.2f, 16.2f, -3.1f, 24.1f)
                curveToRelative(-4.9f, 21f, -11.1f, 43.6f, -28.2f, 56.8f)
                curveTo(684.4f, 371.2f, 674.8f, 375.8f, 672.9f, 384.8f)
                curveToRelative(-2.2f, 10.5f, 7.9f, 19.5f, 17.5f, 24.3f)
                curveToRelative(28.3f, 14f, 61.2f, 14.2f, 92.2f, 20.4f)
                curveToRelative(27.3f, 5.5f, 54.5f, 15.9f, 82.1f, 12.4f)
                curveToRelative(12.3f, -1.6f, 24.1f, -5.9f, 35.7f, -10.2f)
                lineToRelative(72.5f, -26.7f)
                arcToRelative(18.9f, 18.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.7f, -2.9f)
                curveToRelative(4.4f, -3.6f, 4.5f, -10.6f, 2.1f, -15.7f)
                reflectiveCurveToRelative(-6.7f, -9.1f, -10.5f, -13.4f)
                curveToRelative(-8.3f, -9.3f, -14.6f, -22.4f, -10.1f, -33.9f)
                curveToRelative(3f, -7.6f, 10.1f, -13.1f, 12.7f, -21f)
                curveToRelative(4.7f, -14.5f, -8f, -28.1f, -15.7f, -41.2f)
                arcToRelative(69.2f, 69.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -9.3f, -40.3f)
                curveToRelative(1.2f, -15.1f, 7.4f, -29.6f, 7.8f, -44.7f)
                curveToRelative(0.4f, -14.1f, -4.2f, -27.9f, -8.7f, -41.3f)
                curveToRelative(-3.7f, -11.1f, -7.5f, -22.3f, -13.8f, -32.1f)
                curveToRelative(-11.9f, -18.6f, -33.1f, -30.2f, -54.9f, -32.2f)
                curveToRelative(-10.3f, -0.9f, -20.3f, 2.4f, -30.5f, 1.1f)
                curveTo(834f, 85.6f, 820.7f, 82f, 806.4f, 81.6f)
                close()
            }
            path(fill = SolidColor(Color(0xFFEFB7B9))) {
                moveTo(826.7f, 478.4f)
                reflectiveCurveToRelative(-82.2f, -51.6f, -50.3f, -111f)
                curveToRelative(7.4f, -13.7f, 10.7f, -26.7f, 11.3f, -38.6f)
                arcToRelative(83.4f, 83.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -9.4f)
                curveToRelative(0f, -0.2f, 0f, -0.3f, 0f, -0.4f)
                quadToRelative(-0.2f, -2.4f, -0.4f, -4.8f)
                curveTo(783.5f, 282.8f, 763.4f, 262.3f, 763.4f, 262.3f)
                reflectiveCurveToRelative(136.4f, -39.8f, 113.6f, -3.9f)
                curveToRelative(-10.6f, 16.7f, -7.4f, 33f, -0.3f, 49.9f)
                curveToRelative(0.8f, 1.8f, 1.5f, 3.6f, 2.4f, 5.4f)
                curveToRelative(8.2f, 17.8f, 19.6f, 36.3f, 22.8f, 56.3f)
                curveTo(908.3f, 411.1f, 826.7f, 478.4f, 826.7f, 478.4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(876.9f, 258.4f)
                curveToRelative(-10.6f, 16.7f, -7.4f, 33f, -0.3f, 49.9f)
                arcToRelative(92.2f, 92.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -89.3f, 5.9f)
                curveTo(783.5f, 282.8f, 763.4f, 262.3f, 763.4f, 262.3f)
                reflectiveCurveTo(899.8f, 222.5f, 876.9f, 258.4f)
                close()
            }
            path(fill = SolidColor(Color(0xFFEFB7B9))) {
                moveTo(826.7f, 229f)
                moveToRelative(-92f, 0f)
                arcToRelative(92f, 92f, 0f, isMoreThanHalf = true, isPositiveArc = true, 184.1f, 0f)
                arcToRelative(92f, 92f, 0f, isMoreThanHalf = true, isPositiveArc = true, -184.1f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(787.8f, 319.4f)
                curveToRelative(0f, -0.2f, 0f, -0.3f, 0f, -0.4f)
                lineToRelative(0.1f, -0.2f)
                curveTo(787.8f, 319f, 787.8f, 319.2f, 787.8f, 319.4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(826.7f, 478.4f)
                reflectiveCurveToRelative(-82.2f, -51.6f, -50.3f, -111f)
                curveToRelative(7.4f, -13.7f, 10.7f, -26.7f, 11.3f, -38.6f)
                curveToRelative(1.9f, 14.7f, 12f, 32.9f, 30.4f, 58.2f)
                curveToRelative(8f, 11f, 11.4f, 26.4f, 12.5f, 41.8f)
                lineToRelative(1.8f, 1.3f)
                lineToRelative(9f, -5.4f)
                curveToRelative(1f, -17f, 4.4f, -34.2f, 12.6f, -46.4f)
                curveToRelative(21.4f, -32f, 26.5f, -48f, 24.9f, -64.6f)
                curveToRelative(8.2f, 17.8f, 19.6f, 36.3f, 22.8f, 56.3f)
                curveTo(908.3f, 411.1f, 826.7f, 478.4f, 826.7f, 478.4f)
                close()
            }
            path(fill = SolidColor(Color(0xFF9FC2F6))) {
                moveTo(958.5f, 354.5f)
                curveToRelative(-49f, -3.6f, -80f, -41.6f, -80f, -41.6f)
                curveToRelative(2.6f, 17.8f, -1.6f, 33.8f, -24.5f, 68f)
                curveTo(845.9f, 393.1f, 842.5f, 410.2f, 841.5f, 427.2f)
                lineToRelative(-9f, 5.4f)
                lineToRelative(-1.8f, -1.3f)
                curveToRelative(-1.1f, -15.4f, -4.5f, -30.8f, -12.5f, -41.8f)
                curveToRelative(-22.8f, -31.3f, -33f, -51.9f, -30.4f, -68.2f)
                curveToRelative(0f, 0f, -25.1f, 40.8f, -74.1f, 44.1f)
                reflectiveCurveToRelative(-90.1f, 37.9f, -94f, 101.2f)
                lineToRelative(15.7f, 21.5f)
                lineTo(789.9f, 488.2f)
                lineToRelative(-0.1f, 0.3f)
                horizontalLineToRelative(247f)
                lineTo(1052.5f, 465f)
                curveTo(1048.6f, 395.8f, 1007.5f, 358.1f, 958.5f, 354.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.05f,
                strokeAlpha = 0.05f
            ) {
                moveTo(958.5f, 354.5f)
                curveToRelative(-49f, -3.6f, -80f, -41.6f, -80f, -41.6f)
                curveToRelative(2.6f, 17.8f, -1.6f, 33.8f, -24.5f, 68f)
                curveTo(845.9f, 393.1f, 842.5f, 410.2f, 841.5f, 427.2f)
                lineToRelative(-9f, 5.4f)
                lineToRelative(-1.8f, -1.3f)
                curveToRelative(-1.1f, -15.4f, -4.5f, -30.8f, -12.5f, -41.8f)
                curveToRelative(-22.8f, -31.3f, -33f, -51.9f, -30.4f, -68.2f)
                curveToRelative(0f, 0f, -25.1f, 40.8f, -74.1f, 44.1f)
                reflectiveCurveToRelative(-90.1f, 37.9f, -94f, 101.2f)
                lineToRelative(15.7f, 21.5f)
                lineTo(789.9f, 488.2f)
                lineToRelative(-0.1f, 0.3f)
                horizontalLineToRelative(247f)
                lineTo(1052.5f, 465f)
                curveTo(1048.6f, 395.8f, 1007.5f, 358.1f, 958.5f, 354.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(888.9f, 184.6f)
                curveToRelative(-4f, 9.9f, -11.1f, 18.3f, -18.9f, 25.6f)
                curveToRelative(-10.7f, 10.1f, -24.2f, 19.1f, -38.9f, 18.4f)
                arcToRelative(59.7f, 59.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.8f, -27.6f)
                curveToRelative(-9.3f, 5.4f, -16.3f, 14f, -25.2f, 20.1f)
                reflectiveCurveToRelative(-21.4f, 9.3f, -30.1f, 2.9f)
                arcToRelative(90.3f, 90.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 37.7f, -35.6f)
                curveToRelative(-21.1f, 5.5f, -31.9f, 29.2f, -50.9f, 39.9f)
                curveToRelative(-8.9f, 5f, -19.3f, 6.9f, -29.5f, 6.7f)
                curveToRelative(-7.3f, -0.2f, -15.3f, -1.8f, -19.4f, -7.9f)
                curveToRelative(-2.4f, -3.5f, -2.9f, -7.9f, -3f, -12.1f)
                curveToRelative(-0.4f, -19.2f, 7.4f, -38f, 19.4f, -52.9f)
                reflectiveCurveToRelative(28f, -26.3f, 45.1f, -35.1f)
                curveToRelative(6.1f, -3.1f, 12.4f, -6f, 19.2f, -7f)
                curveToRelative(8.4f, -1.2f, 17f, 0.6f, 25.3f, 2.5f)
                curveToRelative(10.7f, 2.5f, 21.5f, 5.2f, 31.4f, 10f)
                curveTo(888.9f, 142.8f, 898.2f, 161.7f, 888.9f, 184.6f)
                close()
            }
            path(fill = SolidColor(Color(0xFFF7D6C8))) {
                moveTo(888.9f, 183.2f)
                curveToRelative(-4f, 9.9f, -11.1f, 18.3f, -18.9f, 25.6f)
                curveToRelative(-10.7f, 10.1f, -24.2f, 19.1f, -38.9f, 18.4f)
                arcToRelative(59.7f, 59.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.8f, -27.6f)
                curveToRelative(-9.3f, 5.4f, -16.3f, 14f, -25.2f, 20.1f)
                reflectiveCurveToRelative(-21.4f, 9.3f, -30.1f, 2.9f)
                arcToRelative(90.3f, 90.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 37.7f, -35.6f)
                curveToRelative(-21.1f, 5.5f, -31.9f, 29.2f, -50.9f, 39.9f)
                curveToRelative(-8.9f, 5f, -19.3f, 6.9f, -29.5f, 6.7f)
                curveToRelative(-7.3f, -0.2f, -15.3f, -1.8f, -19.4f, -7.9f)
                curveToRelative(-2.4f, -3.5f, -2.9f, -7.9f, -3f, -12.1f)
                curveToRelative(-0.4f, -19.2f, 7.4f, -38f, 19.4f, -52.9f)
                reflectiveCurveToRelative(28f, -26.3f, 45.1f, -35.1f)
                curveToRelative(6.1f, -3.1f, 12.4f, -6f, 19.2f, -7f)
                curveToRelative(8.4f, -1.2f, 17f, 0.6f, 25.3f, 2.5f)
                curveToRelative(10.7f, 2.5f, 21.5f, 5.2f, 31.4f, 10f)
                curveTo(888.9f, 141.4f, 898.2f, 160.4f, 888.9f, 183.2f)
                close()
            }
            path(fill = SolidColor(Color(0xFFC3705A))) {
                moveTo(254.3f, 188.3f)
                moveToRelative(-116.8f, 0f)
                arcToRelative(116.8f, 116.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 233.7f, 0f)
                arcToRelative(116.8f, 116.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, -233.7f, 0f)
            }
            path(fill = SolidColor(Color(0xFFECB4B6))) {
                moveTo(359.1f, 413.2f)
                reflectiveCurveToRelative(-107f, 27.4f, -109.7f, 30f)
                reflectiveCurveToRelative(-90.1f, 7.2f, -64.6f, -46.3f)
                curveToRelative(9.4f, -19.6f, 13.2f, -46.3f, 14.4f, -72.3f)
                curveToRelative(0.1f, -1.1f, 0.1f, -2.2f, 0.1f, -3.3f)
                curveToRelative(1.6f, -43.4f, -4.1f, -83.7f, -4.1f, -83.7f)
                reflectiveCurveToRelative(124f, -49f, 114.9f, 25.5f)
                curveToRelative(-2.3f, 18.5f, -0.4f, 37.1f, 3.8f, 54.6f)
                curveToRelative(1.4f, 5.8f, 3f, 11.6f, 4.8f, 17.1f)
                curveTo(333.2f, 379.4f, 359.1f, 413.2f, 359.1f, 413.2f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(313.9f, 317.7f)
                arcToRelative(100.5f, 100.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -114.5f, 3.6f)
                curveToRelative(1.6f, -43.4f, -4.1f, -83.7f, -4.1f, -83.7f)
                reflectiveCurveToRelative(124f, -49f, 114.9f, 25.5f)
                curveTo(307.9f, 281.6f, 309.7f, 300.2f, 313.9f, 317.7f)
                close()
            }
            path(fill = SolidColor(Color(0xFFECB4B6))) {
                moveTo(254f, 235f)
                moveToRelative(-100.5f, 0f)
                arcToRelative(100.5f, 100.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 201f, 0f)
                arcToRelative(100.5f, 100.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -201f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(359.1f, 413.2f)
                reflectiveCurveToRelative(-107f, 27.4f, -109.7f, 30f)
                reflectiveCurveToRelative(-90.1f, 7.2f, -64.6f, -46.3f)
                curveToRelative(9.4f, -19.6f, 13.2f, -46.3f, 14.4f, -72.3f)
                curveToRelative(1f, 13.5f, 8.7f, 46.3f, 9.7f, 57.2f)
                arcToRelative(9.5f, 9.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.4f, 1.9f)
                curveToRelative(1.5f, 4.8f, 6.6f, 10.4f, 12.9f, 15.7f)
                lineToRelative(0.4f, 0.3f)
                arcToRelative(47.7f, 47.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 56.1f, 3.8f)
                arcToRelative(253.2f, 253.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 25.6f, -18.4f)
                curveToRelative(11.6f, -9.6f, 15.3f, -22.6f, 15.7f, -34f)
                arcToRelative(72.8f, 72.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.3f, -16.4f)
                curveTo(333.2f, 379.4f, 359.1f, 413.2f, 359.1f, 413.2f)
                close()
            }
            path(fill = SolidColor(Color(0xFF9FC2F6))) {
                moveTo(494.2f, 488.9f)
                lineTo(57.5f, 488.9f)
                reflectiveCurveToRelative(9.1f, -44.4f, 10.4f, -53.5f)
                curveToRelative(0.7f, -5f, 9f, -23f, 22.4f, -38.6f)
                curveToRelative(11.2f, -13f, 25.9f, -24.5f, 42.8f, -25.3f)
                curveToRelative(31.6f, -1.7f, 65.9f, -46.3f, 65.9f, -46.3f)
                horizontalLineToRelative(0f)
                curveToRelative(0.8f, 13f, 8.8f, 47f, 9.8f, 58.1f)
                arcToRelative(9.5f, 9.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.4f, 1.9f)
                curveToRelative(1.5f, 4.8f, 6.6f, 10.4f, 12.9f, 15.7f)
                lineToRelative(0.4f, 0.3f)
                arcToRelative(47.7f, 47.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 56.1f, 3.8f)
                arcToRelative(254.3f, 254.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 25.6f, -18.4f)
                curveToRelative(11.6f, -9.6f, 15.3f, -22.6f, 15.7f, -34f)
                arcToRelative(75.3f, 75.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2.7f, -22.5f)
                curveToRelative(-0.6f, -2.1f, -1f, -3.3f, -1.1f, -3.4f)
                lineToRelative(1.4f, 0.6f)
                reflectiveCurveToRelative(59.1f, 43f, 113.3f, 60.6f)
                arcToRelative(64.8f, 64.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.8f, 9f)
                arcToRelative(76.8f, 76.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12.1f, 10.8f)
                arcToRelative(112.3f, 112.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 27.7f)
                curveTo(491.2f, 461.8f, 494.2f, 488.9f, 494.2f, 488.9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.05f,
                strokeAlpha = 0.05f
            ) {
                moveTo(494.2f, 488.9f)
                lineTo(57.5f, 488.9f)
                reflectiveCurveToRelative(9.1f, -44.4f, 10.4f, -53.5f)
                curveToRelative(0.7f, -5f, 9f, -23f, 22.4f, -38.6f)
                curveToRelative(11.2f, -13f, 25.9f, -24.5f, 42.8f, -25.3f)
                curveToRelative(31.6f, -1.7f, 65.9f, -46.3f, 65.9f, -46.3f)
                horizontalLineToRelative(0f)
                curveToRelative(0.8f, 13f, 8.8f, 47f, 9.8f, 58.1f)
                arcToRelative(9.5f, 9.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.4f, 1.9f)
                curveToRelative(1.5f, 4.8f, 6.6f, 10.4f, 12.9f, 15.7f)
                lineToRelative(0.4f, 0.3f)
                arcToRelative(47.7f, 47.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 56.1f, 3.8f)
                arcToRelative(254.3f, 254.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 25.6f, -18.4f)
                curveToRelative(11.6f, -9.6f, 15.3f, -22.6f, 15.7f, -34f)
                arcToRelative(75.3f, 75.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2.7f, -22.5f)
                curveToRelative(-0.6f, -2.1f, -1f, -3.3f, -1.1f, -3.4f)
                lineToRelative(1.4f, 0.6f)
                reflectiveCurveToRelative(59.1f, 43f, 113.3f, 60.6f)
                arcToRelative(64.8f, 64.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.8f, 9f)
                arcToRelative(76.8f, 76.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12.1f, 10.8f)
                arcToRelative(112.3f, 112.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 27.7f)
                curveTo(491.2f, 461.8f, 494.2f, 488.9f, 494.2f, 488.9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(302.3f, 201.2f)
                curveToRelative(2.6f, 10f, 6f, 21.1f, 14.9f, 26.3f)
                arcTo(13.1f, 13.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 320.5f, 229.8f)
                curveToRelative(2.2f, 2.4f, 1.5f, 6.1f, 1.2f, 9.4f)
                curveToRelative(-1.1f, 11.1f, 3.6f, 22.4f, 11.5f, 30.4f)
                reflectiveCurveToRelative(18.5f, 12.8f, 29.4f, 15.1f)
                curveToRelative(2.7f, 0.6f, 5.7f, 0.9f, 8f, -0.6f)
                arcToRelative(9.3f, 9.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.3f, -4.7f)
                curveToRelative(3f, -8.1f, 1.8f, -17.2f, -0.8f, -25.5f)
                reflectiveCurveToRelative(-6.3f, -16.2f, -8.2f, -24.6f)
                curveToRelative(-4.6f, -20f, 1.5f, -40.8f, 4.2f, -61.2f)
                curveToRelative(0.9f, -6.8f, 1.4f, -13.8f, -0.3f, -20.4f)
                curveToRelative(-2.5f, -9.6f, -9.5f, -17.3f, -16.3f, -24.5f)
                curveToRelative(-3.6f, -3.9f, -7.3f, -7.9f, -12f, -10.4f)
                curveToRelative(-13.7f, -7.5f, -31.4f, -0.8f, -46.1f, -6.3f)
                curveToRelative(-5.9f, -2.2f, -10.9f, -6.2f, -16.5f, -9f)
                curveToRelative(-11.9f, -6f, -25.9f, -6.3f, -39f, -3.6f)
                reflectiveCurveToRelative(-25.2f, 8.3f, -37.3f, 13.9f)
                lineTo(192.5f, 111.8f)
                curveToRelative(-13f, 6f, -26.9f, 12.8f, -33.9f, 25.3f)
                curveToRelative(-7.4f, 13.4f, -5.4f, 30.5f, -12.8f, 43.8f)
                curveToRelative(-2.3f, 4f, -5.4f, 7.8f, -6f, 12.4f)
                curveToRelative(-0.4f, 3.3f, 0.6f, 6.6f, 1.6f, 9.9f)
                curveToRelative(2.3f, 7.8f, 4.3f, 15.8f, 4.2f, 24f)
                reflectiveCurveToRelative(-2.8f, 16.5f, -8.6f, 22.3f)
                curveToRelative(-2.4f, 2.4f, -5.7f, 5.5f, -4.2f, 8.6f)
                arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2.2f)
                arcToRelative(35.4f, 35.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 33.7f, 5f)
                curveToRelative(4.4f, -1.7f, 8.5f, -4.3f, 11f, -8.3f)
                arcToRelative(32.5f, 32.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.5f, -9.3f)
                curveToRelative(3.2f, -11.8f, 6.3f, -23.7f, 6.1f, -35.9f)
                curveToRelative(-0.1f, -7f, -1.4f, -14.1f, -0.4f, -21f)
                reflectiveCurveToRelative(5.1f, -14.1f, 11.8f, -16f)
                curveToRelative(5f, -1.4f, 10.4f, 0.3f, 15.6f, 0.3f)
                curveToRelative(7.5f, -0.1f, 16.7f, -8.4f, 23.7f, -5.9f)
                curveToRelative(6f, 2.1f, 10.8f, 13.6f, 15.7f, 18f)
                curveTo(268.1f, 198.3f, 285.6f, 204.1f, 302.3f, 201.2f)
                close()
            }
            path(fill = SolidColor(Color(0xFFC3705A))) {
                moveTo(302.3f, 199.9f)
                curveToRelative(2.6f, 10f, 6f, 21.1f, 14.9f, 26.3f)
                arcToRelative(13.1f, 13.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3.2f, 2.2f)
                curveToRelative(2.2f, 2.4f, 1.5f, 6.1f, 1.2f, 9.4f)
                curveToRelative(-1.1f, 11.1f, 3.6f, 22.4f, 11.5f, 30.4f)
                reflectiveCurveToRelative(18.5f, 12.8f, 29.4f, 15.1f)
                curveToRelative(2.7f, 0.6f, 5.7f, 0.9f, 8f, -0.6f)
                arcToRelative(9.3f, 9.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.3f, -4.7f)
                curveToRelative(3f, -8.1f, 1.8f, -17.2f, -0.8f, -25.5f)
                reflectiveCurveToRelative(-6.3f, -16.2f, -8.2f, -24.6f)
                curveToRelative(-4.6f, -20f, 1.5f, -40.8f, 4.2f, -61.2f)
                curveToRelative(0.9f, -6.8f, 1.4f, -13.8f, -0.3f, -20.4f)
                curveToRelative(-2.5f, -9.6f, -9.5f, -17.3f, -16.3f, -24.5f)
                curveToRelative(-3.6f, -3.9f, -7.3f, -7.9f, -12f, -10.4f)
                curveToRelative(-13.7f, -7.5f, -31.4f, -0.8f, -46.1f, -6.3f)
                curveToRelative(-5.9f, -2.2f, -10.9f, -6.2f, -16.5f, -9f)
                curveToRelative(-11.9f, -6f, -25.9f, -6.3f, -39f, -3.6f)
                reflectiveCurveToRelative(-25.2f, 8.3f, -37.3f, 13.9f)
                lineToRelative(-9.1f, 4.2f)
                curveToRelative(-13f, 6f, -26.9f, 12.8f, -33.9f, 25.3f)
                curveToRelative(-7.4f, 13.4f, -5.4f, 30.5f, -12.8f, 43.8f)
                curveToRelative(-2.3f, 4f, -5.4f, 7.8f, -6f, 12.4f)
                curveToRelative(-0.4f, 3.3f, 0.6f, 6.6f, 1.6f, 9.9f)
                curveToRelative(2.3f, 7.8f, 4.3f, 15.8f, 4.2f, 24f)
                reflectiveCurveToRelative(-2.8f, 16.5f, -8.6f, 22.3f)
                curveToRelative(-2.4f, 2.4f, -5.7f, 5.5f, -4.2f, 8.6f)
                arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2.2f)
                arcToRelative(35.4f, 35.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 33.7f, 5f)
                curveToRelative(4.4f, -1.7f, 8.5f, -4.3f, 11f, -8.3f)
                arcToRelative(32.5f, 32.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.5f, -9.3f)
                curveToRelative(3.2f, -11.8f, 6.3f, -23.7f, 6.1f, -35.9f)
                curveToRelative(-0.1f, -7f, -1.4f, -14.1f, -0.4f, -21f)
                reflectiveCurveToRelative(5.1f, -14.1f, 11.8f, -16f)
                curveToRelative(5f, -1.4f, 10.4f, 0.3f, 15.6f, 0.3f)
                curveToRelative(7.5f, -0.1f, 16.7f, -8.4f, 23.7f, -5.9f)
                curveToRelative(6f, 2.1f, 10.8f, 13.6f, 15.7f, 18f)
                curveTo(268.1f, 197f, 285.6f, 202.8f, 302.3f, 199.9f)
                close()
            }
            path(fill = SolidColor(Color(0xFF463A3E))) {
                moveTo(408f, 242.6f)
                horizontalLineToRelative(229.1f)
                verticalLineToRelative(196.5f)
                horizontalLineToRelative(-229.1f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.05f,
                strokeAlpha = 0.05f
            ) {
                moveTo(408f, 242.6f)
                horizontalLineToRelative(229.1f)
                verticalLineToRelative(196.5f)
                horizontalLineToRelative(-229.1f)
                close()
            }
            path(fill = SolidColor(Color(0xFFC17174))) {
                moveTo(604.3f, 525.2f)
                horizontalLineToRelative(-196f)
                curveToRelative(2.5f, -14.3f, 9.9f, -29.9f, 24.4f, -46.8f)
                arcToRelative(173f, 173f, 0f, isMoreThanHalf = false, isPositiveArc = false, 27.6f, -44.2f)
                quadToRelative(2.8f, -6.4f, 5f, -12.9f)
                arcToRelative(188.8f, 188.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8.8f, -39.4f)
                curveToRelative(0f, -0.4f, 0.1f, -0.8f, 0.1f, -1.1f)
                arcToRelative(215.4f, 215.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.2f, -27.9f)
                curveToRelative(0f, -0.7f, 0f, -1.3f, 0f, -2f)
                arcToRelative(254.4f, 254.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -11.3f, -67.4f)
                lineToRelative(-0.3f, -0.8f)
                curveToRelative(-2.1f, -6.7f, -3.7f, -10.5f, -3.7f, -10.5f)
                reflectiveCurveToRelative(3.9f, -2.1f, 10.3f, -5.2f)
                lineToRelative(0.5f, -0.3f)
                curveToRelative(23.6f, -11.3f, 79.5f, -35f, 104.1f, -18.1f)
                lineToRelative(0.4f, 0.3f)
                curveToRelative(8.6f, 6.1f, 13.3f, 17.4f, 11.1f, 36.2f)
                curveToRelative(0f, 0.1f, 0f, 0.2f, 0f, 0.2f)
                curveToRelative(0f, 0.3f, -0.1f, 0.7f, -0.1f, 1f)
                arcToRelative(363.2f, 363.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.9f, 38.9f)
                curveToRelative(0f, 0.6f, 0f, 1.3f, 0f, 2f)
                quadToRelative(0f, 4.6f, 0.1f, 9.3f)
                curveToRelative(0f, 0.4f, 0f, 0.8f, 0f, 1.3f)
                curveToRelative(0.2f, 10.1f, 0.6f, 20.5f, 1.3f, 31.1f)
                curveToRelative(0.9f, 14.7f, 2.1f, 29.6f, 3.5f, 44.3f)
                curveToRelative(0.6f, 6.3f, 1.3f, 12.5f, 2f, 18.6f)
                curveToRelative(0.1f, 0.7f, 0.2f, 1.5f, 0.2f, 2.2f)
                curveToRelative(0.8f, 7.5f, 1.7f, 14.8f, 2.6f, 21.9f)
                lineToRelative(0.1f, 0.5f)
                curveTo(597.9f, 484.4f, 601.7f, 509.1f, 604.3f, 525.2f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(586.6f, 285.1f)
                curveToRelative(0f, 0.1f, 0f, 0.2f, 0f, 0.2f)
                arcToRelative(359.7f, 359.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 40f)
                curveToRelative(0f, 0.6f, 0f, 1.3f, 0f, 2f)
                arcToRelative(95.4f, 95.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -109.3f, 25.7f)
                curveToRelative(0f, -0.7f, 0f, -1.3f, 0f, -2f)
                arcToRelative(253.9f, 253.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -11.5f, -68.2f)
                curveToRelative(-2.1f, -6.7f, -3.7f, -10.5f, -3.7f, -10.5f)
                reflectiveCurveToRelative(3.9f, -2.1f, 10.3f, -5.2f)
                curveTo(493.8f, 255.8f, 550.9f, 231.4f, 575.5f, 248.9f)
                curveTo(584.1f, 255f, 588.8f, 266.3f, 586.6f, 285.1f)
                close()
            }
            path(fill = SolidColor(Color(0xFFC17174))) {
                moveTo(512.3f, 263.1f)
                moveToRelative(-95.3f, 0f)
                arcToRelative(95.3f, 95.3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 190.6f, 0f)
                arcToRelative(95.3f, 95.3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -190.6f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(517.4f, 524.6f)
                lineTo(314.1f, 524.6f)
                reflectiveCurveToRelative(9.1f, -44.4f, 10.4f, -53.5f)
                curveToRelative(0.7f, -5f, 9f, -23f, 22.4f, -38.6f)
                arcToRelative(98.7f, 98.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 60.7f, -21.5f)
                curveToRelative(38.9f, -0.6f, 50.2f, 12.8f, 53.3f, 22.7f)
                arcToRelative(22.2f, 22.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.8f, 10.3f)
                lineToRelative(7.4f, 10.7f)
                close()
            }
            path(fill = SolidColor(Color(0xFF9FC2F6))) {
                moveTo(516.8f, 525.2f)
                lineTo(313.4f, 525.2f)
                reflectiveCurveToRelative(9.1f, -44.4f, 10.4f, -53.5f)
                curveToRelative(0.7f, -5f, 9f, -23f, 22.4f, -38.6f)
                arcToRelative(98.7f, 98.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 60.7f, -21.5f)
                curveToRelative(38.9f, -0.6f, 50.2f, 12.8f, 53.3f, 22.7f)
                arcToRelative(22.2f, 22.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.8f, 10.3f)
                lineToRelative(7.4f, 10.7f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(738.7f, 524.6f)
                lineTo(525.7f, 524.6f)
                curveToRelative(10.2f, -21.6f, 36.8f, -79.3f, 38.3f, -92.6f)
                curveToRelative(0.6f, -5.2f, 6.1f, -10f, 13.4f, -14.1f)
                arcToRelative(75.9f, 75.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.1f, -3.6f)
                lineToRelative(0.4f, -0.2f)
                quadToRelative(2f, -0.9f, 4.1f, -1.6f)
                arcToRelative(81.2f, 81.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 57f, 0.4f)
                curveToRelative(17.5f, 6.7f, 40f, 15.6f, 57.5f, 24f)
                lineToRelative(0.1f, 0.1f)
                curveToRelative(4.5f, 2.2f, 8.7f, 4.3f, 12.4f, 6.3f)
                curveToRelative(7.4f, 4.1f, 12.8f, 7.8f, 14.8f, 10.7f)
                curveToRelative(0.9f, 1.4f, 2f, 7.5f, 3.2f, 17f)
                curveTo(735.4f, 483.9f, 737.1f, 502.9f, 738.7f, 524.6f)
                close()
            }
            path(fill = SolidColor(Color(0xFF9FC2F6))) {
                moveTo(739.4f, 525.2f)
                lineTo(526.3f, 525.2f)
                curveToRelative(10.2f, -21.6f, 36.8f, -79.3f, 38.3f, -92.6f)
                curveToRelative(0.6f, -5.2f, 6.1f, -10f, 13.4f, -14.1f)
                arcToRelative(75.9f, 75.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.1f, -3.6f)
                lineToRelative(0.4f, -0.2f)
                quadToRelative(2f, -0.9f, 4.1f, -1.6f)
                arcToRelative(81.2f, 81.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 57f, 0.4f)
                curveToRelative(17.5f, 6.7f, 40f, 15.6f, 57.5f, 24f)
                lineToRelative(0.1f, 0.1f)
                curveToRelative(4.5f, 2.2f, 8.7f, 4.3f, 12.4f, 6.3f)
                curveToRelative(7.4f, 4.1f, 12.8f, 7.8f, 14.8f, 10.7f)
                curveToRelative(0.9f, 1.4f, 2f, 7.5f, 3.2f, 17f)
                curveTo(736.1f, 484.5f, 737.8f, 503.6f, 739.4f, 525.2f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.1f,
                strokeAlpha = 0.1f
            ) {
                moveTo(705.1f, 420.7f)
                quadToRelative(0f, 4.6f, -0.3f, 9.2f)
                quadToRelative(-0.1f, 2.3f, -0.3f, 4.5f)
                curveToRelative(-0.1f, 1.1f, -0.2f, 2.2f, -0.3f, 3.2f)
                lineToRelative(-0.1f, 1.2f)
                curveToRelative(-1.6f, 14.5f, -4.6f, 29f, -5f, 43.6f)
                curveToRelative(0f, 0.7f, 0f, 1.4f, 0f, 2.1f)
                curveToRelative(-0.1f, 13.7f, 2.2f, 27.2f, 5.2f, 40.7f)
                lineTo(581.5f, 525.2f)
                curveToRelative(-0.2f, -2.5f, -0.3f, -5f, -0.3f, -7.5f)
                curveToRelative(0f, -2.2f, 0.1f, -4.5f, 0.4f, -6.7f)
                curveToRelative(1.9f, -18.4f, 11.3f, -35.6f, 12.7f, -53.9f)
                lineToRelative(0.1f, -0.8f)
                curveToRelative(0.1f, -1.4f, 0.1f, -2.8f, 0.1f, -4.1f)
                curveToRelative(0f, -0.1f, 0f, -0.2f, 0f, -0.4f)
                arcToRelative(70.7f, 70.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2.8f, -17.9f)
                curveToRelative(-0.1f, -0.3f, -0.2f, -0.6f, -0.2f, -0.9f)
                arcToRelative(179.9f, 179.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -6f, -17f)
                lineToRelative(-0.4f, -1.1f)
                curveToRelative(-3.3f, -8.3f, -6.8f, -16.6f, -9.2f, -24.8f)
                arcToRelative(66.1f, 66.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2.9f, -19f)
                curveToRelative(0f, -1.1f, 0.1f, -2.3f, 0.2f, -3.5f)
                curveToRelative(0.1f, -0.9f, 0.2f, -1.9f, 0.3f, -2.8f)
                arcToRelative(44.3f, 44.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.9f, -4.4f)
                curveToRelative(2f, -7.9f, 6.3f, -15.3f, 10.4f, -22.6f)
                lineToRelative(0f, 0f)
                curveToRelative(4.6f, -8.1f, 8.9f, -16.2f, 9.7f, -24.8f)
                arcToRelative(28.6f, 28.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.1f, -3.3f)
                verticalLineToRelative(0f)
                arcToRelative(28.7f, 28.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.4f, -4.2f)
                curveToRelative(-1f, -6.1f, -4.2f, -11.6f, -6.8f, -17.2f)
                curveToRelative(-0.3f, -0.6f, -0.5f, -1.1f, -0.8f, -1.7f)
                curveToRelative(0f, -0.1f, -0.1f, -0.2f, -0.1f, -0.3f)
                curveToRelative(-5f, -11.6f, -7.9f, -23.9f, -10.9f, -36.1f)
                curveToRelative(-0.1f, -0.5f, -0.3f, -1.1f, -0.4f, -1.6f)
                curveToRelative(-0.8f, -3f, -1.5f, -6f, -2.3f, -9f)
                curveToRelative(-4.5f, -16.4f, -10.9f, -32.9f, -23f, -44.7f)
                reflectiveCurveTo(518.3f, 177.3f, 503.1f, 184.8f)
                curveToRelative(-13.1f, 6.6f, -20.3f, 21.2f, -23.5f, 35.5f)
                reflectiveCurveToRelative(-3.5f, 29.3f, -7.6f, 43.3f)
                quadToRelative(-0.4f, 1.5f, -1f, 3.1f)
                curveToRelative(-0.2f, 0.5f, -0.4f, 1f, -0.5f, 1.6f)
                curveToRelative(-1.8f, 5.1f, -4f, 10.1f, -6.4f, 15.2f)
                lineToRelative(-0.3f, 0.5f)
                curveToRelative(-7.6f, 16.2f, -16.4f, 32.1f, -16.7f, 48.3f)
                verticalLineToRelative(0.2f)
                curveToRelative(0f, 0.4f, 0f, 0.9f, 0f, 1.3f)
                arcToRelative(41.9f, 41.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.7f, 11.4f)
                curveToRelative(2f, 6.7f, 5.6f, 12.5f, 9.8f, 18f)
                quadToRelative(1.3f, 1.6f, 2.6f, 3.2f)
                curveToRelative(4.2f, 5.2f, 8.8f, 10.2f, 12.8f, 15.5f)
                lineToRelative(0.1f, 0.2f)
                arcToRelative(63.3f, 63.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.6f, 12.7f)
                arcToRelative(46.5f, 46.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.2f, 35.9f)
                arcToRelative(67.4f, 67.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.9f, 7.7f)
                curveToRelative(-2.6f, 4.4f, -5.4f, 8.8f, -7.6f, 13.4f)
                curveToRelative(-0.8f, 1.5f, -1.4f, 3.1f, -2f, 4.7f)
                curveToRelative(-5f, 12.9f, -6.1f, 27.1f, -10.2f, 40.4f)
                arcToRelative(91.8f, 91.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, -14.5f, 28.3f)
                horizontalLineToRelative(-130f)
                curveToRelative(1.5f, -7.4f, 9f, -44f, 10.2f, -52.2f)
                curveToRelative(0.7f, -5f, 9f, -23f, 22.4f, -38.6f)
                curveToRelative(-1.1f, 0.9f, -2.2f, 1.8f, -3.3f, 2.7f)
                arcToRelative(111.2f, 111.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -21.8f, 25.1f)
                curveToRelative(0.3f, -0.9f, 0.6f, -1.8f, 0.9f, -2.7f)
                curveToRelative(1.8f, -5.5f, 3.8f, -10.9f, 5.8f, -16.2f)
                curveToRelative(5.8f, -15.4f, 12.3f, -31.1f, 23.9f, -42.7f)
                curveToRelative(6.6f, -6.6f, 14.5f, -11.6f, 20.8f, -18.5f)
                curveToRelative(11f, -12.1f, 15.6f, -28.8f, 15.4f, -45.3f)
                curveToRelative(0f, -1.7f, -0.1f, -3.4f, -0.2f, -5.1f)
                curveToRelative(-1.2f, -18f, -7.3f, -35.2f, -14f, -52f)
                curveToRelative(-3.6f, -9f, -7.4f, -18.5f, -7.2f, -28f)
                arcToRelative(31.2f, 31.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, -4f)
                curveToRelative(2.8f, -19.8f, 22.8f, -31.6f, 34.8f, -47.6f)
                curveToRelative(17.9f, -23.7f, 19.2f, -59f, 42.6f, -77.2f)
                curveToRelative(18f, -14f, 43f, -13.1f, 65.7f, -11.6f)
                curveToRelative(21.1f, 1.4f, 40.2f, 4.2f, 61.2f, 7.1f)
                reflectiveCurveTo(613.5f, 126.8f, 630.3f, 139.7f)
                curveToRelative(10.9f, 8.3f, 19.4f, 19.4f, 26.9f, 30.8f)
                curveToRelative(10.1f, 15.3f, 18.9f, 32.3f, 20.1f, 50.6f)
                curveToRelative(0.1f, 1.3f, 0.1f, 2.6f, 0.1f, 3.9f)
                curveToRelative(0.2f, 21.4f, -9.1f, 42.1f, -10.3f, 63.6f)
                curveToRelative(-0.1f, 1f, -0.1f, 2f, -0.1f, 3f)
                arcToRelative(0.7f, 0.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 0.2f)
                curveToRelative(-0.2f, 18.6f, 6.9f, 38.2f, 16.7f, 54f)
                arcTo(144.8f, 144.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 705.1f, 420.7f)
                close()
            }
            path(fill = SolidColor(Color(0xFF463A3E))) {
                moveTo(705.1f, 420.7f)
                curveToRelative(0f, 2.6f, -0.1f, 5.3f, -0.3f, 7.9f)
                quadToRelative(-0.1f, 2.3f, -0.3f, 4.5f)
                reflectiveQuadToRelative(-0.4f, 4.5f)
                curveToRelative(-1.6f, 14.5f, -4.6f, 29f, -5f, 43.6f)
                curveToRelative(0f, 1.1f, 0f, 2.3f, 0f, 3.4f)
                curveToRelative(0.1f, 13.7f, 2.4f, 27.2f, 5.4f, 40.7f)
                lineTo(581.6f, 525.2f)
                curveToRelative(-0.2f, -2.5f, -0.4f, -5f, -0.4f, -7.5f)
                arcToRelative(71.5f, 71.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.4f, -8f)
                curveToRelative(1.9f, -18.4f, 11.3f, -35.6f, 12.7f, -53.9f)
                curveToRelative(0.1f, -1.3f, 0.2f, -2.7f, 0.2f, -4f)
                curveToRelative(0f, -0.3f, 0f, -0.6f, 0f, -0.9f)
                arcTo(71.5f, 71.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 591.5f, 431.8f)
                arcToRelative(179.9f, 179.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -6f, -17f)
                curveToRelative(-3.5f, -8.7f, -7.2f, -17.3f, -9.6f, -26f)
                arcToRelative(67.6f, 67.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2.9f, -17.7f)
                arcToRelative(46.5f, 46.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.2f, -4.8f)
                curveToRelative(0.1f, -0.9f, 0.2f, -1.9f, 0.3f, -2.8f)
                arcToRelative(44.3f, 44.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.9f, -4.4f)
                curveToRelative(2f, -7.9f, 6.3f, -15.3f, 10.4f, -22.6f)
                curveToRelative(4.6f, -8.2f, 8.9f, -16.2f, 9.7f, -24.8f)
                curveToRelative(0.1f, -0.6f, 0.1f, -1.3f, 0.1f, -2f)
                verticalLineToRelative(0f)
                arcToRelative(28f, 28f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.4f, -5.5f)
                curveToRelative(-1f, -6.1f, -4.2f, -11.6f, -6.8f, -17.2f)
                curveToRelative(-0.3f, -0.6f, -0.5f, -1.1f, -0.8f, -1.7f)
                curveToRelative(-5.1f, -11.6f, -8f, -24f, -11.1f, -36.4f)
                curveToRelative(-0.9f, -3.5f, -1.8f, -7.1f, -2.7f, -10.6f)
                curveToRelative(-4.5f, -16.4f, -10.9f, -32.9f, -23f, -44.7f)
                reflectiveCurveToRelative(-31.5f, -17.7f, -46.7f, -10.1f)
                curveToRelative(-13.1f, 6.6f, -20.3f, 21.2f, -23.5f, 35.5f)
                reflectiveCurveToRelative(-3.5f, 29.3f, -7.6f, 43.3f)
                curveToRelative(-0.5f, 1.5f, -1f, 3.1f, -1.5f, 4.6f)
                curveToRelative(-1.9f, 5.3f, -4.2f, 10.5f, -6.6f, 15.7f)
                curveToRelative(-7.8f, 16.6f, -16.9f, 32.9f, -16.7f, 49.6f)
                verticalLineToRelative(0.2f)
                arcToRelative(41.9f, 41.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.7f, 11.4f)
                curveToRelative(2f, 6.7f, 5.6f, 12.5f, 9.8f, 18f)
                quadToRelative(1.3f, 1.6f, 2.6f, 3.2f)
                curveToRelative(4.3f, 5.2f, 8.9f, 10.3f, 12.9f, 15.6f)
                arcToRelative(63.3f, 63.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.6f, 12.7f)
                arcToRelative(46.5f, 46.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.2f, 35.9f)
                arcToRelative(67.4f, 67.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.9f, 7.7f)
                curveToRelative(-2.6f, 4.4f, -5.4f, 8.8f, -7.6f, 13.4f)
                curveToRelative(-0.8f, 1.5f, -1.4f, 3.1f, -2f, 4.7f)
                curveToRelative(-5f, 12.9f, -6.1f, 27.1f, -10.2f, 40.4f)
                arcToRelative(92f, 92f, 0f, isMoreThanHalf = false, isPositiveArc = true, -15.5f, 29.6f)
                lineTo(313.4f, 525.2f)
                reflectiveCurveToRelative(9.1f, -44.4f, 10.4f, -53.5f)
                curveToRelative(0.6f, -4.6f, 7.7f, -20f, 19.2f, -34.6f)
                curveToRelative(1f, -1.3f, 2.1f, -2.7f, 3.3f, -4f)
                arcToRelative(112.1f, 112.1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -24.1f, 26.3f)
                lineToRelative(-0.9f, 1.4f)
                curveToRelative(2.1f, -6.4f, 4.4f, -12.7f, 6.7f, -18.9f)
                curveToRelative(5.8f, -15.4f, 12.3f, -31.1f, 23.9f, -42.7f)
                curveToRelative(6.6f, -6.6f, 14.5f, -11.6f, 20.8f, -18.5f)
                curveToRelative(10.7f, -11.8f, 15.3f, -28f, 15.4f, -44f)
                curveToRelative(0f, -2.1f, 0f, -4.3f, -0.2f, -6.4f)
                curveToRelative(-1.2f, -18f, -7.3f, -35.2f, -14f, -52f)
                curveToRelative(-3.4f, -8.6f, -7f, -17.6f, -7.2f, -26.7f)
                arcToRelative(32.5f, 32.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, -5.3f)
                curveTo(369.7f, 226.6f, 389.7f, 214.8f, 401.7f, 198.8f)
                curveToRelative(17.9f, -23.7f, 19.2f, -59f, 42.6f, -77.2f)
                curveToRelative(18f, -14f, 43f, -13.1f, 65.7f, -11.6f)
                curveToRelative(21.1f, 1.4f, 40.2f, 4.2f, 61.2f, 7.1f)
                reflectiveCurveToRelative(42.3f, 8.4f, 59.1f, 21.3f)
                curveTo(641.2f, 146.8f, 649.7f, 157.8f, 657.2f, 169.2f)
                curveToRelative(10.1f, 15.3f, 18.9f, 32.3f, 20.1f, 50.6f)
                quadToRelative(0.2f, 2.6f, 0.1f, 5.2f)
                curveToRelative(-0.2f, 20.9f, -9.1f, 41.3f, -10.3f, 62.3f)
                curveToRelative(-0.1f, 1.4f, -0.1f, 2.8f, -0.1f, 4.3f)
                arcToRelative(0.7f, 0.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 0.2f)
                curveToRelative(0.1f, 18.2f, 7.2f, 37.3f, 16.7f, 52.7f)
                arcTo(144.8f, 144.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 705.1f, 420.7f)
                close()
            }
        }.build()

        return _Average!!
    }

@Suppress("ObjectPropertyName")
private var _Average: ImageVector? = null
