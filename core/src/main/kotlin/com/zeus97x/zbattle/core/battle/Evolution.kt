package com.zeus97x.zbattle.core.battle

/**
 * Evolution identity and unlock rules (CLAUDE-005 B3, D-EVOLUTION decided 2026-10-10).
 *
 * - A companion keeps its identity (uid, companionId), rarity, branch, nickname and progress when it
 *   evolves; only its form changes.
 * - ZBattle owns combat XP; ZPet owns walking/care progress.
 * - Shared evolution only happens through validated unlock events. Forms never go down, and stale
 *   snapshots are ignored.
 * - A challenge-gated final stage (stage 4, e.g. Founder Stage 4) can be unlocked through either
 *   game's challenge, but only one claim per companion counts.
 * - Until cross-app delivery exists, imported ZPet forms are preserved. A ZBattle unlock for an
 *   imported companion is queued as pending delivery and never applied locally, so nothing
 *   pretends to synchronize.
 * - Thresholds and Founder definitions are pending (D-EVOLUTION-THRESHOLDS), so [EvolutionRules]
 *   ships inactive.
 */
enum class CompanionOrigin(val contractId: String) { ZBattle("zbattle"), ZPet("zpet") }

enum class Branch(val contractId: String) { None("none"), A("A"), B("B") }

/** ZPet form graph: 0 Baby → 1 Young → (2 → 3) Branch A or (4 → 5) Branch B. */
object FormGraph {
    fun branchOf(form: Int): Branch = when (form) {
        0, 1 -> Branch.None
        2, 3 -> Branch.A
        4, 5 -> Branch.B
        else -> throw IllegalArgumentException("Unknown form $form")
    }

    /** 1-based stage: Baby 1, Young 2, advanced 3, final 4. */
    fun stage(form: Int): Int = when (form) {
        0 -> 1
        1 -> 2
        2, 4 -> 3
        3, 5 -> 4
        else -> throw IllegalArgumentException("Unknown form $form")
    }

    fun nextForms(form: Int): List<Int> = when (form) {
        0 -> listOf(1)
        1 -> listOf(2, 4)
        2 -> listOf(3)
        4 -> listOf(5)
        else -> emptyList()
    }

    /** True when [to] is a later form on a path from [from] (a branch, once taken, is kept). */
    fun reachable(from: Int, to: Int): Boolean = nextForms(from).any { it == to || reachable(it, to) }
}

enum class UnlockSource(val app: CompanionOrigin, val challenge: Boolean) {
    ZBattleCombat(CompanionOrigin.ZBattle, false),
    ZBattleChallenge(CompanionOrigin.ZBattle, true),
    ZPetCare(CompanionOrigin.ZPet, false),
    ZPetChallenge(CompanionOrigin.ZPet, true),
}

/** An evolution unlock. [validated] = checked by the approved authority (never just "a phone said so"). */
data class EvolutionUnlock(
    val eventId: String,
    val companionId: String,
    val toForm: Int,
    val source: UnlockSource,
    val validated: Boolean,
)

enum class UnlockResult {
    Applied,
    /** ZBattle unlocked an imported companion: queued for delivery to ZPet; the local form is unchanged. */
    PendingDelivery,
    Duplicate,
    NotValidated,
    UnknownCompanion,
    NoDowngrade,
    IllegalForm,
    /** The other app (or this one) already claimed this companion's challenge-gated final stage. */
    ChallengeAlreadyClaimed,
    /** The source app does not own this companion's form. */
    WrongAuthority,
}

data class ChallengeClaim(val companionId: String, val stage: Int, val source: UnlockSource)

/** Saved evolution history: processed event ids, challenge claims and unlocks awaiting delivery. */
data class EvolutionLedger(
    val processed: Set<String> = emptySet(),
    val claims: List<ChallengeClaim> = emptyList(),
    val outbound: List<EvolutionUnlock> = emptyList(),
)

