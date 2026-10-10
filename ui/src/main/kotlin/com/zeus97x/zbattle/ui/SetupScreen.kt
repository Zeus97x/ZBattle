package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.MasterGender
import com.zeus97x.zbattle.core.MasterStyle
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.Starters

/** First-run "Begin your journey": Pet Master name, cosmetic style/gender and starter companion. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(state: AppState) {
    val p = Z.colors
    var name by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf(MasterStyle.Ranger) }
    var gender by rememberSaveable { mutableStateOf(MasterGender.Male) }
    var starterId by rememberSaveable { mutableStateOf<String?>(null) }
    val master = PetMaster.create(name, style, gender, starterId)
    val nameError = name.isNotEmpty() && PetMaster.validName(name) == null

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Begin your journey", subtitle = "Choose your Pet Master. Every style can learn every skill.")
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtworkSlot(
                        ArtKey.Avatar("${style.id}-${gender.id}"),
                        contentDescription = "${style.label} ${gender.label} Pet Master",
                        modifier = Modifier.size(88.dp).clip(CircleShape).border(2.dp, p.accent, CircleShape),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name.trim().ifEmpty { "Your Pet Master" }, style = MaterialTheme.typography.titleLarge, color = p.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${style.label} · ${gender.label} · Level 1", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                        Text("Pet Master illustrations arrive with the approved art phase.", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PetMaster.MAX_NAME_LENGTH + 1) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Pet Master name") },
                    isError = nameError,
                    supportingText = { Text(if (nameError) "Use 1–${PetMaster.MAX_NAME_LENGTH} characters." else "Saved on this device only.") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
                        focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                        focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
                        focusedLabelColor = p.accent, unfocusedLabelColor = p.textSecondary,
                        unfocusedSupportingTextColor = p.textSecondary, focusedSupportingTextColor = p.textSecondary,
                        cursorColor = p.accent,
                    ),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading("Style")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MasterStyle.entries.forEach { s -> ZFilterChip(s.label, s == style) { style = s } }
                    }
                    SectionHeading("Gender")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MasterGender.entries.forEach { g -> ZFilterChip(g.label, g == gender) { gender = g } }
                    }
                    Text("Appearance is cosmetic: all styles share skills and equipment.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                }
            }
            item { SectionHeading("Starter companion") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                    Starters.creatures.forEach { creature ->
                        val selected = creature.id == starterId
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Dimens.cardRadius))
                                .background(if (selected) p.elevated else p.surface)
                                .border(if (selected) 2.dp else 1.dp, if (selected) p.accent else p.border, RoundedCornerShape(Dimens.cardRadius))
                                .selectable(selected = selected, role = Role.RadioButton) { starterId = creature.id }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ArtworkSlot(ArtKey.CreatureArt(creature), contentDescription = creature.name, modifier = Modifier.fillMaxWidth().size(96.dp))
                            Text(creature.name, style = MaterialTheme.typography.labelLarge, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(creature.family.label, style = MaterialTheme.typography.labelSmall, color = p.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        PrimaryButton(
            text = if (master == null) "Add a name and companion" else "Start adventure",
            onClick = { master?.let(state::completeSetup) },
            enabled = master != null,
            icon = Icons.Filled.Bolt,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.gapSmall),
        )
    }
}
