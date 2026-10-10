package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.zeus97x.zbattle.core.RegionCatalog

@Composable
fun DoubleBattleScreen(state: AppState, areaIndex: Int) {
    val p = Z.colors
    val area = RegionCatalog.area(areaIndex)
    val lineup = PreviewContent.doubleLineup(area)
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.gap),
        verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
    ) {
        item { AppHeader(title = "Double Battle", subtitle = "${area.name} · ${area.group.tradition}", onBack = { state.back() }) }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Two creatures per side", style = MaterialTheme.typography.titleMedium, color = p.textPrimary, modifier = Modifier.weight(1f))
                        PreviewBadge()
                    }
                    Text("Both of your lead creatures act each turn against two opponents. Rules, turn order and rewards are planned for the battle-engine task.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                }
            }
        }
        item {
            ZCard(Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth()) {
                Row(Modifier.padding(Dimens.cardPadding), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtworkSlot(ArtKey.Opponent("double-challenger"), contentDescription = null, modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(p.elevated))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Challenger · pending", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Redeem, contentDescription = null, tint = p.accent)
                            Text("Reward preview pending economy approval", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                        }
                    }
                }
            }
        }
        item { SectionHeading("Opposing lineup", Modifier.padding(horizontal = Dimens.screenPadding), trailing = "${lineup.size}") }
        // 3 × 2 grid of existing ZPet creatures (rotation for layout only).
        lineup.chunked(3).forEach { row ->
            item {
                Row(Modifier.padding(horizontal = Dimens.screenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                    row.forEach { creature ->
                        ZCard(Modifier.weight(1f), onClick = { state.show(Overlay.CreatureDetail(creature.id)) }) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtworkSlot(ArtKey.CreatureArt(creature), contentDescription = creature.name, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                                Text(creature.name, style = MaterialTheme.typography.labelSmall, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeading("Your party", Modifier.padding(horizontal = Dimens.screenPadding), trailing = "Needs 2 companions") }
        item {
            val party = state.settings.party
            Row(Modifier.padding(horizontal = Dimens.screenPadding), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                party.forEach { creature ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircleThumb(creature, 60.dp, ring = p.accent)
                        Text("Lead", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                }
                repeat((2 - party.size).coerceAtLeast(0)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ArtPlaceholder(com.zeus97x.zbattle.core.PlaceholderStyle.Creature, contentDescription = "Open slot", label = null, modifier = Modifier.size(60.dp).clip(androidx.compose.foundation.shape.CircleShape))
                        Text("Open slot", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                }
            }
        }
        item {
            PrimaryButton(
                "Challenge",
                onClick = { state.show(Overlay.Notice("Double battles are planned", "The double-battle engine is not built yet. This screen previews the layout only; no battle, rewards or progress are recorded.")) },
                icon = Icons.Filled.Groups,
                modifier = Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth(),
            )
        }
    }
}
