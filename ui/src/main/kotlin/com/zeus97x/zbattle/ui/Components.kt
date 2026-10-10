package com.zeus97x.zbattle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Creature

/** Pink header with optional back arrow; handles the status-bar inset. */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable () -> Unit)? = null,
) {
    val p = Z.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = Dimens.cardRadius, bottomEnd = Dimens.cardRadius))
            .background(p.headerGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = if (onBack != null) 4.dp else Dimens.screenPadding, end = 8.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = p.onAccent)
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = p.onAccent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.onAccent.copy(alpha = 0.92f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
        if (bottom != null) {
            Box(Modifier.padding(start = if (onBack != null) 12.dp else 0.dp, end = 8.dp, top = 8.dp)) { bottom() }
        }
    }
}

/** Compact value chip. Dark translucent background keeps small white text readable on pink. */
@Composable
fun StatChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier, onHeader: Boolean = true) {
    val p = Z.colors
    val bg = if (onHeader) Color(0x47141720) else p.elevated
    val fg = if (onHeader) Color.White else p.textPrimary
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text("$value $label", style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, container: Color = Z.colors.elevated, content: Color = Z.colors.textPrimary, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Marks anything whose data or behaviour is not real yet. */
@Composable
fun PreviewBadge(modifier: Modifier = Modifier, text: String = "Preview") {
    Pill(text, modifier, container = Color(0xFFFFE08A), content = Color(0xFF3A2A00), icon = Icons.Filled.Science)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val p = Z.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Dimens.primaryButtonHeight),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = p.accentDark, contentColor = p.onAccent),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = Z.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.primaryButtonHeight),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, p.accent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.textPrimary),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val p = Z.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
    }
}

@Composable
fun ZCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, selected: Boolean = false, content: @Composable () -> Unit) {
    val p = Z.colors
    val shape = RoundedCornerShape(Dimens.cardRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = shape,
        color = if (selected) p.elevated else p.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) p.accent else p.border),
        content = content,
    )
}

/** Creature tile: existing ZPet art (fit), name and stage chip. */
@Composable
fun CreatureCard(creature: Creature, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, caption: String? = null) {
    val p = Z.colors
    ZCard(modifier, onClick = onClick) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ArtworkSlot(
                ArtKey.CreatureArt(creature),
                contentDescription = "${creature.name} artwork",
                placeholderLabel = if (creature.hasArtwork) null else "Artwork pending",
                modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(14.dp)).background(p.elevated),
            )
            Text(creature.name, style = MaterialTheme.typography.titleMedium, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Pill(creature.stage.shortLabel)
            if (caption != null) Text(caption, style = MaterialTheme.typography.labelSmall, color = p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProgressRow(label: String, value: String, fraction: Float?, modifier: Modifier = Modifier, planned: Boolean = false) {
    val p = Z.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = p.textPrimary, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = if (planned) p.textSecondary else p.textPrimary)
        }
        if (fraction != null) {
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                color = p.success,
                trackColor = p.elevated,
            )
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = Z.colors
    val clickModifier = when {
        checked != null && onCheckedChange != null -> Modifier.clickable(role = Role.Switch) { onCheckedChange(!checked) }
        onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
        else -> Modifier
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).then(clickModifier).padding(horizontal = Dimens.cardPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(p.elevated), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = p.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
        }
        if (checked != null) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = p.accentDark, checkedThumbColor = Color.White, uncheckedTrackColor = p.elevated, uncheckedBorderColor = p.border),
            )
        } else if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = p.textSecondary)
        }
    }
}

@Composable
fun EmptyState(key: ArtKey, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val p = Z.colors
    Column(modifier.fillMaxWidth().padding(Dimens.gapLarge), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtworkSlot(key, contentDescription = null, modifier = Modifier.size(140.dp).clip(RoundedCornerShape(Dimens.cardRadius)))
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = p.textSecondary)
        action?.invoke()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Z.colors.border))
}

@Composable
fun CircleThumb(creature: Creature, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    val p = Z.colors
    ArtworkSlot(
        ArtKey.CreatureArt(creature),
        contentDescription = creature.name,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(p.elevated)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .padding(4.dp),
    )
}
