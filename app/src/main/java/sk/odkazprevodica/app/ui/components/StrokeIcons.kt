package sk.odkazprevodica.app.ui.components

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Obrysové ikony 24×24 z rovnakých SVG ciest ako v návrhu.
 * Farbu určuje `tint` pri vykreslení cez Icon().
 */
fun strokeIcon(name: String, pathData: String, strokeWidth: Float = 2f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

object AppIcons {
    val Message = strokeIcon("message", "M4 5h16v11H9l-5 4z")
    val Camera = strokeIcon("camera", "M3 8h4l2-3h6l2 3h4v12H3zM12 10a4 4 0 1 0 0 8a4 4 0 1 0 0-8z")
    val Plus = strokeIcon("plus", "M12 5v14M5 12h14")
    val Home = strokeIcon("home", "M3 11l9-8 9 8v10H3z")
    val Send = strokeIcon("send", "M4 12l16-8-6 16-3-7z")
    val Settings = strokeIcon(
        "settings",
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6zM12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1",
    )
    val Back = strokeIcon("back", "M15 5l-7 7 7 7", 2.2f)
    val Check = strokeIcon("check", "M5 12l5 5 9-10", 2.2f)
    val Shield = strokeIcon("shield", "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6zM12 9v4M12 16h.01", 2.2f)
}
