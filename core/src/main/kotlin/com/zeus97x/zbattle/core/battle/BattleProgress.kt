package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.economy.Inventory
import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.ItemKind
import com.zeus97x.zbattle.core.economy.StatBonus
import com.zeus97x.zbattle.core.economy.Transaction

/**
 * A creature the player owns in ZBattle. Separate from ZPet; never written back.
 *
 * Identity (CLAUDE-005 B3, D-EVOLUTION / D-NATIVE-SPECIES): [uid] is the local id and
 * [companionId] the cross-app UUID, assigned once and persisted. Species = family + [rarity], and it
 * stays separate from both the instance and the form ([creatureId] is the current form). Evolving
 * keeps uid, companionId, rarity, nickname, origin and XP.
 */
data class OwnedCreature(
    val uid: Long,
    val creatureId: String,
    /** Combat XP (ZBattle-owned). */
    val xp: Long,
    val companionId: String? = null,
    /** Canonical ZPet rarity id 0..3 (Common/Heroic/Mythic/Celestial; shown as Common/Rare/Epic/Legendary). */
    val rarity: Int = 0,
    val nickname: String? = null,
    val origin: CompanionOrigin = CompanionOrigin.ZBattle,
    /** Last accepted origin-app snapshot revision (0 = none). */
    val sourceRevision: Long = 0,
    /** Equipped charm item id (one slot, CLAUDE-006), or null. */
    val equipment: String? = null,
) {
    init {
        require(uid > 0 && xp >= 0) { "Invalid owned creature" }
        require(CreatureCatalog.byId(creatureId) != null) { "Unknown creature $creatureId" }
        require(rarity in 0..3) { "Invalid rarity" }
        require(companionId == null || UUID_PATTERN.matches(companionId)) { "Invalid companion id" }
        require(nickname == null || nickname.length in 1..NICKNAME_MAX) { "Invalid nickname" }
        require(sourceRevision >= 0) { "Invalid revision" }
        require(equipment == null || ItemCatalog.get(equipment)?.kind == ItemKind.Equipment) { "Invalid equipment" }
    }

    val creature get() = CreatureCatalog.require(creatureId)
    val formIndex: Int get() = creature.formIndex
    val branch: Branch get() = FormGraph.branchOf(formIndex)
    /** Contract species id `family:rarity`. */
    val speciesId: String get() = "${creature.family.index}:$rarity"
    val displayName: String get() = nickname ?: creature.name

    companion object {
        const val NICKNAME_MAX = 24
        val UUID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        /** D-RARITY display names for canonical ids 0..3. */
        val RARITY_NAMES = listOf("Common", "Rare", "Epic", "Legendary")
    }
    val level: Int get() = Leveling.levelFor(xp)
    val equipmentBonus: StatBonus? get() = equipment?.let { ItemCatalog.require(it).bonus }
    /** Battle stats: level stats plus the equipped charm. */
    val stats: StatBlock get() = CreatureStats.forCreature(creature, level) + equipmentBonus
}

/** XP one party member received from a settled battle. */
data class XpGain(val uid: Long, val xp: Long, val levelBefore: Int, val levelAfter: Int)

data class BattleResult(
    val battleId: Long,
    val encounterId: String,
    val outcome: Outcome,
    /** True when this victory was the first against the encounter (the only one that pays XP). */
    val firstVictory: Boolean,
    /** One entry per participant (D-PARTICIPATION); empty when nothing was paid or nobody acted. */
    val gains: List<XpGain> = emptyList(),
    /** Coins credited (C2); never more than the wallet cap allowed. */
    val coins: Long = 0,
    /** Ticket item id granted for a first clear (C2), or null. */
    val ticket: String? = null,
) {
    val xpGained: Long get() = gains.sumOf { it.xp }
    /** A victory over an encounter that was already cleared (pays replay rewards, never a first-clear ticket). */
    val replay: Boolean get() = outcome == Outcome.Victory && !firstVictory
    fun gainFor(uid: Long): XpGain? = gains.firstOrNull { it.uid == uid }
}

/**
 * How first-win XP is shared between participants. D-PARTY-XP is still open; [SplitEvenly] is the
 * proposal (total XP does not grow with party size). A single participant gets the full amount
 * under both rules, so single-creature play is unchanged.
 */
enum class PartyXpRule { SplitEvenly, EachFull }

object PartyXp {
    /** Proposal default until D-PARTY-XP is decided. */
    val rule: PartyXpRule = PartyXpRule.SplitEvenly

    /** Shares [total] between [participants] (in party order); the remainder goes to [finisher]. */
    fun share(total: Long, participants: List<Long>, finisher: Long, rule: PartyXpRule = this.rule): Map<Long, Long> {
        if (participants.isEmpty() || total <= 0) return emptyMap()
        return when (rule) {
            PartyXpRule.EachFull -> participants.associateWith { total }
            PartyXpRule.SplitEvenly -> {
                val each = total / participants.size
                val rest = total - each * participants.size
                val receiver = if (finisher in participants) finisher else participants.first()
                participants.associateWith { each + if (it == receiver) rest else 0 }
            }
        }
    }
}

