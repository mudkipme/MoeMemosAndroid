package me.mudkip.moememos.ui.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun VectorPreview() {
    Image(MemosIcon, null)
}

private var memosIcon: ImageVector? = null

val MemosIcon: ImageVector
get() {
    if (memosIcon != null) {
        return memosIcon!!
    }
    memosIcon = ImageVector.Builder(
        name = "MemosIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 784f,
        viewportHeight = 784f
    ).apply {
        group(
            scaleX = 0.1f,
            scaleY = -0.1f,
            translationX = 0f,
            translationY = 784f,
            pivotX = 0f,
            pivotY = 0f,
        ) {
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(0f, 3920f)
                lineToRelative(0f, -3920f)
                lineToRelative(3920f, 0f)
                lineToRelative(3920f, 0f)
                lineToRelative(0f, 3920f)
                lineToRelative(0f, 3920f)
                lineToRelative(-3920f, 0f)
                lineToRelative(-3920f, 0f)
                lineToRelative(0f, -3920f)
                close()
                moveToRelative(3365f, 3443f)
                curveToRelative(113f, -57f, 165f, -137f, 196f, -302f)
                curveToRelative(40f, -208f, 129f, -367f, 313f, -559f)
                curveToRelative(139f, -145f, 233f, -205f, 651f, -415f)
                curveToRelative(594f, -299f, 846f, -465f, 1085f, -713f)
                curveToRelative(278f, -289f, 427f, -627f, 446f, -1014f)
                curveToRelative(9f, -166f, -16f, -334f, -82f, -566f)
                curveToRelative(-5f, -18f, 3f, -26f, 60f, -52f)
                curveToRelative(200f, -93f, 299f, -314f, 241f, -536f)
                curveToRelative(-24f, -91f, -44f, -126f, -113f, -195f)
                curveToRelative(-73f, -74f, -73f, -60f, -8f, -207f)
                curveToRelative(91f, -205f, 126f, -483f, 90f, -721f)
                curveToRelative(-22f, -154f, -57f, -263f, -129f, -408f)
                curveToRelative(-224f, -451f, -639f, -739f, -1120f, -775f)
                curveToRelative(-66f, -5f, -266f, -10f, -444f, -10f)
                lineToRelative(-323f, 0f)
                lineToRelative(-63f, -57f)
                curveToRelative(-146f, -136f, -331f, -203f, -560f, -203f)
                curveToRelative(-230f, 0f, -425f, 71f, -563f, 204f)
                lineToRelative(-53f, 51f)
                lineToRelative(-157f, 6f)
                curveToRelative(-128f, 6f, -173f, 12f, -247f, 33f)
                curveToRelative(-537f, 160f, -951f, 681f, -1065f, 1341f)
                curveToRelative(-45f, 261f, -47f, 493f, -9f, 912f)
                curveToRelative(27f, 308f, 82f, 666f, 145f, 947f)
                lineToRelative(35f, 159f)
                lineToRelative(-38f, 75f)
                curveToRelative(-37f, 72f, -38f, 79f, -38f, 177f)
                curveToRelative(0f, 97f, 2f, 104f, 38f, 177f)
                curveToRelative(51f, 104f, 122f, 169f, 269f, 243f)
                curveToRelative(105f, 53f, 223f, 130f, 325f, 212f)
                lineToRelative(42f, 33f)
                lineToRelative(-31f, 72f)
                curveToRelative(-59f, 141f, -81f, 247f, -82f, 408f)
                curveToRelative(0f, 151f, 8f, 200f, 54f, 323f)
                curveToRelative(46f, 123f, 162f, 232f, 286f, 268f)
                curveToRelative(49f, 15f, 64f, 35f, 64f, 87f)
                curveToRelative(0f, 54f, 78f, 341f, 125f, 457f)
                curveToRelative(145f, 365f, 284f, 539f, 458f, 575f)
                curveToRelative(61f, 13f, 145f, 1f, 202f, -27f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3194f, 7113f)
                curveToRelative(-12f, -2f, -33f, -16f, -47f, -31f)
                curveToRelative(-33f, -35f, -154f, -279f, -201f, -405f)
                curveToRelative(-102f, -273f, -132f, -580f, -78f, -800f)
                lineToRelative(19f, -78f)
                lineToRelative(-21f, 7f)
                curveToRelative(-40f, 13f, -131f, 84f, -184f, 144f)
                curveToRelative(-43f, 48f, -59f, 60f, -83f, 60f)
                curveToRelative(-104f, 0f, -143f, -90f, -143f, -325f)
                curveToRelative(0f, -137f, 2f, -157f, 28f, -235f)
                curveToRelative(33f, -99f, 73f, -183f, 125f, -260f)
                curveToRelative(21f, -30f, 40f, -60f, 44f, -67f)
                curveToRelative(5f, -7f, -24f, -34f, -80f, -72f)
                curveToRelative(-49f, -33f, -140f, -102f, -203f, -152f)
                curveToRelative(-142f, -113f, -244f, -181f, -335f, -224f)
                curveToRelative(-38f, -17f, -82f, -44f, -97f, -60f)
                curveToRelative(-66f, -66f, -39f, -165f, 65f, -240f)
                lineToRelative(27f, -20f)
                lineToRelative(-35f, -97f)
                curveToRelative(-82f, -230f, -157f, -626f, -207f, -1093f)
                curveToRelative(-17f, -161f, -17f, -672f, 0f, -785f)
                curveToRelative(54f, -352f, 206f, -671f, 425f, -892f)
                curveToRelative(43f, -44f, 108f, -99f, 145f, -122f)
                curveToRelative(67f, -44f, 141f, -80f, 155f, -78f)
                curveToRelative(4f, 1f, 7f, -2f, 7f, -7f)
                curveToRelative(0f, -6f, 9f, -8f, 20f, -6f)
                curveToRelative(13f, 3f, 20f, -2f, 21f, -13f)
                curveToRelative(0f, -14f, 2f, -13f, 9f, 3f)
                curveToRelative(5f, 11f, 9f, 15f, 9f, 9f)
                curveToRelative(1f, -7f, 9f, -14f, 19f, -18f)
                curveToRelative(9f, -3f, 27f, -10f, 40f, -16f)
                curveToRelative(15f, -6f, 21f, -6f, 17f, 0f)
                curveToRelative(-3f, 6f, 1f, 7f, 9f, 4f)
                curveToRelative(9f, -3f, 16f, -10f, 16f, -14f)
                curveToRelative(0f, -4f, 8f, -10f, 19f, -13f)
                curveToRelative(14f, -3f, 17f, 0f, 14f, 14f)
                curveToRelative(-3f, 10f, -9f, 16f, -14f, 13f)
                curveToRelative(-9f, -5f, -12f, 7f, -10f, 36f)
                curveToRelative(0f, 11f, -1f, 11f, -5f, 2f)
                curveToRelative(-7f, -16f, -34f, 3f, -34f, 25f)
                curveToRelative(0f, 8f, -4f, 12f, -8f, 9f)
                curveToRelative(-9f, -5f, -57f, 42f, -84f, 83f)
                curveToRelative(-10f, 14f, -18f, 22f, -18f, 18f)
                curveToRelative(0f, -5f, -16f, 10f, -35f, 32f)
                curveToRelative(-21f, 26f, -32f, 48f, -29f, 59f)
                curveToRelative(5f, 14f, 4f, 14f, -4f, 3f)
                curveToRelative(-9f, -12f, -13f, -12f, -27f, 2f)
                curveToRelative(-9f, 9f, -13f, 21f, -10f, 27f)
                curveToRelative(4f, 6f, 1f, 9f, -6f, 8f)
                curveToRelative(-7f, -2f, -13f, 2f, -14f, 7f)
                curveToRelative(0f, 6f, -17f, 28f, -38f, 50f)
                curveToRelative(-20f, 21f, -37f, 42f, -37f, 46f)
                curveToRelative(0f, 4f, -6f, 14f, -13f, 21f)
                curveToRelative(-20f, 20f, -96f, 134f, -99f, 149f)
                curveToRelative(-2f, 7f, -10f, 21f, -19f, 31f)
                curveToRelative(-21f, 25f, -90f, 166f, -92f, 190f)
                curveToRelative(-1f, 10f, -4f, 18f, -8f, 18f)
                curveToRelative(-11f, 0f, -50f, 137f, -43f, 156f)
                curveToRelative(4f, 14f, 3f, 15f, -4f, 6f)
                curveToRelative(-9f, -14f, -44f, 142f, -57f, 263f)
                curveToRelative(-12f, 103f, -15f, 405f, -4f, 405f)
                curveToRelative(6f, 0f, 7f, 5f, 4f, 11f)
                curveToRelative(-9f, 14f, 5f, 290f, 14f, 284f)
                curveToRelative(4f, -2f, 6f, 9f, 3f, 24f)
                curveToRelative(-5f, 39f, 31f, 324f, 74f, 576f)
                curveToRelative(33f, 194f, 82f, 384f, 131f, 510f)
                curveToRelative(43f, 107f, 131f, 258f, 146f, 248f)
                curveToRelative(6f, -4f, 7f, -1f, 2f, 7f)
                curveToRelative(-5f, 7f, 1f, 20f, 16f, 34f)
                curveToRelative(13f, 12f, 29f, 30f, 36f, 41f)
                curveToRelative(6f, 11f, 16f, 19f, 23f, 17f)
                curveToRelative(6f, -1f, 9f, 2f, 6f, 7f)
                curveToRelative(-9f, 15f, 134f, 143f, 229f, 206f)
                curveToRelative(113f, 73f, 320f, 172f, 453f, 216f)
                curveToRelative(56f, 19f, 104f, 40f, 107f, 47f)
                curveToRelative(3f, 8f, 0f, 10f, -8f, 5f)
                curveToRelative(-22f, -14f, -37f, -12f, -37f, 5f)
                curveToRelative(0f, 15f, -2f, 15f, -10f, 2f)
                curveToRelative(-5f, -8f, -10f, -10f, -10f, -5f)
                curveToRelative(0f, 6f, -10f, 11f, -22f, 11f)
                curveToRelative(-51f, 3f, -78f, 9f, -78f, 17f)
                curveToRelative(0f, 5f, -7f, 7f, -15f, 3f)
                curveToRelative(-11f, -4f, -15f, 1f, -15f, 17f)
                curveToRelative(0f, 18f, -2f, 19f, -10f, 7f)
                curveToRelative(-6f, -9f, -10f, -11f, -10f, -3f)
                curveToRelative(0f, 7f, -6f, 10f, -14f, 7f)
                curveToRelative(-8f, -3f, -24f, 4f, -35f, 15f)
                curveToRelative(-12f, 12f, -23f, 21f, -26f, 21f)
                curveToRelative(-3f, 0f, -9f, 0f, -15f, 0f)
                curveToRelative(-5f, 0f, -10f, 6f, -10f, 14f)
                curveToRelative(0f, 8f, -8f, 13f, -17f, 12f)
                curveToRelative(-10f, 0f, -20f, 9f, -24f, 24f)
                curveToRelative(-4f, 14f, -7f, 19f, -8f, 11f)
                curveToRelative(-1f, -11f, -4f, -11f, -16f, -1f)
                curveToRelative(-8f, 7f, -12f, 17f, -8f, 23f)
                curveToRelative(3f, 5f, 1f, 7f, -4f, 4f)
                curveToRelative(-12f, -8f, -36f, 17f, -26f, 26f)
                curveToRelative(3f, 4f, -1f, 7f, -11f, 7f)
                curveToRelative(-9f, 0f, -20f, 8f, -23f, 18f)
                curveToRelative(-4f, 9f, -15f, 26f, -26f, 37f)
                curveToRelative(-30f, 32f, -98f, 181f, -100f, 219f)
                curveToRelative(-1f, 19f, -5f, 32f, -8f, 30f)
                curveToRelative(-7f, -4f, -20f, 78f, -14f, 84f)
                curveToRelative(2f, 3f, 11f, -3f, 20f, -12f)
                curveToRelative(92f, -92f, 287f, -163f, 525f, -191f)
                curveToRelative(154f, -19f, 157f, -19f, 164f, 1f)
                curveToRelative(3f, 8f, 2f, 12f, -4f, 9f)
                curveToRelative(-23f, -14f, -200f, 211f, -184f, 236f)
                curveToRelative(4f, 7f, 3f, 9f, -4f, 6f)
                curveToRelative(-13f, -9f, -66f, 127f, -58f, 149f)
                curveToRelative(3f, 8f, 2f, 13f, -3f, 10f)
                curveToRelative(-13f, -9f, -32f, 103f, -36f, 217f)
                curveToRelative(-2f, 56f, 3f, 147f, 10f, 202f)
                curveToRelative(16f, 113f, 58f, 294f, 78f, 331f)
                curveToRelative(13f, 25f, 13f, 24f, 30f, -28f)
                curveToRelative(63f, -202f, 270f, -484f, 476f, -650f)
                curveToRelative(144f, -117f, 222f, -162f, 676f, -393f)
                curveToRelative(504f, -257f, 701f, -385f, 884f, -574f)
                curveToRelative(142f, -146f, 256f, -338f, 303f, -509f)
                curveToRelative(18f, -63f, 22f, -105f, 22f, -232f)
                curveToRelative(0f, -155f, -14f, -243f, -49f, -309f)
                curveToRelative(-12f, -24f, -13f, -24f, -23f, -4f)
                curveToRelative(-6f, 11f, -8f, 24f, -4f, 28f)
                curveToRelative(4f, 5f, 2f, 5f, -4f, 2f)
                curveToRelative(-15f, -8f, -25f, 17f, -18f, 41f)
                curveToRelative(5f, 14f, 4f, 15f, -5f, 2f)
                curveToRelative(-9f, -13f, -11f, -12f, -12f, 5f)
                curveToRelative(0f, 11f, -3f, 15f, -5f, 8f)
                curveToRelative(-4f, -8f, -13f, -5f, -29f, 9f)
                curveToRelative(-14f, 12f, -22f, 25f, -19f, 30f)
                curveToRelative(3f, 5f, 0f, 7f, -8f, 6f)
                curveToRelative(-7f, -2f, -12f, 2f, -11f, 7f)
                curveToRelative(1f, 6f, -6f, 11f, -15f, 12f)
                curveToRelative(-9f, 0f, -44f, 11f, -77f, 23f)
                curveToRelative(-58f, 22f, -183f, 34f, -195f, 18f)
                curveToRelative(-3f, -3f, -21f, -8f, -40f, -10f)
                curveToRelative(-19f, -1f, -40f, -7f, -47f, -13f)
                curveToRelative(-7f, -5f, -19f, -15f, -27f, -22f)
                curveToRelative(-7f, -6f, -16f, -9f, -20f, -6f)
                curveToRelative(-3f, 3f, -8f, -2f, -12f, -11f)
                curveToRelative(-3f, -9f, -12f, -16f, -20f, -16f)
                curveToRelative(-22f, 0f, -79f, -84f, -119f, -177f)
                curveToRelative(-55f, -125f, -114f, -211f, -192f, -279f)
                curveToRelative(-38f, -32f, -69f, -59f, -71f, -60f)
                curveToRelative(-1f, 0f, 5f, -32f, 14f, -70f)
                curveToRelative(21f, -91f, 14f, -252f, -16f, -344f)
                curveToRelative(-37f, -111f, -106f, -225f, -197f, -323f)
                curveToRelative(-28f, -31f, -38f, -36f, -45f, -25f)
                curveToRelative(-8f, 10f, -9f, 9f, -4f, -4f)
                curveToRelative(6f, -20f, -21f, -51f, -36f, -42f)
                curveToRelative(-6f, 4f, -8f, 2f, -5f, -3f)
                curveToRelative(9f, -14f, -19f, -37f, -34f, -27f)
                curveToRelative(-7f, 4f, -9f, 3f, -6f, -3f)
                curveToRelative(8f, -13f, -60f, -56f, -74f, -48f)
                curveToRelative(-5f, 4f, -9f, 0f, -9f, -7f)
                curveToRelative(0f, -18f, -57f, -51f, -72f, -42f)
                curveToRelative(-6f, 4f, -8f, 3f, -5f, -3f)
                curveToRelative(6f, -9f, -128f, -73f, -153f, -73f)
                curveToRelative(-5f, 0f, -10f, -5f, -10f, -11f)
                curveToRelative(0f, -5f, -4f, -7f, -10f, -4f)
                curveToRelative(-5f, 3f, -10f, 2f, -10f, -3f)
                curveToRelative(0f, -8f, 89f, -27f, 175f, -38f)
                curveToRelative(143f, -18f, 432f, -9f, 498f, 15f)
                curveToRelative(67f, 25f, 75f, 23f, 96f, -16f)
                curveToRelative(36f, -71f, 113f, -304f, 151f, -456f)
                curveToRelative(62f, -248f, 100f, -517f, 100f, -718f)
                curveToRelative(0f, -42f, 3f, -90f, 6f, -107f)
                curveToRelative(6f, -28f, 9f, -29f, 43f, -25f)
                curveToRelative(47f, 6f, 76f, 11f, 76f, 13f)
                curveToRelative(0f, 2f, 83f, 10f, 108f, 11f)
                curveToRelative(9f, 0f, 17f, 4f, 17f, 7f)
                curveToRelative(0f, 4f, 8f, 8f, 18f, 9f)
                curveToRelative(51f, 5f, 80f, 11f, 91f, 17f)
                curveToRelative(6f, 4f, 23f, 9f, 37f, 12f)
                curveToRelative(40f, 7f, 173f, 77f, 237f, 125f)
                lineToRelative(57f, 42f)
                lineToRelative(0f, 567f)
                lineToRelative(0f, 567f)
                lineToRelative(-27f, 14f)
                curveToRelative(-107f, 54f, -321f, 173f, -333f, 185f)
                curveToRelative(-13f, 12f, -9f, 18f, 25f, 46f)
                curveToRelative(232f, 188f, 355f, 457f, 355f, 774f)
                curveToRelative(0f, 103f, 0f, 105f, 35f, 153f)
                curveToRelative(52f, 72f, 115f, 221f, 136f, 325f)
                curveToRelative(22f, 104f, 27f, 320f, 10f, 426f)
                curveToRelative(-43f, 263f, -177f, 507f, -399f, 722f)
                curveToRelative(-204f, 198f, -433f, 344f, -945f, 603f)
                curveToRelative(-464f, 235f, -561f, 297f, -726f, 465f)
                curveToRelative(-210f, 214f, -357f, 464f, -401f, 682f)
                curveToRelative(-25f, 123f, -43f, 149f, -96f, 141f)
                close()
                moveToRelative(2108f, -3186f)
                curveToRelative(59f, -61f, 80f, -130f, 86f, -278f)
                curveToRelative(5f, -154f, -17f, -257f, -84f, -396f)
                curveToRelative(-56f, -113f, -70f, -128f, -65f, -67f)
                curveToRelative(4f, 53f, -26f, 190f, -46f, 210f)
                curveToRelative(-8f, 8f, -11f, 18f, -8f, 24f)
                curveToRelative(4f, 6f, 3f, 10f, -2f, 9f)
                curveToRelative(-4f, -1f, -33f, 22f, -64f, 52f)
                curveToRelative(-64f, 62f, -120f, 85f, -242f, 103f)
                lineToRelative(-79f, 11f)
                lineToRelative(22f, 28f)
                curveToRelative(12f, 15f, 26f, 24f, 32f, 21f)
                curveToRelative(7f, -5f, 8f, -2f, 3f, 6f)
                curveToRelative(-5f, 8f, -2f, 17f, 7f, 24f)
                curveToRelative(8f, 6f, 39f, 61f, 68f, 121f)
                curveToRelative(61f, 128f, 91f, 165f, 132f, 165f)
                curveToRelative(16f, 0f, 27f, 4f, 24f, 9f)
                curveToRelative(-3f, 5f, 17f, 9f, 45f, 9f)
                curveToRelative(28f, 1f, 53f, 3f, 56f, 5f)
                curveToRelative(12f, 12f, 82f, -22f, 115f, -56f)
                close()
                moveToRelative(-429f, -548f)
                curveToRelative(50f, -11f, 94f, -24f, 95f, -30f)
                curveToRelative(2f, -5f, 9f, -9f, 15f, -9f)
                curveToRelative(20f, 0f, 57f, -43f, 57f, -66f)
                curveToRelative(0f, -13f, 5f, -26f, 11f, -30f)
                curveToRelative(18f, -11f, 2f, -150f, -23f, -198f)
                curveToRelative(-27f, -52f, -119f, -141f, -193f, -185f)
                curveToRelative(-79f, -48f, -223f, -100f, -297f, -108f)
                lineToRelative(-63f, -6f)
                lineToRelative(34f, 39f)
                curveToRelative(18f, 21f, 49f, 54f, 67f, 71f)
                curveToRelative(19f, 18f, 34f, 38f, 34f, 44f)
                curveToRelative(0f, 6f, 5f, 20f, 11f, 31f)
                curveToRelative(9f, 17f, 13f, 18f, 21f, 7f)
                curveToRelative(7f, -10f, 7f, -7f, 3f, 9f)
                curveToRelative(-4f, 13f, -2f, 28f, 3f, 35f)
                curveToRelative(29f, 37f, 78f, 213f, 88f, 314f)
                curveToRelative(8f, 85f, 13f, 103f, 29f, 103f)
                curveToRelative(8f, 0f, 57f, -9f, 108f, -21f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3977f, 4460f)
                curveToRelative(-95f, -24f, -192f, -112f, -224f, -203f)
                curveToRelative(-25f, -72f, -23f, -178f, 4f, -239f)
                curveToRelative(30f, -65f, 95f, -133f, 162f, -165f)
                curveToRelative(46f, -23f, 69f, -28f, 131f, -28f)
                curveToRelative(128f, 0f, 228f, 64f, 287f, 182f)
                curveToRelative(24f, 50f, 28f, 70f, 28f, 143f)
                curveToRelative(0f, 98f, -20f, 152f, -80f, 218f)
                curveToRelative(-70f, 78f, -204f, 118f, -308f, 92f)
                close()
                moveToRelative(234f, -119f)
                curveToRelative(36f, -36f, 37f, -70f, 3f, -110f)
                curveToRelative(-34f, -41f, -75f, -42f, -115f, -2f)
                curveToRelative(-38f, 38f, -39f, 83f, -3f, 116f)
                curveToRelative(37f, 34f, 78f, 33f, 115f, -4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2625f, 4433f)
                curveToRelative(-190f, -68f, -319f, -235f, -355f, -457f)
                curveToRelative(-38f, -237f, 41f, -432f, 208f, -513f)
                curveToRelative(47f, -22f, 71f, -27f, 137f, -27f)
                curveToRelative(222f, -2f, 423f, 179f, 485f, 435f)
                curveToRelative(57f, 240f, -24f, 456f, -207f, 545f)
                curveToRelative(-73f, 36f, -193f, 44f, -268f, 17f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(5805f, 3456f)
                curveToRelative(-83f, -36f, -114f, -138f, -64f, -208f)
                curveToRelative(42f, -58f, 117f, -74f, 177f, -38f)
                curveToRelative(101f, 62f, 93f, 200f, -15f, 246f)
                curveToRelative(-40f, 17f, -59f, 17f, -98f, 0f)
                close()
            }
        }
    }.build()
    return memosIcon!!
}

