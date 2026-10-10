package com.zeus97x.zbattle

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zeus97x.zbattle.ui.ArtLoader
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Decodes APK assets with power-of-two downsampling (as ZPet's CreatureView does), so the
 * 1–2 MB creature PNGs never decode at full size. Missing paths are remembered and return null,
 * which makes the UI draw its placeholder.
 */
class AssetArtLoader(private val assets: AssetManager, private val maxDimension: Int = 768) : ArtLoader {
    private val cache = object : LruCache<String, ImageBitmap>(cacheKilobytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount / 1024
    }
    private val missing = ConcurrentHashMap.newKeySet<String>()

    override fun cached(path: String): ImageBitmap? = cache.get(path)

    override fun load(path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        if (path in missing) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
            bitmap?.asImageBitmap()?.also { cache.put(path, it) } ?: run { missing += path; null }
        } catch (_: IOException) {
            missing += path
            null
        }
    }

    private companion object {
        fun cacheKilobytes(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()
    }
}
