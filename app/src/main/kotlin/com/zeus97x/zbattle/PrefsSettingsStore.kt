package com.zeus97x.zbattle

import android.content.Context
import android.content.SharedPreferences
import com.zeus97x.zbattle.core.MasterGender
import com.zeus97x.zbattle.core.MasterStyle
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.PlayerSettings
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.SettingsStore
import com.zeus97x.zbattle.core.battle.BattleProgress
import com.zeus97x.zbattle.core.battle.BattleProgressCodec

/** Local-only persistence. Nothing here is sent anywhere or written to ZPet. */
class PrefsSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("zbattle.settings.v1", Context.MODE_PRIVATE)

    override fun load(): PlayerSettings {
        val defaults = PlayerSettings()
        val areaRange = RegionCatalog.areas.indices
        val current = prefs.getInt(KEY_AREA, defaults.currentAreaIndex).takeIf { it in areaRange } ?: defaults.currentAreaIndex
        val visited = prefs.getString(KEY_VISITED, null)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in areaRange }
            ?.toSet()
            ?: defaults.visitedAreas
        // An incomplete or invalid saved profile sends the player back to setup rather than crashing.
        val master = PetMaster.create(
            name = prefs.getString(KEY_NAME, null),
            style = prefs.getString(KEY_STYLE, null)?.let { id -> MasterStyle.entries.firstOrNull { it.id == id } },
            gender = prefs.getString(KEY_GENDER, null)?.let { id -> MasterGender.entries.firstOrNull { it.id == id } },
            starterId = prefs.getString(KEY_STARTER, null),
        )
        return PlayerSettings(
            master = master,
            progress = loadProgress(),
            darkMode = prefs.getBoolean(KEY_DARK, defaults.darkMode),
            music = prefs.getBoolean(KEY_MUSIC, defaults.music),
            battleAnimations = prefs.getBoolean(KEY_ANIMATIONS, defaults.battleAnimations),
            currentAreaIndex = current,
            visitedAreas = visited + current,
        )
    }

    override fun save(settings: PlayerSettings) = edit(settings).apply()

    /** Synchronous write, so a ticket roll is on disk before its result is shown (C3). */
    override fun saveDurably(settings: PlayerSettings) {
        check(edit(settings).commit()) { "Save failed" }
    }

    private fun edit(settings: PlayerSettings): SharedPreferences.Editor {
        val editor = prefs.edit()
        val master = settings.master
        if (master == null) {
            editor.remove(KEY_NAME).remove(KEY_STYLE).remove(KEY_GENDER).remove(KEY_STARTER)
        } else {
            editor.putString(KEY_NAME, master.name)
                .putString(KEY_STYLE, master.style.id)
                .putString(KEY_GENDER, master.gender.id)
                .putString(KEY_STARTER, master.starterId)
        }
        editor
            .putBoolean(KEY_DARK, settings.darkMode)
            .putBoolean(KEY_MUSIC, settings.music)
            .putBoolean(KEY_ANIMATIONS, settings.battleAnimations)
            .putInt(KEY_AREA, settings.currentAreaIndex)
            .putString(KEY_VISITED, settings.visitedAreas.sorted().joinToString(","))
            .putString(KEY_PROGRESS, BattleProgressCodec.encode(settings.progress))
        return editor
    }

    /**
     * Unreadable battle data is moved aside (never deleted) so it can be recovered later, and
     * the player continues with fresh progress; their starter is re-granted by AppState.
     */
    private fun loadProgress(): BattleProgress {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return BattleProgress()
        return try {
            BattleProgressCodec.decode(raw)
        } catch (_: IllegalStateException) {
            prefs.edit().putString(KEY_PROGRESS_RECOVERY, raw).remove(KEY_PROGRESS).apply()
            BattleProgress()
        }
    }

    private companion object {
        const val KEY_NAME = "masterName"
        const val KEY_STYLE = "masterStyle"
        const val KEY_GENDER = "masterGender"
        const val KEY_STARTER = "starter"
        const val KEY_DARK = "darkMode"
        const val KEY_MUSIC = "music"
        const val KEY_ANIMATIONS = "battleAnimations"
        const val KEY_AREA = "currentArea"
        const val KEY_VISITED = "visitedAreas"
        const val KEY_PROGRESS = "battleProgress"
        const val KEY_PROGRESS_RECOVERY = "battleProgress.unreadable"
    }
}
