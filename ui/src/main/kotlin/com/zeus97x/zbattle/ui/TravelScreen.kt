package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.RegionGroup
import com.zeus97x.zbattle.core.TravelRules

@Composable
fun TravelScreen(state: AppState, focusGroup: Int) {
    val p = Z.colors
    val settings = state.settings
    var groupIndex by rememberSaveable { mutableStateOf(focusGroup) }
    var selectedArea by rememberSaveable { mutableStateOf<Int?>(settings.currentAreaIndex.takeIf { it / 4 == focusGroup }) }
    val group = RegionCatalog.groups[groupIndex]
    val chosen = selectedArea?.let(RegionCatalog::area)
    val chosenUnlocked = chosen != null && TravelRules.isUnlocked(chosen, settings.visitedAreas)

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Travel", subtitle = "Choose a region, then a location", onBack = { state.back() })
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = Dimens.gap, bottom = Dimens.gap),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
        ) {
            item {
                LazyRow(
                    state = rememberLazyListState(initialFirstVisibleItemIndex = (focusGroup - 1).coerceAtLeast(0)),
                    contentPadding = PaddingValues(horizontal = Dimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(RegionCatalog.groups, key = { it.id }) { g ->
                        RegionChip(g, selected = g.index == groupIndex, isCurrent = g.index == settings.currentArea.groupIndex) {
                            groupIndex = g.index
                            selectedArea = settings.currentAreaIndex.takeIf { it / 4 == g.index }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = Dimens.screenPadding)) {
                    Text(group.tradition, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
                    Text("Group ${group.index + 1} · ${group.rangeLabel}", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            item {
                // The whole portrait map is shown (Contain) so no landmark is cropped; a dimmed
                // cover-scaled copy fills the side bands behind it.
                val mapKey = ArtKey.RegionMap(group)
                val hasMap = rememberArt(mapKey) != null
                Box(
                    Modifier
                        .padding(horizontal = Dimens.screenPadding)
                        .fillMaxWidth()
                        .then(if (hasMap) Modifier.aspectRatio(0.9f) else Modifier.height(190.dp))
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .background(DarkPalette.surface),
                ) {
                    if (hasMap) {
                        ArtworkSlot(mapKey, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.35f), contentScale = ContentScale.Crop)
                    }
                    ArtworkSlot(mapKey, contentDescription = "${group.tradition} region map", modifier = Modifier.fillMaxSize(), placeholderLabel = "Region map art pending")
                }
            }
            items(group.areas, key = { it.id }) { area ->
                AreaRow(
                    area = area,
                    selected = area.index == selectedArea,
                    isCurrent = area.index == settings.currentAreaIndex,
                    visited = area.index in settings.visitedAreas,
                    unlocked = TravelRules.isUnlocked(area, settings.visitedAreas),
                    onClick = {
                        if (TravelRules.isUnlocked(area, settings.visitedAreas)) selectedArea = area.index
                        else state.show(Overlay.LockedArea(area.index))
                    },
                )
            }
            item {
                Text(
                    "Preview rule: the first location of each region is open; the next opens after you visit the previous one. Final unlock requirements arrive with the progression engine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.textSecondary,
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                )
            }
        }
        // Pinned footer; the list above ends before it so nothing is covered.
        Surface(color = p.background, shadowElevation = 6.dp) {
            PrimaryButton(
                text = when {
                    chosen == null -> "Select a location"
                    chosen.index == settings.currentAreaIndex -> "Travel here · already here"
                    else -> "Travel to ${chosen.name}"
                },
                onClick = { chosen?.let(state::travelTo) },
                enabled = chosenUnlocked,
                icon = Icons.Filled.Explore,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding, vertical = Dimens.gapSmall),
            )
        }
    }
}

@Composable
private fun RegionChip(group: RegionGroup, selected: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    val p = Z.colors
    Column(
        Modifier
            .heightIn(min = Dimens.touchTarget)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.accentDark else p.surface)
            .border(1.dp, if (selected) p.accentDark else p.border, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(group.tradition, style = MaterialTheme.typography.labelLarge, color = if (selected) p.onAccent else p.textPrimary, maxLines = 1)
        Text(
            if (isCurrent) "Group ${group.index + 1} · current" else "Group ${group.index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) p.onAccent.copy(alpha = 0.9f) else p.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun AreaRow(area: Area, selected: Boolean, isCurrent: Boolean, visited: Boolean, unlocked: Boolean, onClick: () -> Unit) {
    val p = Z.colors
    ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth(), selected = selected, onClick = onClick) {
        Row(
            Modifier.heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtworkSlot(ArtKey.LocationHero(area), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f)) {
                Text(area.name, style = MaterialTheme.typography.titleMedium, color = if (unlocked) p.textPrimary else p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        isCurrent -> "Current location"
                        !unlocked -> "Locked · tap for requirements"
                        visited -> "Visited · location ${area.stage + 1} of 4"
                        else -> "Open · location ${area.stage + 1} of 4"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.textSecondary,
                )
            }
            when {
                isCurrent -> Box(Modifier.size(32.dp).clip(CircleShape).background(DarkPalette.success), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Current", tint = Color(0xFF0B2416), modifier = Modifier.size(18.dp))
                }
                !unlocked -> Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = p.textSecondary)
                selected -> Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = p.accent)
            }
        }
    }
}
