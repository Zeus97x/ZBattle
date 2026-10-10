package com.zeus97x.zbattle.core.economy

import com.zeus97x.zbattle.core.AssetPack
import com.zeus97x.zbattle.core.battle.BattleAction
import com.zeus97x.zbattle.core.battle.BattleProgress
import com.zeus97x.zbattle.core.battle.BattleProgressCodec
import com.zeus97x.zbattle.core.battle.Encounters
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** CLAUDE-006 C1 / EXT-021: the ledger is all-or-nothing, bounded and once-only per transaction id. */
class InventoryTest {
    private val kit = Inventory().withStarterKit()

    @Test
    fun starterKitIsGrantedOnce() {
        assertEquals(100, kit.coins)
        assertEquals(3, kit[ItemCatalog.POTION])
        assertEquals(kit, kit.withStarterKit())
        // Spending everything never re-grants it.
        val spent = kit.applyOrThrow(Transaction("spend", mapOf(ItemCatalog.COINS to -100L, ItemCatalog.POTION to -3L)))
        assertEquals(0, spent.coins)
        assertEquals(spent, spent.withStarterKit())
    }

    @Test
    fun aRejectedTransactionChangesNothing() {
        // Coins would be fine, but potions would go negative: neither part applies.
        val r = kit.apply(Transaction("buy-1", mapOf(ItemCatalog.COINS to -30L, ItemCatalog.POTION to -4L)))
        assertEquals(TxResult.Rejected(TxError.Underflow, ItemCatalog.POTION), r)
        val over = kit.apply(Transaction("gift", mapOf(ItemCatalog.COINS to 10L, ItemCatalog.POTION to ItemCatalog.STACK_CAP)))
        assertEquals(TxResult.Rejected(TxError.OverCap, ItemCatalog.POTION), over)
        assertEquals(TxResult.Rejected(TxError.UnknownItem, "focus-tonic"), kit.apply(Transaction("x", mapOf("focus-tonic" to 1L))))
        // Nothing above was recorded, so the same id can still be used for a valid transaction.
        assertIs<TxResult.Applied>(kit.apply(Transaction("buy-1", mapOf(ItemCatalog.COINS to -30L, ItemCatalog.POTION to 1L))))
    }

    @Test
    fun aRetriedTransactionPaysOnce() {
        val tx = Transaction("battle-7", mapOf(ItemCatalog.COINS to 20L))
        val once = kit.applyOrThrow(tx)
        assertEquals(TxResult.Rejected(TxError.Duplicate), once.apply(tx))
        assertEquals(TxResult.Rejected(TxError.Duplicate), once.apply(Transaction("battle-7", mapOf(ItemCatalog.COINS to 999L))))
        assertEquals(120, once.coins)
    }

    @Test
    fun capsHold() {
        val rich = Inventory().applyOrThrow(Transaction("a", mapOf(ItemCatalog.COINS to ItemCatalog.COIN_CAP)))
        assertEquals(TxResult.Rejected(TxError.OverCap, ItemCatalog.COINS), rich.apply(Transaction("b", mapOf(ItemCatalog.COINS to 1L))))
        val cosmetic = ItemCatalog.ofKind(ItemKind.Cosmetic).first().id
        val one = Inventory().applyOrThrow(Transaction("c", mapOf(cosmetic to 1L)))
        assertEquals(TxResult.Rejected(TxError.OverCap, cosmetic), one.apply(Transaction("d", mapOf(cosmetic to 1L))))
    }

    @Test
    fun reservedIdsNeverRepeat() {
        val (first, inv) = kit.reserveId("buy")
        val (second, _) = inv.reserveId("buy")
        assertEquals("buy-1", first)
        assertEquals("buy-2", second)
    }

    @Test
    fun catalogueFollowsTheApprovedRules() {
        val ids = ItemCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "ids are unique")
        assertTrue(ids.all { Regex("^[a-z0-9-]+$").matches(it) }, "stable lower-case ids")
        assertTrue(ItemCatalog.ofKind(ItemKind.Ticket).all { !it.purchasable }, "tickets are never sold")
        assertTrue(ItemCatalog.ofKind(ItemKind.ZCube).none { "mythic" in it.id }, "Mythic ZCubes are never sold")
        assertTrue(ItemCatalog.ofKind(ItemKind.ZCube).none { it.purchasable }, "ZCubes wait for catching")
        assertTrue(ItemCatalog.ofKind(ItemKind.Material).isEmpty(), "no crafting materials in v1")
        assertTrue(ids.none { it.startsWith("demo-") || it == "focus-tonic" || it == "revive-seed" })
        assertEquals(listOf(1, 2, 3), ItemCatalog.ofKind(ItemKind.Ticket).map { it.ticketTier })
    }

    @Test
    fun inventorySurvivesSaveAndOldSavesGetTheKitOnLoad() {
        var p = BattleProgress().withStarter("sparklit")
        p = p.copy(inventory = p.inventory.applyOrThrow(Transaction("t1", mapOf("fang-charm-1" to 1L, ItemCatalog.TICKET_RARE to 2L))))
        p = p.startBattle(Encounters.playable.single()).act(BattleAction.Attack)
        val back = BattleProgressCodec.decode(BattleProgressCodec.encode(p))
        assertEquals(p, back)
        assertEquals(setOf(ItemCatalog.STARTER_KIT_TX, "t1"), back.inventory.applied)

        val v1 = File(AssetPack.root.parentFile, "ai/integration/fixtures/migration/zbattle-battleprogress-v1.b64").readText().trim()
        val old = BattleProgressCodec.decode(v1)
        assertEquals(Inventory(), old.inventory)
        val loaded = old.withStarter("cindlet")
        assertEquals(100, loaded.inventory.coins)
        assertEquals(loaded, loaded.withStarter("cindlet"), "loading again grants nothing more")
    }

    @Test
    fun corruptInventoryIsRejected() {
        val p = BattleProgress().withStarter("sparklit")
        val raw = java.util.Base64.getDecoder().decode(BattleProgressCodec.encode(p))
        // The last 8 bytes are nextSeq: zero is invalid.
        val bad = raw.copyOf().also { for (i in it.size - 8 until it.size) it[i] = 0 }
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(java.util.Base64.getEncoder().encodeToString(bad)) }
    }
}
