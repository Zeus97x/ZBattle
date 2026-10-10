package com.zeus97x.zbattle.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.zeus97x.zbattle.ui.ArtLoader
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads art from the repository the same way the APK packages it: the untouched ZPet pack
 * (`ZBattle-ZPet-Assets/assets`) plus `app/src/main/assets` (WebP scenery). Decodes with Skia,
 * which handles PNG and WebP like Android's decoder.
 */
class FileArtLoader(private val roots: List<File>, private val maxDimension: Int = 1024) : ArtLoader {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    override fun cached(path: String): ImageBitmap? = cache[path]

    override fun load(path: String): ImageBitmap? {
        cache[path]?.let { return it }
        if (path in missing) return null
        val file = roots.map { File(it, path) }.firstOrNull { it.isFile }
        if (file == null) {
            missing += path
            return null
        }
        val decoded = runCatching { Image.makeFromEncoded(file.readBytes()) }.getOrNull() ?: return null
        val image = downscale(decoded).toComposeImageBitmap()
        cache[path] = image
        return image
    }

    private fun downscale(source: Image): Image {
        val scale = maxDimension.toDouble() / maxOf(source.width, source.height)
        if (scale >= 1.0) return source
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(source, Rect.makeWH(w.toFloat(), h.toFloat()))
        return surface.makeImageSnapshot()
    }
}
