package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private val iconCache = HashMap<String, ImageVector>()

private fun svgIcon(name: String, vararg paths: String): ImageVector {
    return synchronized(iconCache) {
        iconCache.getOrPut(name) {
            val builder = ImageVector.Builder(
                name = "Filled.$name",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            )
            for (p in paths) {
                builder.addPath(
                    pathData = PathParser().parsePathString(p).toNodes(),
                    fill = SolidColor(Color.Black)
                )
            }
            builder.build()
        }
    }
}

val Icons.Filled.AddCircleOutline: ImageVector
    get() = svgIcon("AddCircleOutline", "M13,7h-2v4H7v2h4v4h2v-4h4v-2h-4V7zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z")

val Icons.Filled.Alarm: ImageVector
    get() = svgIcon("Alarm", "M22,5.72l-4.6,-3.86 -1.29,1.53 4.6,3.86L22,5.72zM7.88,3.39L6.6,1.86 2,5.71l1.29,1.53 4.59,-3.85zM12.5,8H11v6l4.75,2.85 0.75,-1.23 -4,-2.37V8zM12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zm0,16c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z")

val Icons.Filled.AlarmAdd: ImageVector
    get() = svgIcon("AlarmAdd", "M7.88,3.39L6.6,1.86 2,5.71l1.29,1.53 4.59,-3.85zM22,5.72l-4.6,-3.86 -1.29,1.53 4.6,3.86L22,5.72zM12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zm0,16c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7zm1,-11h-2v3H8v2h3v3h2v-3h3v-2h-3V9z")

val Icons.Filled.AudioFile: ImageVector
    get() = svgIcon("AudioFile", "M14,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8L14,2zM16,13h-3v3.75c0,1.24 -1.01,2.25 -2.25,2.25S8.5,17.99 8.5,16.75c0,-1.24 1.01,-2.25 2.25,-2.25c0.46,0 0.89,0.14 1.25,0.38V11h4V13zM13,9V3.5L18.5,9H13z")

val Icons.Filled.AutoAwesome: ImageVector
    get() = svgIcon("AutoAwesome", "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20l2.5,-5.5L17,12L11.5,9.5z")

val Icons.Filled.AutoStories: ImageVector
    get() = svgIcon("AutoStories", "M19,1l-5,5v11l5,-4.5V1zM1,6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5V6c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6zM23,19.5V6c-0.6,-0.45 -1.25,-0.75 -2,-1v13.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5v2c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5,-0.5V19.5z")

val Icons.Filled.Backup: ImageVector
    get() = svgIcon("Backup", "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96zM14,13v4h-4v-4H7l5,-5 5,5h-3z")

val Icons.Filled.Book: ImageVector
    get() = svgIcon("Book", "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z")

val Icons.Filled.Bookmark: ImageVector
    get() = svgIcon("Bookmark", "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z")

val Icons.Filled.BookmarkAdded: ImageVector
    get() = svgIcon("BookmarkAdded", "M19,21l-7,-3l-7,3V5c0,-1.1 0.9,-2 2,-2h7c-0.63,0.84 -1,1.87 -1,3c0,2.76 2.24,5 5,5c0.34,0 0.68,-0.03 1,-0.1V21zM17.83,9L15,6.17l1.41,-1.41l1.41,1.41l3.54,-3.54l1.41,1.41L17.83,9z")

val Icons.Filled.BookmarkBorder: ImageVector
    get() = svgIcon("BookmarkBorder", "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2zm0,15l-5,-2.18L7,18V5h10v13z")

val Icons.Filled.CalendarToday: ImageVector
    get() = svgIcon("CalendarToday", "M20,3h-1V1h-2v2H7V1H5v2H4c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zm0,18H4V8h16v13z")

val Icons.Filled.CameraAlt: ImageVector
    get() = svgIcon("CameraAlt", "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zm3,15c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z")

val Icons.Filled.CardGiftcard: ImageVector
    get() = svgIcon("CardGiftcard", "M20,6h-2.18c0.11,-0.31 0.18,-0.65 0.18,-1 0,-1.66 -1.34,-3 -3,-3 -1.05,0 -1.96,0.54 -2.5,1.35l-0.5,0.67 -0.5,-0.68C10.96,2.54 10.05,2 9,2 7.34,2 6,3.34 6,5c0,0.35 0.07,0.69 0.18,1H4c-1.11,0 -1.99,0.89 -1.99,2L2,19c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V8c0,-1.11 -0.89,-2 -2,-2zm-5,-2c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM9,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zm11,15H4v-2h16v2zm0,-5H4V8h5.08L7,10.83 8.62,12 11,8.76l1,-1.36 1,1.36L15.38,12 17,10.83 14.92,8H20v6z")

