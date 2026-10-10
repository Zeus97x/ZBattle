package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.economy.Inventory
import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.ItemKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Base64

/**
 * Versioned binary save (Base64), in the style of ZPet's `Progression`/`AdventureState` codecs.
 * Decoding validates every field and rejects trailing data; callers keep the raw string for
 * recovery when [decode] fails. Holds no secrets or device paths.
 *
 * - v1 (CLAUDE-002): one fighter per battle.
 * - v2 (CLAUDE-005 B2): chosen party, party battles (team, active member, participants,
 *   pending replacement) and per-participant XP results. v1 saves are migrated on read: the
 *   single fighter becomes a one-member team, and the old result is credited to the lead.
 * - v3 (CLAUDE-005 B3): companion identity (companionId, rarity, nickname, origin, source revision)
 *   and the evolution ledger. Older saves read with no companionId (assigned once by the app and
 *   saved), rarity 0 (D-NATIVE-SPECIES), ZBattle origin and an empty ledger.
 * - v4 (CLAUDE-005 B5): `settledThrough`. Older saves derive it: every id below the active battle
 *   (or below `nextBattleId` when idle) was already settled.
 * - v5 (CLAUDE-006 C1): inventory (balances, applied transaction ids, id sequence). Older saves read
 *   with an empty inventory; the starter kit is then granted once by [BattleProgress.withStarter].
 * - v6 (CLAUDE-006 C2): the last result also records coins and the ticket paid. Older results read
 *   as 0 coins and no ticket (nothing was paid then).
 * - v7 (CLAUDE-006 battle items): the active battle records items used. Older battles read as 0.
 * - v8 (CLAUDE-006 equipment): each owned creature records its equipped charm. Older saves read as none.
 */
object BattleProgressCodec {
    const val VERSION = 8

