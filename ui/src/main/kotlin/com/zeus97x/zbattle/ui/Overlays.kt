package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.MasterGender
import com.zeus97x.zbattle.core.MasterStyle
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.TravelRules
import com.zeus97x.zbattle.core.battle.CompanionOrigin
import com.zeus97x.zbattle.core.battle.Encounters
import com.zeus97x.zbattle.core.battle.EvolutionRules
import com.zeus97x.zbattle.core.battle.FormGraph
import com.zeus97x.zbattle.core.battle.OwnedCreature
import com.zeus97x.zbattle.core.battle.Skills

/** Shows only catalogue facts; unknown stats are omitted rather than invented. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatureDetailSheet(state: AppState, creatureId: String) {
    val p = Z.colors
    val creature = CreatureCatalog.byId(creatureId) ?: return
    BottomSheet(onDismiss = state::dismissOverlay, title = creature.name) {
        ArtworkSlot(
            ArtKey.CreatureArt(creature),
            contentDescription = "${creature.name} artwork",
            placeholderLabel = if (creature.hasArtwork) null else "Artwork pending — will come from the approved art phase",
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(Dimens.cardRadius)).background(p.elevated),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(creature.stage.label)
            Pill(creature.family.label)
        }
        DetailLine("Tradition", creature.family.tradition)
        DetailLine("Evolution line", creature.family.forms.joinToString(" · "))
        Text(creature.family.lore, style = MaterialTheme.typography.bodyLarge, color = p.textPrimary)
        val owned = state.settings.progress.ownsSpecies(creature.id)
        if (owned != null) {
            val stats = owned.stats
            val skill = Skills.forFamily(creature.family.index)
            Text("Your companion · Level ${owned.level}", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            XpRow(owned.xp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("HP ${stats.maxHp}")
                Pill("Power ${stats.power}")
                Pill("Guard ${stats.guard}")
                Pill("Speed ${stats.speed}")
            }
            DetailLine("Skill", "${skill.name} · ${skill.effect.label}")
            DetailLine("Species", "${creature.family.label} · ${OwnedCreature.RARITY_NAMES[owned.rarity]} (${owned.speciesId})")
            DetailLine(
                "Evolution",
                when {
                    owned.origin == CompanionOrigin.ZPet -> "Form managed by ZPet · kept as imported until cross-app delivery is live"
                    EvolutionRules.availableCombatEvolutions(owned).isNotEmpty() -> "Ready to evolve"
                    FormGraph.nextForms(owned.formIndex).isEmpty() -> "Final form"
                    else -> "Requirements pending approval · identity, rarity, branch, nickname and XP are kept when it evolves"
                },
            )
            val progress = state.settings.progress
            val inParty = progress.inParty(owned.uid)
            val isLead = progress.lead?.uid == owned.uid
            Text(
                when {
                    isLead -> "Party lead · fights first"
                    inParty -> "In your party (${progress.partyMembers.size}/3)"
                    else -> "Not in your party (${progress.partyMembers.size}/3)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = p.textSecondary,
            )
            if (progress.active == null && progress.creatures.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val full = Modifier.fillMaxWidth()
                    if (inParty && !isLead) SecondaryButton("Make lead", onClick = { state.makeLead(owned.uid) }, modifier = full)
                    if (inParty && progress.partyMembers.size > 1) SecondaryButton("Remove from party", onClick = { state.toggleParty(owned.uid) }, modifier = full)
                    if (!inParty && progress.partyMembers.size < 3) SecondaryButton("Add to party", onClick = { state.toggleParty(owned.uid) }, modifier = full)
                }
            }
        } else {
            Text("Not in your party. Catching with ZCubes and the one-way ZPet import are planned.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val p = Z.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = p.textPrimary)
    }
}

@Composable
fun ConfirmChallengeDialog(state: AppState, overlay: Overlay.ConfirmChallenge) {
    val area = RegionCatalog.area(overlay.areaIndex)
    val encounter = Encounters.find(overlay.areaIndex, overlay.opponentSlot)
    if (encounter == null) {
        val opponent = PreviewContent.opponents(area)[overlay.opponentSlot]
        ZDialog(
            title = "${opponent.label} is a preview",
            message = "This opponent has no battle yet. Real battles start with Wild Voltmaw at Olympian Foothills; rosters for every area arrive in a later task.",
            confirm = "OK" to state::dismissOverlay,
            onDismissRequest = state::dismissOverlay,
        )
        return
    }
    val lead = state.settings.ownedParty.firstOrNull()
    val rematch = encounter.id in state.settings.progress.defeated
    ZDialog(
        title = "Challenge ${encounter.label}?",
        message = buildString {
            val others = state.settings.ownedParty.size - 1
            append("${lead?.creature?.name ?: "Your companion"} (Lv ${lead?.level ?: 1})")
            if (others > 0) append(" + $others in reserve")
            append(" vs ${encounter.creature.name} (Lv ${encounter.level}) at ${area.name}. ")
            append(if (rematch) "Replay: reduced rewards, awaiting approval (0 XP for now). No first-clear rewards repeat." else "First victory: +${encounter.firstWinXp} XP.")
        },
        confirm = "Battle" to { state.startBattle(encounter) },
        dismiss = "Cancel" to state::dismissOverlay,
        onDismissRequest = state::dismissOverlay,
    )
}

@Composable
fun ConfirmRetreatDialog(state: AppState) {
    ZDialog(
        title = "Retreat from battle?",
        message = "Retreating ends this battle with no rewards. You can challenge again any time.",
        confirm = "Retreat" to { state.retreatBattle() },
        dismiss = "Keep battling" to state::dismissOverlay,
        onDismissRequest = state::dismissOverlay,
    )
}

@Composable
fun LockedAreaDialog(state: AppState, areaIndex: Int) {
    val area = RegionCatalog.area(areaIndex)
    ZDialog(
        title = "${area.name} is locked",
        message = TravelRules.requirement(area),
        confirm = "OK" to state::dismissOverlay,
        onDismissRequest = state::dismissOverlay,
    )
}

@Composable
fun NoticeDialog(state: AppState, overlay: Overlay.Notice) {
    ZDialog(overlay.title, overlay.message, confirm = "OK" to state::dismissOverlay, onDismissRequest = state::dismissOverlay)
}

/** Name and cosmetic appearance can change; the starter companion stays as chosen at setup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditMasterDialog(state: AppState) {
    val p = Z.colors
    val current = state.settings.master ?: return
    var text by rememberSaveable { mutableStateOf(current.name) }
    var style by rememberSaveable { mutableStateOf(current.style) }
    var gender by rememberSaveable { mutableStateOf(current.gender) }
    val updated = PetMaster.create(text, style, gender, current.starterId)
    AlertDialog(
        onDismissRequest = state::dismissOverlay,
        containerColor = p.surface,
        title = { Text("Edit Pet Master", color = p.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it.take(PetMaster.MAX_NAME_LENGTH + 1) }, singleLine = true, isError = updated == null, label = { Text("Name") })
                Text(
                    if (updated == null) "Use 1–${PetMaster.MAX_NAME_LENGTH} characters." else "Saved on this device only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (updated == null) MaterialTheme.colorScheme.error else p.textSecondary,
                )
                Text("Style", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MasterStyle.entries.forEach { s -> ZFilterChip(s.label, s == style) { style = s } }
                }
                Text("Gender", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MasterGender.entries.forEach { g -> ZFilterChip(g.label, g == gender) { gender = g } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = updated != null, onClick = {
                updated?.let { m -> state.updateSettings { it.copy(master = m) } }
                state.dismissOverlay()
            }) { Text("Save", color = if (updated != null) p.accent else p.textSecondary) }
        },
        dismissButton = { TextButton(onClick = state::dismissOverlay) { Text("Cancel", color = p.textPrimary) } },
    )
}

@Composable
private fun ZDialog(
    title: String,
    message: String,
    confirm: Pair<String, () -> Unit>,
    onDismissRequest: () -> Unit,
    dismiss: Pair<String, () -> Unit>? = null,
) {
    val p = Z.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = p.surface,
        title = { Text(title, color = p.textPrimary) },
        text = { Text(message, color = p.textSecondary, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = confirm.second) { Text(confirm.first, color = p.accent) } },
        dismissButton = dismiss?.let { (label, action) -> { TextButton(onClick = action) { Text(label, color = p.textPrimary) } } },
    )
}
