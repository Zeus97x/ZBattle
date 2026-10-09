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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.PreviewOpponent
import com.zeus97x.zbattle.core.RegionCatalog

@Composable
fun ChallengesScreen(state: AppState, areaIndex: Int) {
    val p = Z.colors
    val area = RegionCatalog.area(areaIndex)
    val opponents = PreviewContent.opponents(area)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Area progress", style = MaterialTheme.typography.titleMedium, color = p.textPrimary, modifier = Modifier.weight(1f))
                            PreviewBadge(text = "Planned")
                        }
                        // Separate indicators; thresholds come from the later gameplay task.
                        ProgressRow("Steps walked", "0 / threshold pending", fraction = 0f, planned = true)
                        ProgressRow("Region boss wins", "0 / threshold pending", fraction = 0f, planned = true)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading("Opponents", Modifier.weight(1f))
                    PreviewBadge()
                }
            }
            items(opponents.filterNot { it.isBoss }, key = { it.slot }) { opponent ->
                OpponentCard(opponent, onChallenge = { state.show(Overlay.ConfirmChallenge(area.index, opponent.slot)) })
            }
            item { SectionHeading("Region Boss") }
            items(opponents.filter { it.isBoss }, key = { it.slot }) { boss ->
                OpponentCard(boss, onChallenge = { state.show(Overlay.ConfirmChallenge(area.index, boss.slot)) })
            }
            item {
                Text(
                    "Opponent names, portraits, parties and rewards are placeholders. Creatures shown are existing ZPet designs rotated for layout only.",
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
                        Pill("Not completed", icon = Icons.Filled.EmojiEvents)
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

