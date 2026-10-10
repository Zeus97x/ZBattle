package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.CreatureCatalog
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
 */
object BattleProgressCodec {
    const val VERSION = 1

    fun encode(progress: BattleProgress): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { d ->
            d.writeInt(VERSION)
            d.writeLong(progress.nextUid)
            d.writeLong(progress.nextBattleId)
            d.writeInt(progress.creatures.size)
            progress.creatures.forEach { d.writeLong(it.uid); d.writeUTF(it.creatureId); d.writeLong(it.xp) }
            d.writeInt(progress.defeated.size)
            progress.defeated.sorted().forEach(d::writeUTF)
            d.writeInt(progress.wins.size)
            progress.wins.toSortedMap().forEach { (k, v) -> d.writeUTF(k); d.writeInt(v) }
            d.writeBoolean(progress.active != null)
            progress.active?.let { writeBattle(d, it) }
            d.writeBoolean(progress.lastResult != null)
            progress.lastResult?.let { r ->
                d.writeLong(r.battleId); d.writeUTF(r.encounterId); d.writeUTF(r.outcome.name)
                d.writeLong(r.xpGained); d.writeInt(r.levelBefore); d.writeInt(r.levelAfter); d.writeBoolean(r.firstVictory)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    /** @throws IllegalStateException when the data is unreadable or fails validation. */
    fun decode(value: String): BattleProgress = try {
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use { d ->
            if (d.readInt() != VERSION) throw IOException("Unknown battle save version")
            val nextUid = d.readLong()
            val nextBattleId = d.readLong()
            val creatures = List(count(d, 500)) { OwnedCreature(d.readLong(), d.readUTF(), d.readLong()) }
            val defeated = List(count(d, 10_000)) { d.readUTF() }.toSet()
            val wins = List(count(d, 10_000)) { d.readUTF() to d.readInt() }.toMap()
            val active = if (d.readBoolean()) readBattle(d) else null
            val last = if (d.readBoolean()) {
                BattleResult(d.readLong(), d.readUTF(), Outcome.valueOf(d.readUTF()), d.readLong(), d.readInt(), d.readInt(), d.readBoolean())
            } else null
            if (d.available() != 0) throw IOException("Trailing data")
            if (nextUid < 1 || nextBattleId < 1) throw IOException("Invalid counters")
            if (creatures.any { it.uid >= nextUid } || creatures.map { it.uid }.toSet().size != creatures.size) throw IOException("Invalid creature ids")
            if (wins.values.any { it < 0 }) throw IOException("Invalid wins")
            if (active != null && (active.battleId >= nextBattleId || creatures.none { it.uid == active.playerUid })) throw IOException("Invalid active battle")
            BattleProgress(creatures, nextUid, nextBattleId, defeated, wins, active, last)
        }
    } catch (e: Exception) {
        throw IllegalStateException("Battle save unreadable; raw data retained for recovery", e)
    }

    private fun count(d: DataInputStream, max: Int): Int = d.readInt().also { if (it < 0 || it > max) throw IOException("Invalid count") }

    private fun writeBattle(d: DataOutputStream, b: BattleState) {
        d.writeLong(b.battleId); d.writeUTF(b.encounterId); d.writeLong(b.playerUid)
        writeCombatant(d, b.player); writeCombatant(d, b.enemy)
        d.writeInt(b.turn); d.writeInt(b.skillCooldown); d.writeInt(b.burnTurns); d.writeInt(b.weakenTurns)
        d.writeInt(b.log.size); b.log.forEach(d::writeUTF)
        d.writeUTF(b.outcome?.name ?: "")
    }

    private fun readBattle(d: DataInputStream): BattleState {
        val state = BattleState(
            battleId = d.readLong(),
            encounterId = d.readUTF(),
            playerUid = d.readLong(),
            player = readCombatant(d),
            enemy = readCombatant(d),
            turn = d.readInt(),
            skillCooldown = d.readInt(),
            burnTurns = d.readInt(),
            weakenTurns = d.readInt(),
            log = List(count(d, 100)) { d.readUTF() },
            outcome = d.readUTF().takeIf { it.isNotEmpty() }?.let(Outcome::valueOf),
        )
        if (state.outcome != null) throw IOException("Settled battle stored as active")
        if (Encounters.byId(state.encounterId) == null) throw IOException("Unknown encounter")
        if (state.turn !in 0 until BattleEngine.TURN_LIMIT || state.skillCooldown !in 0..BattleEngine.SKILL_COOLDOWN ||
            state.burnTurns !in 0..BattleEngine.EFFECT_TURNS || state.weakenTurns !in 0..BattleEngine.EFFECT_TURNS
        ) throw IOException("Invalid battle counters")
        return state
    }

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
