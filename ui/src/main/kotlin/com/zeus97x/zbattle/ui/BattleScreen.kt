package com.zeus97x.zbattle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.battle.BattleAction
import com.zeus97x.zbattle.core.battle.BattleResult
import com.zeus97x.zbattle.core.battle.BattleState
import com.zeus97x.zbattle.core.battle.Combatant
import com.zeus97x.zbattle.core.battle.Encounters
import com.zeus97x.zbattle.core.battle.Outcome
import com.zeus97x.zbattle.core.battle.RepeatEnd
import com.zeus97x.zbattle.core.battle.RepeatSession
import com.zeus97x.zbattle.core.battle.description
import com.zeus97x.zbattle.core.battle.BattleEngine
import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.ItemKind
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Real turn-based battle (CLAUDE-002). State lives in saved progress, so it survives restarts. */
@Composable
fun BattleScreen(state: AppState, areaIndex: Int, opponentSlot: Int) {
    val progress = state.settings.progress
    val active = progress.active
    val result = progress.lastResult
    val area = RegionCatalog.area(areaIndex)
    val encounter = Encounters.find(areaIndex, opponentSlot)
    Column(Modifier.fillMaxSize()) {
        AppHeader(
            title = if (active != null) "Battle" else "Battle results",
            subtitle = "${area.name} · ${encounter?.label ?: "Opponent"}",
            onBack = { state.back() },
        )
        when {
            active != null -> ActiveBattle(state, active)
            result != null -> ResultPanel(state, result)
            else -> EmptyState(
                ArtKey.Event("no-battle"),
                "No battle in progress",
                "Start a battle from the Challenges list.",
                action = { PrimaryButton("Back to challenges", onClick = { state.finishBattle() }) },
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActiveBattle(state: AppState, battle: BattleState) {
    val p = Z.colors
    val area = Encounters.byId(battle.encounterId)?.area ?: RegionCatalog.area(0)
    val shake = remember { Animatable(0f) }
    var lastTurn by remember { mutableStateOf(battle.turn) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(battle.turn) {
        if (battle.turn != lastTurn) {
            lastTurn = battle.turn
            if (state.settings.battleAnimations) for (target in listOf(-10f, 10f, -6f, 0f)) shake.animateTo(target, tween(70))
        }
    }
    // Auto-fight scheduler: one coroutine per battle state, cancelled as soon as the state changes
    // or the screen leaves; the step re-checks that state, so it can never act twice.
    if (state.autoFight) {
        LaunchedEffect(battle) {
            delay(if (state.settings.battleAnimations) AUTO_STEP_MS else AUTO_STEP_FAST_MS)
            state.autoFightStep(battle)
        }
    }
    var picking by remember(battle.battleId) { mutableStateOf(false) }
    var pickingItem by remember(battle.battleId) { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { state.stopAutoFight() } }

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            ArtworkSlot(ArtKey.LocationBattle(area), contentDescription = "${area.name} battle scenery", modifier = Modifier.fillMaxSize(), placeholderAlignment = Alignment.TopCenter)
            if (rememberArt(ArtKey.LocationBattle(area)) == null) {
                Pill("Scenery pending", modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            Column(Modifier.align(Alignment.TopEnd).padding(12.dp).fillMaxWidth(0.58f)) { HealthPanel(battle.enemy, "Opponent") }
            ArtworkSlot(
                ArtKey.CreatureArt(battle.enemy.creature),
                contentDescription = "Opponent ${battle.enemy.creature.name}",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, top = 48.dp).size(130.dp)
                    .offset { IntOffset(shake.value.roundToInt(), 0) },
            )
            ArtworkSlot(
                ArtKey.CreatureArt(battle.player.creature),
                contentDescription = "Your ${battle.player.creature.name}",
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 60.dp).size(140.dp),
            )
            Column(Modifier.align(Alignment.BottomEnd).padding(12.dp).fillMaxWidth(0.58f)) { HealthPanel(battle.player, "Your companion") }
        }
        Column(Modifier.padding(Dimens.screenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Turn ${battle.turn + 1}", container = p.accentDark, content = p.onAccent, icon = Icons.Filled.Bolt)
                Pill("Next: ${battle.enemyIntent}")
            }
            battle.activeEffects.takeIf { it.isNotEmpty() }?.let { effects -> EffectStrip(battle.enemy.creature.name, effects) }
            state.repeat?.takeIf { it.running }?.let { r ->
                Pill("Repeat ${r.played + 1} of ${r.target} · ${r.wins} won", container = p.accentDark, content = p.onAccent)
            }
            if (battle.team.size > 1) TeamStrip(battle)
            if (state.autoFight) {
                Text("Auto battle on · tap Stop to end it, or any other move to take control. Auto never uses items.", style = MaterialTheme.typography.bodyMedium, color = p.accent)
            } else {
                Text("Tip: hold Attack to battle automatically.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = p.accent) }
            // Screen readers announce each new turn without the player having to find the log.
            ZCard(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    battle.log.takeLast(5).forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = if (line.startsWith("Turn ")) p.textSecondary else p.textPrimary)
                    }
                }
            }
            Text(
                "${battle.skill.name} · ${battle.skillStatus} · ${battle.skill.effect.label}: ${battle.skill.effect.description}. " +
                    "Usable every ${BattleEngine.SKILL_COOLDOWN + 1} turns.",
                style = MaterialTheme.typography.labelMedium,
                color = p.textSecondary,
            )
        }
    }
    // Action grid. Bottom navigation is hidden in battle, so this panel pads for the system
    // navigation bar itself (fixes controls sitting under the Android nav bar).
    Column(
        Modifier
            .fillMaxWidth()
            .background(p.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (pickingItem && !battle.awaitingReplacement) {
            Text(
                "Items · uses your turn, then the opponent attacks · ${battle.itemsLeft} of ${BattleEngine.MAX_ITEMS_PER_BATTLE} left",
                style = MaterialTheme.typography.titleSmall,
                color = p.textPrimary,
            )
            val inventory = state.settings.progress.inventory
            val usable = ItemCatalog.ofKind(ItemKind.Consumable).filter { inventory[it.id] > 0 }
            if (usable.isEmpty()) Text("No battle items. Buy some in the Shop.", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            usable.forEach { item ->
                val refusal = state.settings.progress.itemRefusal(item.id)
                ActionButton(
                    "${item.name} ×${inventory[item.id]}" + (refusal?.let { " · $it" } ?: ""),
                    Icons.Filled.LocalDrink,
                    Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = refusal == null,
                ) {
                    pickingItem = false
                    notice = null
                    state.useItem(item.id)
                }
            }
            ActionButton("Cancel", Icons.Filled.DirectionsRun, Modifier.fillMaxWidth()) { pickingItem = false }
            return@Column
        }
        if (battle.awaitingReplacement || picking) {
            Text(
                if (battle.awaitingReplacement) "${battle.player.creature.name} fainted · choose who fights next (no turn used)"
                else "Switch · uses your turn, then the opponent attacks",
                style = MaterialTheme.typography.titleSmall,
                color = p.textPrimary,
            )
            battle.benchIndices().forEach { i ->
                val m = battle.team[i].combatant
                ActionButton("${m.creature.name} · HP ${m.hp}/${m.maxHp}", Icons.Filled.SwapHoriz, Modifier.fillMaxWidth(), primary = true) {
                    picking = false
                    notice = null
                    if (battle.awaitingReplacement) state.replaceWith(i) else state.switchTo(i)
                }
            }
            if (!battle.awaitingReplacement) ActionButton("Cancel", Icons.Filled.DirectionsRun, Modifier.fillMaxWidth()) { picking = false }
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Attack doubles as the auto-battle control: hold to start, tap while auto runs to stop.
            AttackButton(
                auto = state.autoFight,
                modifier = Modifier.weight(1f),
                onTap = {
                    notice = null
                    if (state.autoFight) state.stopAutoFight() else state.battleAction(BattleAction.Attack)
                },
                onHold = {
                    notice = null
                    state.startAutoFight()
                },
            )
            ActionButton(
                if (battle.skillReady) "Skill" else "Skill (${battle.skillCooldown})",
                Icons.Filled.AutoAwesome,
                Modifier.weight(1f).semantics { stateDescription = battle.skillStatus },
                primary = true,
                enabled = battle.skillReady,
            ) {
                notice = null
                state.battleAction(BattleAction.Skill)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Items", Icons.Filled.LocalDrink, Modifier.weight(1f)) {
                state.stopAutoFight()
                notice = null
                pickingItem = true
            }
            ActionButton("Switch", Icons.Filled.SwapHoriz, Modifier.weight(1f)) {
                if (battle.canSwitch) {
                    state.stopAutoFight()
                    picking = true
                } else {
                    notice = if (battle.team.size > 1) "No one else can fight right now."
                    else "Only one companion in your party · more arrive with ZCubes and the ZPet import."
                }
            }
        }
        ActionButton("Retreat", Icons.Filled.DirectionsRun, Modifier.fillMaxWidth()) {
            state.stopAutoFight()
            state.show(Overlay.ConfirmRetreat)
        }
    }
}

/** Party HP at a glance; the active member is highlighted. Wraps at large text sizes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamStrip(battle: BattleState) {
    val p = Z.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        battle.team.forEachIndexed { i, m ->
            val c = m.combatant
            Pill(
                "${c.creature.name} ${if (c.fainted) "· fainted" else "${c.hp}/${c.maxHp}"}",
                container = if (i == battle.activeIndex) p.accentDark else p.elevated,
                content = if (i == battle.activeIndex) p.onAccent else p.textPrimary,
            )
        }
    }
}

/** Effects still running on the opponent, with turns left, so they are not only in the log. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectStrip(enemyName: String, effects: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        effects.forEach { Pill("$enemyName · $it") }
    }
}

/** Pause between auto moves so each turn stays readable; shorter when animations are off. */
private const val AUTO_STEP_MS = 900L
private const val AUTO_STEP_FAST_MS = 400L

@Composable
private fun ResultPanel(state: AppState, result: BattleResult) {
    val p = Z.colors
    val title = when (result.outcome) {
        Outcome.Victory -> "Victory!"
        Outcome.Defeat -> "Defeated"
        Outcome.Retreat -> "Retreated"
    }
    val coins = if (result.coins > 0) " · +${formatCoins(result.coins)} coins" else ""
    val detail = when {
        result.outcome == Outcome.Victory && result.firstVictory -> "+${result.xpGained} XP shared by the creatures that fought$coins"
        result.replay -> "Rematch won · replays pay a quarter: +${result.xpGained} XP$coins"
        result.outcome == Outcome.Defeat -> "No rewards. Your companion recovers fully after each battle."
        else -> "No rewards."
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.navigationBars).padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (result.outcome == Outcome.Victory) p.accent else p.textSecondary, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = p.textPrimary)
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = p.textSecondary)
        result.ticket?.let { ItemCatalog.get(it) }?.let { ticket ->
            Pill("+1 ${ticket.name} · first clear", container = p.accentDark, content = p.onAccent, icon = Icons.Filled.EmojiEvents)
        }
        val shown = result.gains.mapNotNull { g -> state.settings.progress.owned(g.uid)?.let { it to g } }
            .ifEmpty { state.settings.ownedParty.take(1).map { it to null } }
        shown.forEach { (owned, gain) ->
            ZCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${owned.creature.name} · Level ${owned.level}", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                    if (gain != null && gain.xp > 0) {
                        Text(
                            "+${gain.xp} XP" + if (gain.levelAfter > gain.levelBefore) " · Level ${gain.levelBefore} → ${gain.levelAfter}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.accent,
                        )
                    }
                    XpRow(owned.xp)
                }
            }
        }
        state.repeat?.takeIf { !it.running && it.encounterId == result.encounterId }?.let { r -> RepeatSummary(r) }
        val encounter = Encounters.byId(result.encounterId)
        if (encounter != null && result.outcome == Outcome.Victory && result.encounterId in state.settings.progress.defeated) {
            ZCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repeat this battle · choose how many", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                    Text(
                        "Auto battles back to back, each at full HP. Stops on a defeat or if you leave. No items are used and no first-clear rewards repeat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatSession.CHOICES.forEach { n ->
                            SecondaryButton("$n", onClick = { state.startRepeat(encounter, n) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        PrimaryButton("Continue", onClick = { state.finishBattle() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RepeatSummary(r: RepeatSession) {
    val p = Z.colors
    ZCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Repeat session", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            Text("${r.played} of ${r.target} battles · ${r.wins} won · +${r.xp} XP · +${formatCoins(r.coins)} coins", style = MaterialTheme.typography.bodyLarge, color = p.textPrimary)
            Text(
                when (r.end) {
                    RepeatEnd.Completed -> "Finished."
                    RepeatEnd.Defeat -> "Stopped after a defeat."
                    RepeatEnd.Interrupted -> "Stopped because it was interrupted. Start it again when you're ready."
                    null -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = p.textSecondary,
            )
        }
    }
}

@Composable
fun XpRow(xp: Long) {
    val inLevel = com.zeus97x.zbattle.core.battle.Leveling.progressInLevel(xp)
    if (inLevel == null) ProgressRow("Experience", "Max level", 1f)
    else ProgressRow("Experience", "${inLevel.first} / ${inLevel.second} XP", inLevel.first.toFloat() / inLevel.second)
}

@Composable
private fun HealthPanel(c: Combatant, role: String) {
    val fraction = c.hp.toFloat() / c.maxHp
    val bar = when {
        fraction > 0.5f -> Color(0xFF49B87C)
        fraction > 0.2f -> Color(0xFFE0B341)
        else -> Color(0xFFE5484D)
    }
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xCC141720)).padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.creature.name, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("Lv ${c.level}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD9D6DE))
        }
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = bar,
            trackColor = Color(0xFF414555),
        )
        Text("HP ${c.hp}/${c.maxHp} · $role", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD9D6DE), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Attack with auto-battle on long press. A Material Button has no long-click, so this is a
 * clickable surface styled like [ActionButton]; TalkBack offers "Start auto battle" as its long-press action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttackButton(auto: Boolean, modifier: Modifier, onTap: () -> Unit, onHold: () -> Unit) {
    val p = Z.colors
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .heightIn(min = Dimens.primaryButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (auto) "Stop auto battle" else "Attack",
                onClick = onTap,
                onLongClickLabel = if (auto) null else "Start auto battle",
                onLongClick = if (auto) null else {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHold()
                    }
                },
            ),
        shape = RoundedCornerShape(16.dp),
        color = p.accentDark,
        contentColor = p.onAccent,
    ) {
        Box(Modifier.padding(ButtonDefaults.ContentPadding), contentAlignment = Alignment.Center) {
            ActionContent(if (auto) "Stop" else "Attack", if (auto) Icons.Filled.Stop else Icons.Filled.Bolt)
        }
    }
}

@Composable
private fun ActionContent(text: String, icon: ImageVector) {
    // Large font scales stack the icon above the label so words never break mid-word.
    if (LocalDensity.current.fontScale > 1.15f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  $text", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val p = Z.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Dimens.primaryButtonHeight),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) p.accentDark else p.elevated,
            contentColor = if (primary) p.onAccent else p.textPrimary,
        ),
    ) {
        ActionContent(text, icon)
    }
}
