package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route

@Composable
fun ProfileScreen(state: AppState) {
    val p = Z.colors
    val settings = state.settings
    var progressGroup by rememberSaveable { mutableStateOf(settings.currentArea.groupIndex) }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.gapLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
    ) {
        item { AppHeader(title = "Profile", subtitle = "Stored on this device only") }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                Row(Modifier.padding(Dimens.cardPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ArtworkSlot(
                        settings.master?.avatarKey ?: ArtKey.Avatar("default"),
                        contentDescription = "Pet Master avatar",
                        modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, p.accent, CircleShape),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(settings.displayName, style = MaterialTheme.typography.headlineSmall, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        settings.master?.let { m ->
                            Text("${m.appearanceLabel} · Level 1", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("First companion: ${m.starter.name}", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Current: ${settings.currentArea.name}", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { state.show(Overlay.EditMaster) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Pet Master", tint = p.accent)
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressRow("Locations visited", "${settings.visitedAreas.size} / ${RegionCatalog.areas.size}", settings.visitedAreas.size / RegionCatalog.areas.size.toFloat())
                ProgressRow("ZPet creatures with artwork", "${CreatureCatalog.created.size} / ${CreatureCatalog.all.size}", null)
            }
        }
        item { SectionHeading("Region progress", Modifier.padding(horizontal = Dimens.screenPadding)) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Dimens.screenPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RegionCatalog.groups, key = { it.id }) { g ->
                    ZFilterChip("${g.tradition} · ${g.index + 1}", g.index == progressGroup) { progressGroup = g.index }
                }
            }
        }
        item {
            val group = RegionCatalog.groups[progressGroup]
            Row(Modifier.padding(horizontal = Dimens.screenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                group.areas.forEach { area ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ArtworkSlot(
                            ArtKey.Badge(area.id),
                            contentDescription = "${area.name} medallion",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(p.surface)
                                .border(1.dp, if (area.index in settings.visitedAreas) p.success else p.border, CircleShape),
                        )
                        Text(area.name, style = MaterialTheme.typography.labelSmall, color = p.textSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            Text("Medallions are planned; outlines turn green for visited locations.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, modifier = Modifier.padding(horizontal = Dimens.screenPadding))
        }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Filled.EmojiEvents, "Achievements", "Planned list", onClick = { state.navigate(Route.Achievements) })
                    Hairline()
                    SettingsRow(Icons.Filled.Construction, "Skills and equipment", "Planned · every style shares the same skills and equipment")
                    Hairline()
                    SettingsRow(Icons.Filled.Pets, "Pet Master tasks", "Planned · gathering, crafting and companion expeditions; no AFK rewards yet")
                    Hairline()
                    SettingsRow(Icons.Filled.SyncAlt, "ZPet connection", "Planned · copy a pet from ZPet with separate battle progress. ZBattle rewards never return to ZPet.")
                }
            }
        }
        item { SectionHeading("Settings", Modifier.padding(horizontal = Dimens.screenPadding)) }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Filled.MusicNote, "Music", "Saved preference · audio is planned, nothing plays yet", checked = settings.music, onCheckedChange = { v -> state.updateSettings { it.copy(music = v) } })
                    Hairline()
                    SettingsRow(Icons.Filled.DarkMode, "Dark Mode", "Applies immediately", checked = settings.darkMode, onCheckedChange = { v -> state.updateSettings { it.copy(darkMode = v) } })
                    Hairline()
                    SettingsRow(Icons.Filled.Animation, "Battle Animations", "Hit shake in battle preview", checked = settings.battleAnimations, onCheckedChange = { v -> state.updateSettings { it.copy(battleAnimations = v) } })
                }
            }
        }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                SettingsRow(Icons.Filled.AccountCircle, "Account", "Local profile only · no sign-in required.")
            }
        }
    }
}

@Composable
fun AchievementsScreen(state: AppState) {
    val settings = state.settings
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Achievements", onBack = { state.back() })
        LazyColumn(contentPadding = PaddingValues(Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
            item {
                EmptyState(
                    ArtKey.Badge("achievements"),
                    "Achievements are planned",
                    "The achievement list will be defined with the progression task. Your real local progress so far:",
                )
            }
            item {
                ZCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProgressRow("Locations visited", "${settings.visitedAreas.size} / ${RegionCatalog.areas.size}", settings.visitedAreas.size / RegionCatalog.areas.size.toFloat())
                        RegionCatalog.groups.forEach { g ->
                            val visited = g.areas.count { it.index in settings.visitedAreas }
                            ProgressRow("${g.tradition} · group ${g.index + 1}", "$visited / 4", visited / 4f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventsScreen() {
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Events")
        EmptyState(
            ArtKey.Event("none"),
            "No events yet",
            "Events are planned. Nothing is running, and no limited-time content is available.",
        )
    }
}