/**
 * Saved battle progress (schema v1, see [BattleProgressCodec]).
 *
 * Mid-battle recovery: the active battle is saved after every turn and RESUMED when the app
 * reopens. Rewards are applied in the same state change that ends the battle and only while
 * [active] still holds that battle id, so reopening a result can never pay twice.
 */
data class BattleProgress(
    val creatures: List<OwnedCreature> = emptyList(),
    val nextUid: Long = 1,
    val nextBattleId: Long = 1,
    /** Encounters whose first-victory reward has been claimed. */
    val defeated: Set<String> = emptySet(),
    /** Total victories per encounter, rematches included. */
    val wins: Map<String, Int> = emptyMap(),
    val active: BattleState? = null,
    /** Last settled battle, shown on the results screen until dismissed. */
    val lastResult: BattleResult? = null,
    /** Chosen party (owned uids, lead first, at most [BattleEngine.PARTY_SIZE]). Empty = default order. */
    val party: List<Long> = emptyList(),
    /** Evolution unlock history (CLAUDE-005 B3). */
    val evolution: EvolutionLedger = EvolutionLedger(),
    /** Highest battle id already settled (CLAUDE-005 B5). A battle id at or below it can never pay again. */
    val settledThrough: Long = 0,
    /** Coins, items and tickets (CLAUDE-006 C1). Saved with the battle so rewards settle atomically. */
    val inventory: Inventory = Inventory(),
) {
    /** Party used for the next battle: the chosen members that are still owned, else the first owned creatures. */
    val partyMembers: List<OwnedCreature>
        get() = party.mapNotNull(::owned).ifEmpty { creatures }.take(BattleEngine.PARTY_SIZE)

    val lead: OwnedCreature? get() = partyMembers.firstOrNull()

    fun inParty(uid: Long): Boolean = partyMembers.any { it.uid == uid }

    /** Adds [uid] to the end of the party, or removes it. The party never becomes empty or exceeds 3. */
    fun toggleParty(uid: Long): BattleProgress {
        check(active == null) { "Party can't change during a battle" }
        val current = partyMembers.map { it.uid }
        val next = when {
            owned(uid) == null -> current
            uid in current -> if (current.size > 1) current - uid else current
            current.size < BattleEngine.PARTY_SIZE -> current + uid
            else -> current
        }
        return copy(party = next)
    }

    /** Makes [uid] the lead (first to fight). */
    fun makeLead(uid: Long): BattleProgress {
        check(active == null) { "Party can't change during a battle" }
        val current = partyMembers.map { it.uid }
        if (uid !in current) return this
        return copy(party = listOf(uid) + (current - uid))
    }

    fun owned(uid: Long): OwnedCreature? = creatures.firstOrNull { it.uid == uid }

    fun ownsSpecies(creatureId: String): OwnedCreature? = creatures.firstOrNull { it.creatureId == creatureId }

    fun winsIn(area: Area): Int = wins.filterKeys { it.startsWith("${area.id}/") }.values.sum()

    /**
     * Grants the chosen starter once; existing saves keep their creatures. Also grants the starter
     * kit once (D-SHOP), which covers saves made before the inventory existed.
     */
    fun withStarter(creatureId: String): BattleProgress =
        (if (creatures.isNotEmpty()) this else copy(creatures = listOf(OwnedCreature(nextUid, creatureId, 0)), nextUid = nextUid + 1))
            .copy(inventory = inventory.withStarterKit())

    fun startBattle(encounter: Encounter): BattleProgress {
        check(active == null) { "Finish or retreat from the current battle first" }
        val members = partyMembers
        check(members.isNotEmpty()) { "No companion to battle with" }
        val battle = BattleEngine.start(nextBattleId, encounter, members.map { BattleEngine.Entrant(it.uid, it.creatureId, it.level, it.equipmentBonus) })
        return copy(active = battle, nextBattleId = nextBattleId + 1, lastResult = null)
    }

    fun act(action: BattleAction): BattleProgress {
        val battle = checkNotNull(active) { "No active battle" }
        val next = BattleEngine.act(battle, action)
        return if (next.over) settle(next) else copy(active = next)
    }

    /**
     * Puts charm [itemId] in [uid]'s slot (null removes it). The charm comes out of the bag and any
     * charm already in the slot goes back, as one ledger transaction. Not during a battle, because
     * stats are fixed when the battle starts.
     */
    fun equip(uid: Long, itemId: String?): BattleProgress {
        check(active == null) { "Equipment can't change during a battle" }
        val creature = checkNotNull(owned(uid)) { "Unknown creature" }
        if (creature.equipment == itemId) return this
        if (itemId != null) {
            check(ItemCatalog.get(itemId)?.kind == ItemKind.Equipment) { "Not equipment" }
            check(inventory[itemId] > 0) { "No ${ItemCatalog.require(itemId).name} in the bag" }
        }
        val deltas = buildMap {
            itemId?.let { put(it, -1L) }
            creature.equipment?.let { put(it, 1L) }
        }
        val (id, reserved) = inventory.reserveId("equip")
        return copy(
            creatures = creatures.map { if (it.uid == uid) it.copy(equipment = itemId) else it },
            inventory = reserved.applyOrThrow(Transaction(id, deltas)),
        )
    }

    /** Why [itemId] can't be used in the active battle now, or null when it can. */
    fun itemRefusal(itemId: String): String? {
        val battle = active ?: return "No battle"
        val effect = ItemCatalog.get(itemId)?.effect ?: return "Not a battle item"
        if (inventory[itemId] <= 0) return "None left"
        return BattleEngine.itemRefusal(battle, effect)
    }

    /**
     * Uses one battle item (manual only). The item leaves the inventory in the same state change as
     * the turn, as transaction `item-<battleId>-<turn>`; if the creature is knocked out before it
     * moves, the item isn't used and stays in the inventory.
     */
    fun useItem(itemId: String): BattleProgress {
        val battle = checkNotNull(active) { "No active battle" }
        itemRefusal(itemId)?.let { error(it) }
        val item = ItemCatalog.require(itemId)
        val next = BattleEngine.useItem(battle, item.effect!!, item.name)
        val paid = if (next.itemsUsed > battle.itemsUsed) inventory.applyOrThrow(Transaction("item-${battle.battleId}-${next.turn}", mapOf(itemId to -1L))) else inventory
        val updated = copy(inventory = paid)
        return if (next.over) updated.settle(next) else updated.copy(active = next)
    }

    fun switchTo(index: Int): BattleProgress {
        val battle = checkNotNull(active) { "No active battle" }
        val next = BattleEngine.switch(battle, index)
        return if (next.over) settle(next) else copy(active = next)
    }

    fun replaceWith(index: Int): BattleProgress = copy(active = BattleEngine.replace(checkNotNull(active) { "No active battle" }, index))

    fun retreat(): BattleProgress = settle(BattleEngine.retreat(checkNotNull(active) { "No active battle" }))

    fun dismissResult(): BattleProgress = copy(lastResult = null)

    private fun settle(finished: BattleState): BattleProgress {
        val outcome = checkNotNull(finished.outcome)
        // Idempotency guards: only the battle currently held as active can be settled, and an id that
        // was settled before (e.g. an older copy of a battle restored from a backup) never pays again.
        check(active?.battleId == finished.battleId) { "Battle already settled" }
        check(finished.battleId > settledThrough) { "Battle already settled" }
        val encounter = Encounters.byId(finished.encounterId)
        val firstVictory = outcome == Outcome.Victory && finished.encounterId !in defeated
        val payout = when {
            encounter == null -> Payout.NONE
            firstVictory -> BattleRewards.firstWin(encounter)
            outcome == Outcome.Victory -> BattleRewards.replay(encounter)
            else -> Payout.NONE
        }
        val xp = payout.xp
        // Coins and tickets go to the inventory in the same state change, under the battle's id.
        // A full wallet or ticket stack is credited up to its cap rather than failing the settlement.
        val coins = minOf(payout.coins, ItemCatalog.COIN_CAP - inventory.coins)
        val ticket = payout.ticketItemId?.takeIf { inventory[it] < ItemCatalog.require(it).cap }
        val deltas = buildMap {
            if (coins > 0) put(ItemCatalog.COINS, coins)
            if (ticket != null) put(ticket, 1L)
        }
        val paid = if (deltas.isEmpty()) inventory else inventory.applyOrThrow(Transaction("battle-${finished.battleId}", deltas))
        // Only creatures that actually fought share the reward, in party order (D-PARTICIPATION).
        val participants = finished.team.map { it.uid }.filter { it in finished.participants && owned(it) != null }
        val shares = PartyXp.share(xp, participants, finished.playerUid)
        val gains = participants.map { uid ->
            val before = owned(uid)!!
            XpGain(uid, shares[uid] ?: 0, before.level, Leveling.levelFor(before.xp + (shares[uid] ?: 0)))
        }
        val updated = creatures.map { c -> shares[c.uid]?.let { c.copy(xp = c.xp + it) } ?: c }
        return copy(
            creatures = updated,
            defeated = if (firstVictory) defeated + finished.encounterId else defeated,
            wins = if (outcome == Outcome.Victory) wins + (finished.encounterId to (wins[finished.encounterId] ?: 0) + 1) else wins,
            active = null,
            settledThrough = finished.battleId,
            lastResult = BattleResult(finished.battleId, finished.encounterId, outcome, firstVictory, gains, coins, ticket),
            inventory = paid,
        )
    }
}
