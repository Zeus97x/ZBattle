package com.zeus97x.zbattle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.ArtworkFilter
import com.zeus97x.zbattle.core.CollectionSort
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.StageFilter

@Composable
fun CollectionScreen(state: AppState) {
    val p = Z.colors
    val query = state.collectionQuery
    val results = query.apply()
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val columns = if (LocalDensity.current.fontScale >= 1.5f) GridCells.Fixed(1) else GridCells.Adaptive(150.dp)

    Column(Modifier.fillMaxSize()) {
        AppHeader(
            title = "Collection",
            subtitle = "${CreatureCatalog.created.size} created ZPet creatures · ${CreatureCatalog.pending.size} forms awaiting artwork",
        )
        LazyVerticalGrid(
            columns = columns,
            state = rememberLazyGridState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                    OutlinedTextField(
                        value = query.search,
                        onValueChange = { state.collectionQuery = query.copy(search = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search by name") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = if (query.search.isNotEmpty()) {
                            { IconButton(onClick = { state.collectionQuery = query.copy(search = "") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") } }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
                            focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                            focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
                            focusedLeadingIconColor = p.textSecondary, unfocusedLeadingIconColor = p.textSecondary,
                            focusedPlaceholderColor = p.textSecondary, unfocusedPlaceholderColor = p.textSecondary,
                            cursorColor = p.accent,
                        ),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortMenu(query.sort) { state.collectionQuery = query.copy(sort = it) }
                        TextButton(onClick = { filtersOpen = !filtersOpen }, modifier = Modifier.heightIn(min = Dimens.touchTarget)) {
                            Icon(Icons.Filled.FilterList, contentDescription = null, tint = p.accent)
                            Text(
                                if (query.activeFilterCount > 0) " Filters (${query.activeFilterCount})" else " Filters",
                                color = p.textPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Box(Modifier.weight(1f))
                        Text("${results.size} shown", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                    }
                    if (filtersOpen) FilterPanel(state)
                    ProgressRow(
                        label = "Forms with created artwork",
                        value = "${CreatureCatalog.created.size} / ${CreatureCatalog.all.size}",
                        fraction = CreatureCatalog.created.size / CreatureCatalog.all.size.toFloat(),
                    )
                    Text(
                        "Owned counts arrive with the one-way ZPet import. Unfinished forms stay as placeholders until ChatGPT's artwork is approved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.textSecondary,
                    )
                }
            }
            if (results.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(ArtKey.Event("collection-empty"), "No matches", "Try another name or clear filters.")
                }
            }
            items(results, key = { it.id }) { creature ->
                CreatureCard(
                    creature,
                    onClick = { state.show(Overlay.CreatureDetail(creature.id)) },
                    caption = if (creature.hasArtwork) creature.family.label else "${creature.family.label} · artwork pending",
                )
            }
        }
    }
}

@Composable
private fun SortMenu(selected: CollectionSort, onSelect: (CollectionSort) -> Unit) {
    val p = Z.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.heightIn(min = Dimens.touchTarget)) {
            Icon(Icons.Filled.Sort, contentDescription = null, tint = p.accent)
            Text(" ${selected.label}", color = p.textPrimary, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CollectionSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = { onSelect(sort); open = false },
                    leadingIcon = if (sort == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                    } else null,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(state: AppState) {
    val p = Z.colors
    val query = state.collectionQuery
    ZCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Artwork", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtworkFilter.entries.forEach { filter ->
                    ZFilterChip(filter.label, query.artwork == filter) { state.collectionQuery = query.copy(artwork = filter) }
                }
            }
            Text("Stage", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageFilter.entries.forEach { stage ->
                    ZFilterChip(stage.label, query.stage == stage) { state.collectionQuery = query.copy(stage = stage) }
                }
            }
            Text("Family", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CreatureCatalog.families.forEach { family ->
                    val on = family.index in query.families
                    ZFilterChip(family.label, on) {
                        state.collectionQuery = query.copy(families = if (on) query.families - family.index else query.families + family.index)
                    }
                }
            }
            if (query.activeFilterCount > 0) {
                TextButton(onClick = { state.collectionQuery = query.copy(families = emptySet(), stage = StageFilter.All, artwork = ArtworkFilter.Created) }) {
                    Text("Reset filters", color = p.accent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = Z.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = p.surface,
            labelColor = p.textPrimary,
            selectedContainerColor = p.accentDark,
            selectedLabelColor = p.onAccent,
            selectedLeadingIconColor = p.onAccent,
        ),
    )
}
