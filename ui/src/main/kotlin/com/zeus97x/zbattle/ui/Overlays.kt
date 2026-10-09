package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.zeus97x.zbattle.core.PlayerSettings
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.TravelRules

/** Shows only catalogue facts; unknown stats are omitted rather than invented. */
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
        Text("Battle stats, levels and ownership are not defined yet and are intentionally not shown.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
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
    val opponent = PreviewContent.opponents(area)[overlay.opponentSlot]
    ZDialog(
        title = "Challenge ${opponent.label}?",
        message = "Opens the battle preview at ${area.name}. No results, rewards or unlocks are recorded.",
        confirm = "Start preview" to { state.navigate(Route.Battle(overlay.areaIndex, overlay.opponentSlot)) },
        dismiss = "Cancel" to state::dismissOverlay,
        onDismissRequest = state::dismissOverlay,
    )
}

@Composable
fun ConfirmRetreatDialog(state: AppState) {
    ZDialog(
        title = "Retreat from battle?",
        message = "You will return to the challenge list. Nothing is lost in the preview.",
        confirm = "Retreat" to { state.popTo { it is Route.Challenges } },
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

@Composable
fun EditNameDialog(state: AppState) {
    val p = Z.colors
    var text by rememberSaveable { mutableStateOf(state.settings.displayName) }
    val valid = PlayerSettings.validName(text)
    AlertDialog(
        onDismissRequest = state::dismissOverlay,
        containerColor = p.surface,
        title = { Text("Display name", color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it.take(PlayerSettings.MAX_NAME_LENGTH + 1) }, singleLine = true, isError = valid == null)
                Text(
                    if (valid == null) "Use 1–${PlayerSettings.MAX_NAME_LENGTH} characters." else "Saved on this device only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (valid == null) MaterialTheme.colorScheme.error else p.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid != null, onClick = {
                valid?.let { name -> state.updateSettings { it.copy(displayName = name) } }
                state.dismissOverlay()
            }) { Text("Save", color = if (valid != null) p.accent else p.textSecondary) }
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
