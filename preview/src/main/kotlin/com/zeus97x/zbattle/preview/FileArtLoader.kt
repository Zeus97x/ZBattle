package com.zeus97x.zbattle.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.zeus97x.zbattle.ui.ArtLoader
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Reads art from the repository the same way the APK packages it: the untouched ZPet pack
 * (`ZBattle-ZPet-Assets/assets`) plus `app/src/main/assets` for future artwork.
 */
class FileArtLoader(private val roots: List<File>, private val maxDimension: Int = 512) : ArtLoader {
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
        val image = ImageIO.read(file)?.let(::downscale)?.toComposeImageBitmap() ?: return null
        cache[path] = image
        return image
    }

    private fun downscale(source: BufferedImage): BufferedImage {
        val scale = maxDimension.toDouble() / maxOf(source.width, source.height)
        if (scale >= 1.0) return source
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(source, 0, 0, w, h, null)
        g.dispose()
        return out
    }
}