/**
 * Inactive configuration. Combat-XP thresholds (minimum level per target form) and challenge
 * definitions are not approved yet, so no automatic evolution is offered.
 */
object EvolutionRules {
    val combatLevelThresholds: Map<Int, Int>? = null

    /** Forms a ZBattle-native companion may evolve into from combat progress now (empty while inactive). */
    fun availableCombatEvolutions(c: OwnedCreature, thresholds: Map<Int, Int>? = combatLevelThresholds): List<Int> {
        if (thresholds == null || c.origin != CompanionOrigin.ZBattle) return emptyList()
        return FormGraph.nextForms(c.formIndex).filter { target -> thresholds[target]?.let { c.level >= it } == true }
    }
}

/** Applies one unlock event. Returns the new progress (unchanged unless recorded) and why. */
fun BattleProgress.applyUnlock(u: EvolutionUnlock): Pair<BattleProgress, UnlockResult> {
    if (u.eventId in evolution.processed || evolution.outbound.any { it.eventId == u.eventId }) return this to UnlockResult.Duplicate
    if (!u.validated) return this to UnlockResult.NotValidated
    val c = creatures.firstOrNull { it.companionId != null && it.companionId == u.companionId } ?: return this to UnlockResult.UnknownCompanion
    val processed = evolution.copy(processed = evolution.processed + u.eventId)
    if (u.toForm !in 0..5) return copy(evolution = processed) to UnlockResult.IllegalForm
    if (u.toForm <= c.formIndex) return copy(evolution = processed) to UnlockResult.NoDowngrade
    if (!FormGraph.reachable(c.formIndex, u.toForm)) return copy(evolution = processed) to UnlockResult.IllegalForm
    if (c.origin == CompanionOrigin.ZBattle && u.source.app != CompanionOrigin.ZBattle) return copy(evolution = processed) to UnlockResult.WrongAuthority

    val stage = FormGraph.stage(u.toForm)
    val gated = u.source.challenge && stage == 4
    if (gated && evolution.claims.any { it.companionId == u.companionId && it.stage == 4 }) {
        return copy(evolution = processed) to UnlockResult.ChallengeAlreadyClaimed
    }
    val claims = if (gated) evolution.claims + ChallengeClaim(u.companionId, 4, u.source) else evolution.claims

    if (c.origin == CompanionOrigin.ZPet && u.source.app == CompanionOrigin.ZBattle) {
        return copy(evolution = evolution.copy(claims = claims, outbound = evolution.outbound + u)) to UnlockResult.PendingDelivery
    }
    val evolved = c.copy(creatureId = c.creature.family.forms[u.toForm].lowercase().replace(" ", ""))
    return copy(
        creatures = creatures.map { if (it.uid == c.uid) evolved else it },
        evolution = processed.copy(claims = claims),
    ) to UnlockResult.Applied
}

/**
 * An origin-app snapshot of an imported companion's form. Older revisions are ignored; a newer
 * revision with a lower form keeps the current form (an ordinary stale snapshot never downgrades).
 */
fun BattleProgress.applyFormSnapshot(companionId: String, formIndex: Int, sourceRevision: Long): BattleProgress {
    val c = creatures.firstOrNull { it.companionId == companionId && it.origin == CompanionOrigin.ZPet } ?: return this
    if (sourceRevision <= c.sourceRevision) return this
    val form = if (formIndex > c.formIndex && FormGraph.reachable(c.formIndex, formIndex)) formIndex else c.formIndex
    val updated = c.copy(creatureId = c.creature.family.forms[form].lowercase().replace(" ", ""), sourceRevision = sourceRevision)
    return copy(creatures = creatures.map { if (it.uid == c.uid) updated else it })
}

/** Assigns a companion UUID once to every creature that has none (persist the result immediately). */
fun BattleProgress.withCompanionIds(newId: () -> String): BattleProgress =
    if (creatures.all { it.companionId != null }) this
    else copy(creatures = creatures.map { if (it.companionId == null) it.copy(companionId = newId()) else it })