    fun encode(progress: BattleProgress): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { d ->
            d.writeInt(VERSION)
            d.writeLong(progress.nextUid)
            d.writeLong(progress.nextBattleId)
            d.writeInt(progress.creatures.size)
            progress.creatures.forEach { c ->
                d.writeLong(c.uid); d.writeUTF(c.creatureId); d.writeLong(c.xp)
                d.writeUTF(c.companionId ?: ""); d.writeInt(c.rarity); d.writeUTF(c.nickname ?: "")
                d.writeUTF(c.origin.name); d.writeLong(c.sourceRevision)
                d.writeUTF(c.equipment ?: "")
            }
            d.writeInt(progress.defeated.size)
            progress.defeated.sorted().forEach(d::writeUTF)
            d.writeInt(progress.wins.size)
            progress.wins.toSortedMap().forEach { (k, v) -> d.writeUTF(k); d.writeInt(v) }
            d.writeInt(progress.party.size)
            progress.party.forEach(d::writeLong)
            d.writeBoolean(progress.active != null)
            progress.active?.let { writeBattle(d, it) }
            d.writeBoolean(progress.lastResult != null)
            progress.lastResult?.let { r ->
                d.writeLong(r.battleId); d.writeUTF(r.encounterId); d.writeUTF(r.outcome.name); d.writeBoolean(r.firstVictory)
                d.writeInt(r.gains.size)
                r.gains.forEach { g -> d.writeLong(g.uid); d.writeLong(g.xp); d.writeInt(g.levelBefore); d.writeInt(g.levelAfter) }
                d.writeLong(r.coins); d.writeUTF(r.ticket ?: "")
            }
            val ledger = progress.evolution
            d.writeInt(ledger.processed.size); ledger.processed.sorted().forEach(d::writeUTF)
            d.writeInt(ledger.claims.size)
            ledger.claims.forEach { d.writeUTF(it.companionId); d.writeInt(it.stage); d.writeUTF(it.source.name) }
            d.writeInt(ledger.outbound.size)
            ledger.outbound.forEach { writeUnlock(d, it) }
            d.writeLong(progress.settledThrough)
            val inv = progress.inventory
            d.writeInt(inv.balances.size); inv.balances.toSortedMap().forEach { (k, v) -> d.writeUTF(k); d.writeLong(v) }
            d.writeInt(inv.applied.size); inv.applied.sorted().forEach(d::writeUTF)
            d.writeLong(inv.nextSeq)
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    /** @throws IllegalStateException when the data is unreadable or fails validation. */
    fun decode(value: String): BattleProgress = try {
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use { d ->
            val version = d.readInt()
            if (version !in 1..VERSION) throw IOException("Unknown battle save version")
            val nextUid = d.readLong()
            val nextBattleId = d.readLong()
            val creatures = List(count(d, 500)) {
                if (version >= 3) {
                    OwnedCreature(
                        uid = d.readLong(), creatureId = d.readUTF(), xp = d.readLong(),
                        companionId = d.readUTF().ifEmpty { null }, rarity = d.readInt(), nickname = d.readUTF().ifEmpty { null },
                        origin = CompanionOrigin.valueOf(d.readUTF()), sourceRevision = d.readLong(),
                        equipment = if (version >= 8) d.readUTF().ifEmpty { null } else null,
                    )
                } else OwnedCreature(d.readLong(), d.readUTF(), d.readLong())
            }
            val defeated = List(count(d, 10_000)) { d.readUTF() }.toSet()
            val wins = List(count(d, 10_000)) { d.readUTF() to d.readInt() }.toMap()
            val party = if (version >= 2) List(count(d, BattleEngine.PARTY_SIZE)) { d.readLong() } else emptyList()
            val active = if (d.readBoolean()) (if (version >= 2) readBattle(d, version) else readBattleV1(d)) else null
            val last = if (!d.readBoolean()) null else if (version >= 2) {
                val r = BattleResult(d.readLong(), d.readUTF(), Outcome.valueOf(d.readUTF()), d.readBoolean(),
                    List(count(d, BattleEngine.PARTY_SIZE)) { XpGain(d.readLong(), d.readLong(), d.readInt(), d.readInt()) })
                if (version >= 6) r.copy(coins = d.readLong(), ticket = d.readUTF().ifEmpty { null }) else r
            } else {
                val battleId = d.readLong(); val encounterId = d.readUTF(); val outcome = Outcome.valueOf(d.readUTF())
                val xp = d.readLong(); val before = d.readInt(); val after = d.readInt(); val first = d.readBoolean()
                // v1 results belonged to the only fighter, which was always the lead.
                val gains = creatures.firstOrNull()?.let { listOf(XpGain(it.uid, xp, before, after)) } ?: emptyList()
                BattleResult(battleId, encounterId, outcome, first, gains)
            }
            val ledger = if (version >= 3) EvolutionLedger(
                processed = List(count(d, 10_000)) { d.readUTF() }.toSet(),
                claims = List(count(d, 10_000)) { ChallengeClaim(d.readUTF(), d.readInt(), UnlockSource.valueOf(d.readUTF())) },
                outbound = List(count(d, 10_000)) { readUnlock(d) },
            ) else EvolutionLedger()
            val settledThrough = if (version >= 4) d.readLong() else (active?.battleId ?: nextBattleId) - 1
            val inventory = if (version >= 5) Inventory(
                balances = List(count(d, 1_000)) { d.readUTF() to d.readLong() }.let { pairs ->
                    pairs.toMap().also { if (it.size != pairs.size) throw IOException("Duplicate inventory entry") }
                },
                applied = List(count(d, 1_000_000)) { d.readUTF() }.toSet(),
                nextSeq = d.readLong(),
            ) else Inventory()
            if (d.available() != 0) throw IOException("Trailing data")
            if (settledThrough < 0 || settledThrough >= nextBattleId || (active != null && active.battleId <= settledThrough)) throw IOException("Invalid settlement marker")
            if (nextUid < 1 || nextBattleId < 1) throw IOException("Invalid counters")
            val uids = creatures.map { it.uid }
            if (uids.any { it >= nextUid } || uids.toSet().size != uids.size) throw IOException("Invalid creature ids")
            if (wins.values.any { it < 0 }) throw IOException("Invalid wins")
            if (party.toSet().size != party.size || party.any { it !in uids }) throw IOException("Invalid party")
            if (active != null && (active.battleId >= nextBattleId || active.team.any { it.uid !in uids })) throw IOException("Invalid active battle")
            if (last != null && (last.gains.any { it.xp < 0 } || last.coins < 0)) throw IOException("Invalid result")
            if (last?.ticket != null && ItemCatalog.get(last.ticket)?.kind != ItemKind.Ticket) throw IOException("Invalid result ticket")
            val ids = creatures.mapNotNull { it.companionId }
            if (ids.toSet().size != ids.size) throw IOException("Duplicate companion ids")
            if (ledger.outbound.any { !it.validated || it.toForm !in 0..5 }) throw IOException("Invalid outbound unlock")
            BattleProgress(creatures, nextUid, nextBattleId, defeated, wins, active, last, party, ledger, settledThrough, inventory)
        }
    } catch (e: Exception) {
        throw IllegalStateException("Battle save unreadable; raw data retained for recovery", e)
    }

    private fun count(d: DataInputStream, max: Int): Int = d.readInt().also { if (it < 0 || it > max) throw IOException("Invalid count") }

    private fun writeBattle(d: DataOutputStream, b: BattleState) {
        d.writeLong(b.battleId); d.writeUTF(b.encounterId)
        d.writeInt(b.team.size)
        b.team.forEach { m -> d.writeLong(m.uid); writeCombatant(d, m.combatant); d.writeInt(m.skillCooldown) }
        d.writeInt(b.activeIndex)
        writeCombatant(d, b.enemy)
        d.writeInt(b.turn); d.writeInt(b.burnTurns); d.writeInt(b.weakenTurns)
        d.writeInt(b.participants.size); b.participants.sorted().forEach(d::writeLong)
        d.writeBoolean(b.awaitingReplacement)
        d.writeInt(b.log.size); b.log.forEach(d::writeUTF)
        d.writeUTF(b.outcome?.name ?: "")
        d.writeInt(b.itemsUsed)
    }

    private fun readBattle(d: DataInputStream, version: Int): BattleState {
        val battleId = d.readLong()
        val encounterId = d.readUTF()
        val team = List(count(d, BattleEngine.PARTY_SIZE)) { TeamMember(d.readLong(), readCombatant(d), d.readInt()) }
        val state = BattleState(
            battleId = battleId,
            encounterId = encounterId,
            team = team,
            activeIndex = d.readInt(),
            enemy = readCombatant(d),
            turn = d.readInt(),
            burnTurns = d.readInt(),
            weakenTurns = d.readInt(),
            participants = List(count(d, BattleEngine.PARTY_SIZE)) { d.readLong() }.toSet(),
            awaitingReplacement = d.readBoolean(),
            log = List(count(d, 100)) { d.readUTF() },
            outcome = d.readUTF().takeIf { it.isNotEmpty() }?.let(Outcome::valueOf),
            itemsUsed = if (version >= 7) d.readInt() else 0,
        )
        if (state.participants.any { p -> team.none { it.uid == p } }) throw IOException("Invalid participants")
        if (state.awaitingReplacement != (state.player.fainted)) throw IOException("Invalid replacement state")
        if (state.awaitingReplacement && state.benchIndices().isEmpty()) throw IOException("Invalid replacement state")
        return validated(state)
    }

    /** v1 battle: one fighter with its own cooldown. */
    private fun readBattleV1(d: DataInputStream): BattleState {
        val battleId = d.readLong()
        val encounterId = d.readUTF()
        val uid = d.readLong()
        val player = readCombatant(d)
        val enemy = readCombatant(d)
        val turn = d.readInt()
        val cooldown = d.readInt()
        val state = BattleState(
            battleId = battleId,
            encounterId = encounterId,
            team = listOf(TeamMember(uid, player, cooldown)),
            activeIndex = 0,
            enemy = enemy,
            turn = turn,
            burnTurns = d.readInt(),
            weakenTurns = d.readInt(),
            participants = if (turn > 0) setOf(uid) else emptySet(),
            log = List(count(d, 100)) { d.readUTF() },
            outcome = d.readUTF().takeIf { it.isNotEmpty() }?.let(Outcome::valueOf),
        )
        return validated(state)
    }

    private fun validated(state: BattleState): BattleState {
        if (state.outcome != null) throw IOException("Settled battle stored as active")
        if (Encounters.byId(state.encounterId) == null) throw IOException("Unknown encounter")
        if (state.turn !in 0 until BattleEngine.TURN_LIMIT || state.team.any { it.skillCooldown !in 0..BattleEngine.SKILL_COOLDOWN } ||
            state.burnTurns !in 0..BattleEngine.EFFECT_TURNS || state.weakenTurns !in 0..BattleEngine.EFFECT_TURNS ||
            state.itemsUsed !in 0..BattleEngine.MAX_ITEMS_PER_BATTLE
        ) throw IOException("Invalid battle counters")
        return state
    }

    private fun writeUnlock(d: DataOutputStream, u: EvolutionUnlock) {
        d.writeUTF(u.eventId); d.writeUTF(u.companionId); d.writeInt(u.toForm); d.writeUTF(u.source.name); d.writeBoolean(u.validated)
    }

    private fun readUnlock(d: DataInputStream) = EvolutionUnlock(d.readUTF(), d.readUTF(), d.readInt(), UnlockSource.valueOf(d.readUTF()), d.readBoolean())

    private fun writeCombatant(d: DataOutputStream, c: Combatant) {
        d.writeUTF(c.creatureId)
        for (n in intArrayOf(c.level, c.maxHp, c.hp, c.power, c.guard, c.speed)) d.writeInt(n)
    }

    private fun readCombatant(d: DataInputStream): Combatant {
        val id = d.readUTF()
        if (CreatureCatalog.byId(id) == null) throw IOException("Unknown creature")
        return Combatant(id, d.readInt(), d.readInt(), d.readInt(), d.readInt(), d.readInt(), d.readInt())
    }
}
