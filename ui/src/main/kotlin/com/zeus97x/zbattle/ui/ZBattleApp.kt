package com.zeus97x.zbattle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.Tab

/** Root composable used by the Android activity and the layout harness alike. */
@Composable
fun ZBattleApp(state: AppState) {
    ZBattleTheme(dark = state.settings.darkMode) {
        val p = Z.colors
        val holder = rememberSaveableStateHolder()
        Box(Modifier.fillMaxSize().background(p.background)) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val route = state.nav.current
                    // Keeps each destination's scroll position while it stays reachable.
                    holder.SaveableStateProvider(route.toString()) {
                        RouteContent(route, state)
                    }
                }
                // Battle is immersive: the header back arrow / Retreat exit it, so the bar is hidden.
                if (state.nav.current !is Route.Battle) {
                    BottomBar(selected = state.nav.selectedTab, onSelect = state::selectTab)
                }
            }
            OverlayHost(state)
        }
    }
}

@Composable
private fun RouteContent(route: Route, state: AppState) {
    when (route) {
        Route.Home -> HomeScreen(state)
        Route.Collection -> CollectionScreen(state)
        Route.Events -> EventsScreen()
        Route.Profile -> ProfileScreen(state)
        Route.Achievements -> AchievementsScreen(state)
        is Route.Travel -> TravelScreen(state, route.focusGroup)
        is Route.Challenges -> ChallengesScreen(state, route.areaIndex)
        is Route.Battle -> BattleScreen(state, route.areaIndex, route.opponentSlot)
        is Route.DoubleBattle -> DoubleBattleScreen(state, route.areaIndex)
        is Route.ItemShop -> ItemShopScreen(state, route.category)
    }
}

@Composable
private fun OverlayHost(state: AppState) {
    when (val overlay = state.nav.overlay) {
        null -> Unit
        Overlay.ShopSheet -> ShopCategorySheet(state)
        is Overlay.CreatureDetail -> CreatureDetailSheet(state, overlay.creatureId)
        is Overlay.ConfirmChallenge -> ConfirmChallengeDialog(state, overlay)
        Overlay.ConfirmRetreat -> ConfirmRetreatDialog(state)
        is Overlay.LockedArea -> LockedAreaDialog(state, overlay.areaIndex)
        is Overlay.Notice -> NoticeDialog(state, overlay)
        Overlay.EditName -> EditNameDialog(state)
    }
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Collection -> Icons.Filled.GridView
    Tab.Shop -> Icons.Filled.Storefront
    Tab.Home -> Icons.Filled.Bolt
    Tab.Events -> Icons.Filled.Event
    Tab.Profile -> Icons.Filled.Person
}

/** Collection | Shop | raised Home | Events | Profile. */
@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val p = Z.colors
    Surface(color = p.surface, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(Dimens.bottomBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(Dimens.bottomBarHeight)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (tab == Tab.Home) {
                        Box(
                            Modifier
                                .offset(y = (-14).dp)
                                .size(Dimens.homeActionSize)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(p.headerGradient)
                                .semantics { contentDescription = "Home" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(34.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tab.icon(), contentDescription = null, tint = if (isSelected) p.accent else p.textSecondary, modifier = Modifier.size(24.dp))
                            FitText(tab.label, color = if (isSelected) p.textPrimary else p.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Rounded bottom sheet over a dimmed scrim; scrim tap and system Back dismiss it. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = Z.colors
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(p.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close", onClick = onDismiss),
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Dimens.sheetRadius, topEnd = Dimens.sheetRadius),
            color = p.surface,
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding)
                    .padding(bottom = Dimens.gap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(999.dp)).background(p.border))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
                content()
            }
        }
    }
}

/** Single-line label that steps its size down (to 9sp) instead of truncating at large font scales. */
@Composable
private fun FitText(text: String, color: androidx.compose.ui.graphics.Color) {
    val base = MaterialTheme.typography.labelSmall
    var size by remember(text) { mutableStateOf(base.fontSize) }
    Text(
        text,
        style = base.copy(fontSize = size),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.padding(horizontal = 2.dp),
        onTextLayout = { if (it.didOverflowWidth && size.value > 9f) size = (size.value - 1f).sp },
    )
}
