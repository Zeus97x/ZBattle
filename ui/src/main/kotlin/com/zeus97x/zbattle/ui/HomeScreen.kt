package com.zeus97x.zbattle.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route

@Composable
fun HomeScreen(state: AppState) {
    val p = Z.colors
    val area = state.settings.currentArea
    val party = state.settings.ownedParty
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.gapLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
    ) {
        item { HomeHeader(state) }
        item { LocationCarousel(state, area) }
        item {
            Column(Modifier.padding(horizontal = Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                PrimaryButton("Battle", onClick = { state.navigate(Route.Challenges(area.index)) }, icon = Icons.Filled.Bolt, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Double Battle", onClick = { state.navigate(Route.DoubleBattle(area.index)) }, icon = Icons.Filled.Groups, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            SectionHeading("My Party", Modifier.padding(horizontal = Dimens.screenPadding), trailing = "${party.size}/3")
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Dimens.screenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                items(party, key = { it.uid }) { owned ->
                    CreatureCard(
                        owned.creature,
                        modifier = Modifier.width(144.dp),
                        onClick = { state.show(Overlay.CreatureDetail(owned.creatureId)) },
                        caption = "Lv ${owned.level} · ${owned.xp % 100}/100 XP",
                    )
                }
            }
        }
        item {
            Text(
                "More companions arrive with ZCubes catching and the planned one-way ZPet import. Battle progress never returns to ZPet.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.textSecondary,
                modifier = Modifier.padding(horizontal = Dimens.screenPadding),
            )
        }
    }
}

@Composable
private fun HomeHeader(state: AppState) {
    val p = Z.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(p.headerGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Dimens.screenPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArtworkSlot(ArtKey.BrandLogo, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x33141720)))
            Text(
                "ZBattle",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.onAccent,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column {
            Text("Welcome, ${state.settings.displayName}", style = MaterialTheme.typography.titleMedium, color = p.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            state.settings.master?.let { master ->
                Text("${master.appearanceLabel} · Level 1", style = MaterialTheme.typography.bodyMedium, color = p.onAccent.copy(alpha = 0.92f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // XP here is still a placeholder; coins are the real balance (CLAUDE-006 C1).
            StatChip(Icons.Filled.Star, "0", "XP")
            StatChip(Icons.Filled.MonetizationOn, formatCoins(state.settings.progress.inventory.coins), "coins")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationCarousel(state: AppState, current: Area) {
    val p = Z.colors
    val group = current.group
    val pager = rememberPagerState(initialPage = current.stage) { group.areas.size }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = Dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
            SectionHeading("Current region", Modifier.weight(1f))
            Text("Group ${group.index + 1} of ${RegionCatalog.groups.size}", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
        }
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 12.dp,
            key = { group.areas[it].index },
        ) { page ->
            LocationCard(
                area = group.areas[page],
                isCurrent = group.areas[page] == current,
                onTravel = { state.navigate(Route.Travel(group.index)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(group.areas.size) { index ->
                val active = index == pager.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) p.accent else p.border),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
/** Full-bleed scenery card with region chip, Travel pill and lower scrim text. */
@Composable
fun LocationCard(area: Area, isCurrent: Boolean, onTravel: () -> Unit, modifier: Modifier = Modifier) {
    val p = Z.colors
    // Grows with font scale so the title and description keep room over the scrim.
    val height = (240 * LocalDensity.current.fontScale.coerceIn(1f, 1.4f)).dp
    val hasScenery = rememberArt(ArtKey.LocationHero(area)) != null
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(p.surface),
    ) {
        ArtworkSlot(ArtKey.LocationHero(area), contentDescription = "${area.name} scenery", modifier = Modifier.fillMaxSize())
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Pill(area.group.tradition, container = Color(0xE6F5F4F7), content = Color(0xFF141720), icon = Icons.Filled.Place, modifier = Modifier.weight(1f, fill = false))
            Row(
                Modifier
                    .heightIn(min = Dimens.touchTarget)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(role = Role.Button, onClick = onTravel)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill("Travel", container = p.accentDark, content = p.onAccent, icon = Icons.Filled.Explore)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9141720))))
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isCurrent) Pill("You are here", container = DarkPalette.success, content = Color(0xFF0B2416))
                if (!hasScenery) Pill("Scenery pending", container = Color(0x66141720), content = Color(0xFFE6E3EA))
            }
            Text(area.name, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(RegionCatalog.scenery(area), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE6E3EA), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
