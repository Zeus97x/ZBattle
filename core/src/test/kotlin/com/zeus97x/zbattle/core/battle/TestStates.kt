package com.zeus97x.zbattle.core.battle

/** Test helper: replaces the active member's combatant. */
fun BattleState.withPlayer(transform: (Combatant) -> Combatant): BattleState =
    copy(team = team.mapIndexed { i, m -> if (i == activeIndex) m.copy(combatant = transform(m.combatant)) else m })