val Icons.Filled.ChevronLeft: ImageVector
    get() = svgIcon("ChevronLeft", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")

val Icons.Filled.ContentCopy: ImageVector
    get() = svgIcon("ContentCopy", "M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1zm3,4H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zm0,16H8V7h11v14z")

val Icons.Filled.ContentPaste: ImageVector
    get() = svgIcon("ContentPaste", "M19,2h-4.18C14.4,0.84 13.3,0 12,0c-1.3,0 -2.4,0.84 -2.82,2H5c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zm-7,0c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zm7,18H5V4h2v3h10V4h2v16z")

val Icons.Filled.DarkMode: ImageVector
    get() = svgIcon("DarkMode", "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 -0.9,-0.1 -1.36,-0.1z")

val Icons.Filled.DoneAll: ImageVector
    get() = svgIcon("DoneAll", "M18,7l-1.41,-1.41 -6.34,6.34 1.41,1.41L18,7zm4.24,-1.41L11.66,16.17 7.48,12l-1.41,1.41L11.66,19l12,-12 -1.42,-1.41zM0.41,13.41L6,19l1.41,-1.41L1.83,12 0.41,13.41z")

val Icons.Filled.Download: ImageVector
    get() = svgIcon("Download", "M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z")

val Icons.Filled.ExpandLess: ImageVector
    get() = svgIcon("ExpandLess", "M12,8l-6,6 1.41,1.41L12,10.83l4.59,4.58L18,14z")

val Icons.Filled.ExpandMore: ImageVector
    get() = svgIcon("ExpandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z")

val Icons.Filled.Explore: ImageVector
    get() = svgIcon("Explore", "M12,10.9c-0.61,0 -1.1,0.49 -1.1,1.1s0.49,1.1 1.1,1.1c0.61,0 1.1,-0.49 1.1,-1.1s-0.49,-1.1 -1.1,-1.1zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm2.19,12.19L6,18l3.81,-8.19L18,6l-3.81,8.19z")

val Icons.Filled.FastForward: ImageVector
    get() = svgIcon("FastForward", "M4,18l8.5,-6L4,6v12zm9,-12v12l8.5,-6L13,6z")

val Icons.Filled.FastRewind: ImageVector
    get() = svgIcon("FastRewind", "M11,18V6l-8.5,6 8.5,6zm0.5,-6l8.5,6V6l-8.5,6z")

val Icons.Filled.FolderOpen: ImageVector
    get() = svgIcon("FolderOpen", "M20,6h-8l-2,-2H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zm0,12H4V8h16v10z")

val Icons.Filled.FormatListBulleted: ImageVector
    get() = svgIcon("FormatListBulleted", "M4,10.5c-0.83,0 -1.5,0.67 -1.5,1.5s0.67,1.5 1.5,1.5 1.5,-0.67 1.5,-1.5 -0.67,-1.5 -1.5,-1.5zm0,-6c-0.83,0 -1.5,0.67 -1.5,1.5S3.17,7.5 4,7.5 5.5,6.83 5.5,6 4.83,4.5 4,4.5zm0,12c-0.83,0 -1.5,0.68 -1.5,1.5s0.68,1.5 1.5,1.5 1.5,-0.68 1.5,-1.5 -0.67,-1.5 -1.5,-1.5zM7,19h14v-2H7v2zm0,-6h14v-2H7v2zm0,-8v2h14V5H7z")

val Icons.Filled.FormatSize: ImageVector
    get() = svgIcon("FormatSize", "M9,4v3h5v12h3V7h5V4H9zm-6,8h3v7h3v-7h3V9H3v3z")

val Icons.Filled.Fullscreen: ImageVector
    get() = svgIcon("Fullscreen", "M7,14H5v5h5v-2H7v-3zm-2,-4h2V7h3V5H5v5zm12,7h-3v2h5v-5h-2v3zM14,5v2h3v3h2V5h-5z")

val Icons.Filled.FullscreenExit: ImageVector
    get() = svgIcon("FullscreenExit", "M5,16h3v3h2v-5H5v2zm3,-8H5v2h5V5H8v3zm6,11h2v-3h3v-2h-5v5zm2,-11V5h-2v5h5V8h-3z")

val Icons.Filled.Headphones: ImageVector
    get() = svgIcon("Headphones", "M12,3c-4.97,0 -9,4.03 -9,9v7c0,1.1 0.9,2 2,2h4v-8H5v-1c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v1h-4v8h4c1.1,0 2,-0.9 2,-2v-7c0,-4.97 -4.03,-9 -9,-9z")

val Icons.Filled.HelpOutline: ImageVector
    get() = svgIcon("HelpOutline", "M11,18h2v-2h-2v2zm1,-16C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm0,18c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zm0,-14c-2.21,0 -4,1.79 -4,4h2c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,2 -3,1.75 -3,5h2c0,-2.25 3,-2.5 3,-5 0,-2.21 -1.79,-4 -4,-4z")

val Icons.Filled.History: ImageVector
    get() = svgIcon("History", "M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zm-1,5v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z")

val Icons.Filled.LightMode: ImageVector
    get() = svgIcon("LightMode", "M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -2.24,-5 -5,-5zM2,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zm18,0h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zm0,18v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM5.99,4.58c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0s0.39,-1.03 0,-1.41L5.99,4.58zm12.37,12.37c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0 0.39,-0.39 0.39,-1.03 0,-1.41l-1.06,-1.06zm1.06,-10.96c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06zM7.05,18.36c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06z")

val Icons.Filled.Lightbulb: ImageVector
    get() = svgIcon("Lightbulb", "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9v1zm3,-19C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7z")

val Icons.Filled.MarkEmailRead: ImageVector
    get() = svgIcon("MarkEmailRead", "M12,19c0,-3.87 3.13,-7 7,-7c1.08,0 2.09,0.25 3,0.68V6c0,-1.1 -0.9,-2 -2,-2H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h8.08C12.03,19.67 12,19.34 12,19zM4,6l8,5l8,-5v2l-8,5L4,8V6zM17.34,22l-3.54,-3.54l1.41,-1.41l2.12,2.12l4.24,-4.24L23,16.34L17.34,22z")

val Icons.Filled.MenuBook: ImageVector
    get() = svgIcon("MenuBook", "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5,-0.5V6C22.4,5.55 21.75,5.25 21,5zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.7,0 -4.15,0.65 -5.5,1.5V8c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.2,0 2.4,0.15 3.5,0.5V18.5z")

val Icons.Filled.MusicNote: ImageVector
    get() = svgIcon("MusicNote", "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z")

val Icons.Filled.MyLocation: ImageVector
    get() = svgIcon("MyLocation", "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zm8.94,3c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94V1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11H1v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94V23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94H23v-2h-2.06zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z")

val Icons.Filled.NavigateBefore: ImageVector
    get() = svgIcon("NavigateBefore", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")

val Icons.Filled.NavigateNext: ImageVector
    get() = svgIcon("NavigateNext", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")

val Icons.Filled.NightsStay: ImageVector
    get() = svgIcon("NightsStay", "M11.1,12.08C8.77,7.57 10.6,3.6 11.63,2.01C6.27,2.2 1.98,6.59 1.98,12c0,0.14 0.02,0.28 0.02,0.42C2.62,12.15 3.29,12 4,12c1.66,0 3.18,0.83 4.1,2.15C9.77,14.63 11,16.17 11,18c0,1.52 -0.87,2.83 -2.12,3.51c0.98,0.32 2.03,0.5 3.11,0.5c3.5,0 6.58,-1.8 8.37,-4.52C18,17.72 13.38,16.52 11.1,12.08z")

val Icons.Filled.NotificationsActive: ImageVector
    get() = svgIcon("NotificationsActive", "M7.58,4.08L6.15,2.65C3.75,4.48 2.17,7.3 2.03,10.5h2c0.15,-2.65 1.51,-4.97 3.55,-6.42zm12.39,6.42h2c-0.15,-3.2 -1.73,-6.02 -4.12,-7.85l-1.42,1.43c2.02,1.45 3.39,3.77 3.54,6.42zM18,11c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2v-5zm-6,11c0.14,0 0.27,-0.01 0.4,-0.04 0.65,-0.14 1.18,-0.58 1.44,-1.18 0.1,-0.24 0.15,-0.5 0.15,-0.78h-4c0.01,1.1 0.9,2 2.01,2z")

val Icons.Filled.Pause: ImageVector
    get() = svgIcon("Pause", "M6,19h4V5H6v14zm8,-14v14h4V5h-4z")

val Icons.Filled.PauseCircleFilled: ImageVector
    get() = svgIcon("PauseCircleFilled", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm-1,14H9V8h2v8zm4,0h-2V8h2v8z")

val Icons.Filled.PhoneAndroid: ImageVector
    get() = svgIcon("PhoneAndroid", "M16,1H8C6.34,1 5,2.34 5,4v16c0,1.66 1.34,3 3,3h8c1.66,0 3,-1.34 3,-3V4c0,-1.66 -1.34,-3 -3,-3zm-2,20h-4v-1h4v1zm3.25,-3H6.75V4h10.5v14z")

val Icons.Filled.PhotoLibrary: ImageVector
    get() = svgIcon("PhotoLibrary", "M22,16V4c0,-1.1 -0.9,-2 -2,-2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2zm-11,-4l2.03,2.71L16,11l4,5H8l3,-4zM2,6v14c0,1.1 0.9,2 2,2h14v-2H4V6H2z")

val Icons.Filled.PlayCircleFilled: ImageVector
    get() = svgIcon("PlayCircleFilled", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm-2,14.5v-9l6,4.5 -6,4.5z")

val Icons.Filled.Public: ImageVector
    get() = svgIcon("Public", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm-1,17.93c-3.95,-0.49 -7,-3.85 -7,-7.93 0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zm6.9,-2.54c-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3c0,-0.55 -0.45,-1 -1,-1H8v-2h2c0.55,0 1,-0.45 1,-1V7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41 0,2.08 -0.8,3.97 -2.1,5.39z")

val Icons.Filled.Repeat: ImageVector
    get() = svgIcon("Repeat", "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zm10,10H7v-3l-4,4 4,4v-3h12v-6h-2v4z")

val Icons.Filled.Schedule: ImageVector
    get() = svgIcon("Schedule", "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zm0.5,-13H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z")

val Icons.Filled.Security: ImageVector
    get() = svgIcon("Security", "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4zm0,10.99h7c-0.53,4.12 -3.28,7.79 -7,8.94V12H5V6.3l7,-3.11v8.8z")

val Icons.Filled.Sensors: ImageVector
    get() = svgIcon("Sensors", "M7.76,16.24C6.67,15.16 6,13.66 6,12s0.67,-3.16 1.76,-4.24l1.42,1.42C8.45,9.9 8,10.9 8,12c0,1.1 0.45,2.1 1.17,2.83L7.76,16.24zM16.24,16.24C17.33,15.16 18,13.66 18,12s-0.67,-3.16 -1.76,-4.24l-1.42,1.42C15.55,9.9 16,10.9 16,12c0,1.1 -0.45,2.1 -1.17,2.83L16.24,16.24zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM4.93,19.07c-1.81,-1.81 -2.93,-4.31 -2.93,-7.07s1.12,-5.26 2.93,-7.07l1.41,1.41C4.9,7.79 4,9.79 4,12s0.9,4.21 2.34,5.66L4.93,19.07zM19.07,19.07c1.81,-1.81 2.93,-4.31 2.93,-7.07s-1.12,-5.26 -2.93,-7.07l-1.41,1.41C19.1,7.79 20,9.79 20,12s-0.9,4.21 -2.34,5.66L19.07,19.07z")

val Icons.Filled.SkipNext: ImageVector
    get() = svgIcon("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")

val Icons.Filled.SkipPrevious: ImageVector
    get() = svgIcon("SkipPrevious", "M6,6h2v12H6zm3.5,6l8.5,6V6z")

val Icons.Filled.Speed: ImageVector
    get() = svgIcon("Speed", "M20.38,8.57l-1.23,1.85a8,8 0,0 1,-0.22 7.58H5.07A8,8 0,0 1,15.58 6.85l1.85,-1.23A10,10 0,0 0,3.35 19a2,2 0,0 0,1.72 1h13.85a2,2 0,0 0,1.74 -1,10 10,0 0,0 -0.27,-10.44zm-9.79,6.84a2,2 0,0 0,2.83 0l5.66,-8.49 -8.49,5.66a2,2 0,0 0,0 2.83z")

val Icons.Filled.Stop: ImageVector
    get() = svgIcon("Stop", "M6,6h12v12H6z")

val Icons.Filled.StopCircle: ImageVector
    get() = svgIcon("StopCircle", "M12,2C6.48,2 2,6.48 2,12c0,5.52 4.48,10 10,10s10,-4.48 10,-10C22,6.48 17.52,2 12,2zM16,16H8V8h8V16z")

val Icons.Filled.TouchApp: ImageVector
    get() = svgIcon("TouchApp", "M9,11.24V7.5C9,6.12 10.12,5 11.5,5S14,6.12 14,7.5v3.74c1.21,-0.81 2,-2.18 2,-3.74C16,5.01 13.99,3 11.5,3S7,5.01 7,7.5C7,9.06 7.79,10.43 9,11.24zM18.84,15.87l-4.54,-2.26c-0.17,-0.07 -0.35,-0.11 -0.54,-0.11H13v-6C13,6.67 12.33,6 11.5,6S10,6.67 10,7.5v10.74c-3.6,-0.76 -3.54,-0.75 -3.67,-0.75 -0.31,0 -0.59,0.13 -0.79,0.33l-0.79,0.8 4.94,4.94C9.96,23.83 10.34,24 10.75,24h6.79c0.75,0 1.33,-0.55 1.44,-1.28l0.75,-5.27c0.01,-0.07 0.02,-0.14 0.02,-0.2C19.75,16.63 19.37,16.09 18.84,15.87z")

val Icons.Filled.Tune: ImageVector
    get() = svgIcon("Tune", "M3,17v2h6v-2H3zM3,5v2h10V5H3zm10,16v-2h8v-2h-8v-2h-2v6h2zM7,9v2H3v2h4v2h2V9H7zm14,4v-2H11v2h10zm-6,-4h2V7h4V5h-4V3h-2v6z")

val Icons.Filled.UploadFile: ImageVector
    get() = svgIcon("UploadFile", "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zm4,18H6V4h7v5h5v11zM8,15.01l1.41,1.41L11,14.84V19h2v-4.16l1.59,1.59L16,15.01 12.01,11z")

val Icons.Filled.VerifiedUser: ImageVector
    get() = svgIcon("VerifiedUser", "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4zm-2,16l-4,-4 1.41,-1.41L10,14.17l6.59,-6.59L18,9l-8,8z")

val Icons.Filled.ViewAgenda: ImageVector
    get() = svgIcon("ViewAgenda", "M20,13H3c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h17c0.55,0 1,-0.45 1,-1v-6c0,-0.55 -0.45,-1 -1,-1zm0,-10H3c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h17c0.55,0 1,-0.45 1,-1V4c0,-0.55 -0.45,-1 -1,-1z")

val Icons.Filled.VolumeDown: ImageVector
    get() = svgIcon("VolumeDown", "M18.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM5,9v6h4l5,5V4L9,9H5z")

val Icons.Filled.VolumeOff: ImageVector
    get() = svgIcon("VolumeOff", "M16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v2.21l2.45,2.45c0.03,-0.2 0.05,-0.41 0.05,-0.63zm2.5,0c0,0.94 -0.2,1.82 -0.54,2.64l1.51,1.51C20.63,14.91 21,13.5 21,12c0,-4.28 -2.99,-7.86 -7,-8.77v2.06c2.89,0.86 5,3.54 5,6.71zM4.27,3L3,4.27 7.73,9H3v6h4l5,5v-6.73l4.25,4.25c-0.67,0.52 -1.42,0.93 -2.25,1.18v2.06c1.38,-0.31 2.63,-0.95 3.69,-1.81L19.73,21 21,19.73l-9,-9L4.27,3zM12,4L9.91,6.09 12,8.18V4z")

val Icons.Filled.VolumeUp: ImageVector
    get() = svgIcon("VolumeUp", "M3,9v6h4l5,5V4L7,9H3zm13.5,3c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z")

val Icons.Filled.WbSunny: ImageVector
    get() = svgIcon("WbSunny", "M6.76,4.84l-1.8,-1.79 -1.41,1.41 1.79,1.79 1.42,-1.41zM4,10.5H1v2h3v-2zm9,-9.95h-2V3.5h2V0.55zm7.45,3.91l-1.41,-1.41 -1.79,1.79 1.41,1.41 1.79,-1.79zm-3.21,13.7l1.79,1.8 1.41,-1.41 -1.8,-1.79 -1.4,1.4zM20,10.5v2h3v-2h-3zm-8,-5c-3.31,0 -6,2.69 -6,6s2.69,6 6,6 6,-2.69 6,-6 -2.69,-6 -6,-6zm-1,16.95h2V19.5h-2v2.95zm-7.45,-3.91l1.41,1.41 1.79,-1.8 -1.41,-1.41 -1.79,1.8z")
