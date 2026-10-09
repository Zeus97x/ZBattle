package com.zeus97x.zbattle.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtCatalog
import com.zeus97x.zbattle.core.ArtFit
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.PlaceholderStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Platform image source. Android reads APK assets; the JVM harness reads the repo files. */
interface ArtLoader {
    /** Already-decoded bitmap for [path], without doing I/O. */
    fun cached(path: String): ImageBitmap?
    /** Decodes [path] (downsampled), or returns null when no such asset exists. Off main thread. */
    fun load(path: String): ImageBitmap?
}

object NoArtLoader : ArtLoader {
    override fun cached(path: String): ImageBitmap? = null
    override fun load(path: String): ImageBitmap? = null
}

val LocalArtLoader = staticCompositionLocalOf<ArtLoader> { NoArtLoader }

@Composable
fun rememberArt(key: ArtKey): ImageBitmap? {
    val loader = LocalArtLoader.current
    val candidates = remember(key) { ArtCatalog.candidates(key) }
    val initial = remember(key, loader) { candidates.firstNotNullOfOrNull(loader::cached) }
    val image by produceState(initial, key, loader) {
        if (value == null && candidates.isNotEmpty()) {
            value = withContext(Dispatchers.IO) { candidates.firstNotNullOfOrNull(loader::load) }
        }
    }
    return image
}

private fun PlaceholderStyle.icon(): ImageVector = when (this) {
    PlaceholderStyle.Creature -> Icons.Filled.Pets
    PlaceholderStyle.Brand -> Icons.Filled.Bolt
    PlaceholderStyle.Map -> Icons.Filled.Map
    PlaceholderStyle.Scenery -> Icons.Filled.Landscape
    PlaceholderStyle.Portrait -> Icons.Filled.Person
    PlaceholderStyle.Item -> Icons.Filled.Inventory2
    PlaceholderStyle.Badge -> Icons.Filled.EmojiEvents
}

/**
 * Draws the artwork for [key] or, until that art exists, a restrained themed gradient with a
 * native icon. Creature art uses contain/fit with alpha; scenery uses crop/cover.
 */
@Composable
fun ArtworkSlot(
    key: ArtKey,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderLabel: String? = null,
    placeholderAlignment: Alignment = Alignment.Center,
) {
    val image = rememberArt(key)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = if (key.fit == ArtFit.Cover) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            ArtPlaceholder(key.placeholder, contentDescription, placeholderLabel, Modifier.matchParentSize(), placeholderAlignment)
        }
    }
}

@Composable
fun ArtPlaceholder(
    style: PlaceholderStyle,
    contentDescription: String?,
    label: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    val p = Z.colors
    val brush = remember(style, p) {
        when (style) {
            PlaceholderStyle.Scenery, PlaceholderStyle.Map -> Brush.linearGradient(listOf(p.elevated, p.surface, p.accentDark.copy(alpha = 0.35f)))
            PlaceholderStyle.Creature -> Brush.radialGradient(listOf(p.elevated, p.surface))
            else -> Brush.linearGradient(listOf(p.elevated, p.surface))
        }
    }
    Box(modifier.background(brush), contentAlignment = alignment) {
        Column(Modifier.padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(style.icon(), contentDescription = contentDescription, tint = p.textSecondary.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
