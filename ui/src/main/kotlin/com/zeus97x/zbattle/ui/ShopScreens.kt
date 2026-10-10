package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.DemoShopItem
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.PreviewContent
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.ShopCategory
import java.util.Locale

private fun ShopCategory.icon(): ImageVector = when (this) {
    ShopCategory.Equipment -> Icons.Filled.Backpack
    ShopCategory.Consumables -> Icons.Filled.LocalDrink
    ShopCategory.Cosmetics -> Icons.Filled.Palette
}

@Composable
fun ShopCategorySheet(state: AppState) {
    val p = Z.colors
    BottomSheet(onDismiss = state::dismissOverlay, title = "Shop") {
        Text("Categories are provisional groupings; the economy is not approved yet.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
        ShopCategory.entries.forEach { category ->
            ZCard(Modifier.fillMaxWidth(), onClick = { state.navigate(Route.ItemShop(category)) }) {
                Row(
                    Modifier.heightIn(min = 72.dp).padding(horizontal = Dimens.cardPadding, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(p.elevated), contentAlignment = Alignment.Center) {
                        Icon(category.icon(), contentDescription = null, tint = p.accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(category.label, style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                        Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = p.textSecondary)
                }
            }
        }
    }
}

@Composable
fun ItemShopScreen(state: AppState, initialCategory: ShopCategory) {
    val p = Z.colors
    var category by rememberSaveable { mutableStateOf<ShopCategory?>(initialCategory) }
    var search by rememberSaveable { mutableStateOf("") }
    val needle = search.trim().lowercase(Locale.ROOT)
    val items = PreviewContent.shopItems.filter {
        (category == null || it.category == category) && (needle.isEmpty() || it.label.lowercase(Locale.ROOT).contains(needle))
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader(
            title = "Item Shop",
            subtitle = "Demo catalogue · purchases disabled",
            onBack = { state.back() },
            actions = { StatChip(Icons.Filled.MonetizationOn, "0", "coins", Modifier.padding(end = 4.dp)) },
        )
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall),
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search items") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (search.isNotEmpty()) {
                        { IconButton(onClick = { search = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") } }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
                        focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                        focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
                        focusedPlaceholderColor = p.textSecondary, unfocusedPlaceholderColor = p.textSecondary,
                        cursorColor = p.accent,
                    ),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ZFilterChip("All", category == null) { category = null } }
                    items(ShopCategory.entries) { c -> ZFilterChip(c.label, category == c) { category = c } }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading(category?.label ?: "All items", Modifier.weight(1f))
                    PreviewBadge(text = "Demo")
                }
            }
            if (items.isEmpty()) {
                item { EmptyState(ArtKey.Item("none"), "No items", "Try another search or category.") }
            }
            items(items, key = { it.id }) { item ->
                ShopItemCard(item, onBuy = {
                    state.show(Overlay.Notice("Purchases unavailable", "${item.label} is demo content. Buying is disabled until the ZBattle economy and items are approved."))
                })
            }
        }
    }
}

@Composable
fun ShopItemCard(item: DemoShopItem, onBuy: () -> Unit, modifier: Modifier = Modifier) {
    val p = Z.colors
    ZCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtworkSlot(ArtKey.Item(item.id), contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.category.label, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                Text("Price pending", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
            }
            Button(
                onClick = onBuy,
                modifier = Modifier.heightIn(min = Dimens.touchTarget),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.elevated, contentColor = p.textPrimary),
            ) { Text("Buy") }
        }
    }
}

