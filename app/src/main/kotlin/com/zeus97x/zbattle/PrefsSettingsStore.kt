package com.zeus97x.zbattle

import android.content.Context
import com.zeus97x.zbattle.core.PlayerSettings
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.SettingsStore

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
        return PlayerSettings(
            displayName = prefs.getString(KEY_NAME, null)?.let(PlayerSettings::validName) ?: defaults.displayName,
            darkMode = prefs.getBoolean(KEY_DARK, defaults.darkMode),
            music = prefs.getBoolean(KEY_MUSIC, defaults.music),
            battleAnimations = prefs.getBoolean(KEY_ANIMATIONS, defaults.battleAnimations),
            currentAreaIndex = current,
            visitedAreas = visited + current,
        )
    }

    override fun save(settings: PlayerSettings) {
        prefs.edit()
            .putString(KEY_NAME, settings.displayName)
            .putBoolean(KEY_DARK, settings.darkMode)
            .putBoolean(KEY_MUSIC, settings.music)
            .putBoolean(KEY_ANIMATIONS, settings.battleAnimations)
            .putInt(KEY_AREA, settings.currentAreaIndex)
            .putString(KEY_VISITED, settings.visitedAreas.sorted().joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_NAME = "displayName"
        const val KEY_DARK = "darkMode"
        const val KEY_MUSIC = "music"
        const val KEY_ANIMATIONS = "battleAnimations"
        const val KEY_AREA = "currentArea"
        const val KEY_VISITED = "visitedAreas"
    }
}
