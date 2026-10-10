package com.zeus97x.zbattle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.RegionCatalog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Battle layout with explicit preview state; no damage, victories or rewards are recorded. */
@Composable
fun BattleScreen(state: AppState, areaIndex: Int, opponentSlot: Int) {
    val p = Z.colors
    val area = RegionCatalog.area(areaIndex)
    val opponent = PreviewContent.opponents(area)[opponentSlot]
    val party = state.settings.party
    var activeIndex by rememberSaveable { mutableStateOf(0) }
    var switching by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("Preview battle · the battle engine is not built yet, so actions change nothing.") }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val animate = state.settings.battleAnimations

    fun previewAction(label: String) {
        status = "$label preview · no damage applied. Battle rules arrive with the battle-engine task."
        if (animate) scope.launch {
            for (target in listOf(-10f, 10f, -6f, 0f)) shake.animateTo(target, tween(70))
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Battle", subtitle = "${area.name} · ${opponent.label}", onBack = { state.show(Overlay.ConfirmRetreat) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                ArtworkSlot(ArtKey.LocationBattle(area), contentDescription = "${area.name} battle scenery", modifier = Modifier.fillMaxSize(), placeholderAlignment = Alignment.TopCenter)
                Column(Modifier.align(Alignment.TopStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreviewBadge()
                    if (rememberArt(ArtKey.LocationBattle(area)) == null) Pill("Scenery pending")
                }
                Column(Modifier.align(Alignment.TopEnd).padding(12.dp).fillMaxWidth(0.55f)) {
                    HealthPanel(opponent.party.first().name, "Opponent")
                }
                ArtworkSlot(
                    ArtKey.CreatureArt(opponent.party.first()),
                    contentDescription = "Opponent ${opponent.party.first().name}",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, top = 40.dp).size(130.dp)
                        .offset { IntOffset(shake.value.roundToInt(), 0) },
                )
                ArtworkSlot(
                    ArtKey.CreatureArt(party[activeIndex]),
                    contentDescription = "Your ${party[activeIndex].name}",
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 52.dp).size(140.dp),
                )
                Column(Modifier.align(Alignment.BottomEnd).padding(12.dp).fillMaxWidth(0.55f)) {
                    HealthPanel(party[activeIndex].name, "Your creature")
                }
            }
            Column(Modifier.padding(Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Your turn · preview", container = p.accentDark, content = p.onAccent, icon = Icons.Filled.Bolt)
                    Pill("Turn 1")
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                Text("Party", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    party.forEachIndexed { index, creature ->
                        val selected = index == activeIndex
                        Box(
                            Modifier.selectable(selected = selected, enabled = switching, role = Role.RadioButton) {
                                activeIndex = index
                                switching = false
                                status = "Switched to ${creature.name} (preview only)."
                            },
                        ) {
                            CircleThumb(creature, 56.dp, ring = if (selected) p.accent else if (switching) p.textSecondary else null)
                        }
                    }
                }
                if (switching) Text("Choose a party creature to switch in.", style = MaterialTheme.typography.bodyMedium, color = p.accent)
            }
        }
        // Bottom action grid: Attack / Skill / Switch / Retreat.
        Column(Modifier.background(p.surface).padding(Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Attack", Icons.Filled.Bolt, Modifier.weight(1f), primary = true) { previewAction("Attack") }
                ActionButton("Skill", Icons.Filled.AutoAwesome, Modifier.weight(1f), primary = true) { previewAction("Skill") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Switch", Icons.Filled.SwapHoriz, Modifier.weight(1f)) {
                    if (party.size > 1) switching = !switching
                    else status = "Only one companion so far · more arrive with encounters and ZPet import."
                }
                ActionButton("Retreat", Icons.Filled.DirectionsRun, Modifier.weight(1f)) { state.show(Overlay.ConfirmRetreat) }
            }
        }
    }
}

@Composable
private fun HealthPanel(name: String, role: String) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xCC141720)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LinearProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = Color(0xFF49B87C),
            trackColor = Color(0xFF414555),
        )
        Text("$role · HP preview", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD9D6DE), maxLines = 1)
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val p = Z.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.primaryButtonHeight),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) p.accentDark else p.elevated,
            contentColor = if (primary) p.onAccent else p.textPrimary,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("  $text", style = MaterialTheme.typography.titleMedium)
    }
}

