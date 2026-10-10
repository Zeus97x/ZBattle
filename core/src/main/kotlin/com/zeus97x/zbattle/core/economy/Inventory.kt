package com.zeus97x.zbattle.core.economy

/**
 * Typed inventory with a transaction ledger (CLAUDE-006 C1, EXT-021).
 *
 * Every change is a [Transaction] with an id. A transaction applies completely or not at all: an
 * unknown item, a balance below zero, a balance over the item's cap, or an id that was applied
 * before rejects the whole transaction and leaves the inventory unchanged. Retrying a transaction
 * that already went through is therefore safe: it is reported as [TxResult.Rejected] with
 * [TxError.Duplicate] and pays nothing twice.
 *
 * The inventory is part of the battle save, so a battle's rewards are written in the same state
 * change that settles the battle.
 */
data class Transaction(val id: String, val deltas: Map<String, Long>) {
    init {
        require(id.isNotBlank() && id.length <= MAX_ID) { "Invalid transaction id" }
        require(deltas.isNotEmpty() && deltas.values.none { it == 0L }) { "Empty transaction" }
    }

    companion object {
        const val MAX_ID = 64
    }
}

enum class TxError { Duplicate, UnknownItem, Underflow, OverCap }

sealed interface TxResult {
    data class Applied(val inventory: Inventory) : TxResult
    data class Rejected(val error: TxError, val itemId: String? = null) : TxResult
}

data class Inventory(
    /** Item id → quantity; zero balances are not stored. */
    val balances: Map<String, Long> = emptyMap(),
    /** Ids of every transaction applied so far. */
    val applied: Set<String> = emptySet(),
    /** Counter for locally generated ids (purchases); only grows. */
    val nextSeq: Long = 1,
) {
    init {
        require(balances.all { (id, n) -> ItemCatalog.get(id) != null && n in 1..ItemCatalog.require(id).cap }) { "Invalid balance" }
        require(nextSeq >= 1) { "Invalid sequence" }
    }

    operator fun get(itemId: String): Long = balances[itemId] ?: 0
    val coins: Long get() = this[ItemCatalog.COINS]

    fun apply(tx: Transaction): TxResult {
        if (tx.id in applied) return TxResult.Rejected(TxError.Duplicate)
        val next = balances.toMutableMap()
        for ((itemId, delta) in tx.deltas) {
            val def = ItemCatalog.get(itemId) ?: return TxResult.Rejected(TxError.UnknownItem, itemId)
            val after = this[itemId] + delta
            if (after < 0) return TxResult.Rejected(TxError.Underflow, itemId)
            if (after > def.cap) return TxResult.Rejected(TxError.OverCap, itemId)
            if (after == 0L) next.remove(itemId) else next[itemId] = after
        }
        return TxResult.Applied(copy(balances = next, applied = applied + tx.id))
    }

    /** Applies [tx] or throws; for callers where a rejection is a programming error. */
    fun applyOrThrow(tx: Transaction): Inventory = when (val r = apply(tx)) {
        is TxResult.Applied -> r.inventory
        is TxResult.Rejected -> error("Transaction ${tx.id} rejected: ${r.error} ${r.itemId ?: ""}".trim())
    }

    /** Grants the starter kit once (D-SHOP); a second call changes nothing. */
    fun withStarterKit(): Inventory =
        if (ItemCatalog.STARTER_KIT_TX in applied) this
        else applyOrThrow(Transaction(ItemCatalog.STARTER_KIT_TX, ItemCatalog.starterKit))

    /** Reserves a fresh local transaction id, e.g. for a purchase. */
    fun reserveId(prefix: String): Pair<String, Inventory> = "$prefix-$nextSeq" to copy(nextSeq = nextSeq + 1)
}
