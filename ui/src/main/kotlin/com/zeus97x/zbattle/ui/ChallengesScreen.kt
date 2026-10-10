package com.zeus97x.zbattle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.PreviewOpponent
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.battle.Encounter
import com.zeus97x.zbattle.core.battle.Encounters

@Composable
fun ChallengesScreen(state: AppState, areaIndex: Int) {
    val p = Z.colors
    val area = RegionCatalog.area(areaIndex)
    val opponents = PreviewContent.opponents(area)
    val progress = state.settings.progress
    val realEncounters = Encounters.playable.filter { it.area == area }
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Challenges", subtitle = "${area.name} · ${area.group.tradition}", onBack = { state.back() })
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
        ) {
            item {
                ZCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkSlot(ArtKey.LocationHero(area), contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(area.name, style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                            Text(RegionCatalog.scenery(area), style = MaterialTheme.typography.bodyMedium, color = p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item {
                ZCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Area progress", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                        if (realEncounters.isNotEmpty()) {
                            val beaten = realEncounters.count { it.id in progress.defeated }
                            ProgressRow("Opponents defeated", "$beaten / ${realEncounters.size}", beaten.toFloat() / realEncounters.size)
                        }
                        // Separate indicators; thresholds come from the later progression task.
                        ProgressRow("Steps walked", "0 / threshold pending", fraction = 0f, planned = true)
                        ProgressRow("Region boss wins", "0 / threshold pending", fraction = 0f, planned = true)
                    }
                }
            }
            item { SectionHeading("Opponents") }
            items(opponents.filterNot { it.isBoss }, key = { it.slot }) { opponent ->
                val real = Encounters.find(area.index, opponent.slot)
                if (real != null) {
                    EncounterCard(
                        real,
                        defeated = real.id in progress.defeated,
                        inProgress = progress.active?.encounterId == real.id,
                        onChallenge = {
                            if (progress.active?.encounterId == real.id) state.startBattle(real)
                            else state.show(Overlay.ConfirmChallenge(area.index, opponent.slot))
                        },
                    )
                } else {
                    OpponentCard(opponent, onChallenge = { state.show(Overlay.ConfirmChallenge(area.index, opponent.slot)) })
                }
            }
            item { SectionHeading("Region Boss") }
            items(opponents.filter { it.isBoss }, key = { it.slot }) { boss ->
                OpponentCard(boss, onChallenge = { state.show(Overlay.ConfirmChallenge(area.index, boss.slot)) })
            }
            item {
                Text(
                    if (realEncounters.isEmpty()) "Real battles start at Olympian Foothills in this build. Cards marked Preview are placeholders: names, parties and rewards are not final."
                    else "Cards marked Preview are placeholders: names, parties and rewards are not final. Rosters for all 48 areas arrive in a later task.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.textSecondary,
                )
            }
        }
    }
}

@Composable
fun OpponentCard(opponent: PreviewOpponent, onChallenge: () -> Unit, modifier: Modifier = Modifier) {
    val p = Z.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        color = p.surface,
        border = BorderStroke(if (opponent.isBoss) 2.dp else 1.dp, if (opponent.isBoss) p.accent else p.border),
    ) {
        Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkSlot(
                    opponent.artKey,
                    contentDescription = null,
                    modifier = Modifier.size(if (opponent.isBoss) 76.dp else 60.dp).clip(RoundedCornerShape(16.dp)).background(p.elevated),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(opponent.label, style = MaterialTheme.typography.titleMedium, color = p.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (opponent.isBoss) Pill("Boss", container = p.accentDark, content = p.onAccent, icon = Icons.Filled.Shield)
                        PreviewBadge()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Party", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                opponent.party.forEach { CircleThumb(it, 44.dp) }
            }
            PrimaryButton("Challenge", onClick = onChallenge, icon = Icons.Filled.Bolt, modifier = Modifier.fillMaxWidth())
        }
    }
}


/** A real opponent with stats, first-win reward and completion state. */
@Composable
fun EncounterCard(encounter: Encounter, defeated: Boolean, inProgress: Boolean, onChallenge: () -> Unit, modifier: Modifier = Modifier) {
    val p = Z.colors
    val stats = encounter.stats
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        color = p.surface,
        border = BorderStroke(2.dp, if (defeated) p.success else p.accent),
    ) {
        Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkSlot(
                    ArtKey.CreatureArt(encounter.creature),
                    contentDescription = encounter.creature.name,
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(p.elevated),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(encounter.label, style = MaterialTheme.typography.titleMedium, color = p.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Lv ${encounter.level} · ${encounter.creature.family.label} · HP ${stats.maxHp}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when {
                            inProgress -> Pill("In progress", container = p.accentDark, content = p.onAccent)
                            defeated -> Pill("Defeated", container = DarkPalette.success, content = Color(0xFF0B2416), icon = Icons.Filled.EmojiEvents)
                            else -> Pill("First win +${encounter.firstWinXp} XP", icon = Icons.Filled.EmojiEvents)
                        }
                    }
                }
            }
            PrimaryButton(
                when {
                    inProgress -> "Resume battle"
                    defeated -> "Rematch · practice"
                    else -> "Challenge"
                },
                onClick = onChallenge,
                icon = Icons.Filled.Bolt,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
