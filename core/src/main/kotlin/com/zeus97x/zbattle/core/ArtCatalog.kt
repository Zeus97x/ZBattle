package com.zeus97x.zbattle.core

/**
 * Single artwork lookup keyed by stable logical ids. Screens ask for an [ArtKey]; they never
 * name image files. Existing ZPet creatures resolve to the untouched asset pack. Every other
 * group resolves by convention to `art/<key>.png` (or .webp) so ChatGPT's final illustrations
 * can be dropped into app/src/main/assets/art/ later without touching layout code. Until a file
 * exists the platform loader returns nothing and the UI draws a themed placeholder.
 */
sealed class ArtKey(val path: String, val fit: ArtFit, val placeholder: PlaceholderStyle) {
    class CreatureArt(val creature: Creature) : ArtKey("creature/${creature.id}", ArtFit.Contain, PlaceholderStyle.Creature)
    data object BrandLogo : ArtKey("branding/logo", ArtFit.Contain, PlaceholderStyle.Brand)
    data object BrandIcon : ArtKey("branding/icon", ArtFit.Contain, PlaceholderStyle.Brand)
    data object BrandSplash : ArtKey("branding/splash", ArtFit.Cover, PlaceholderStyle.Brand)
    // Maps are tall journey illustrations (~2:3); shown whole so every landmark stays visible.
    class RegionMap(group: RegionGroup) : ArtKey("region/${group.id}/map", ArtFit.Contain, PlaceholderStyle.Map)
    class LocationHero(area: Area) : ArtKey("location/${area.id}/hero", ArtFit.Cover, PlaceholderStyle.Scenery)
    class LocationBattle(area: Area) : ArtKey("location/${area.id}/battle", ArtFit.Cover, PlaceholderStyle.Scenery)
    class Boss(id: String) : ArtKey("boss/$id", ArtFit.Contain, PlaceholderStyle.Portrait)
    class Opponent(id: String) : ArtKey("opponent/$id", ArtFit.Contain, PlaceholderStyle.Portrait)
    class Item(id: String) : ArtKey("item/$id", ArtFit.Contain, PlaceholderStyle.Item)
    class Badge(id: String) : ArtKey("badge/$id", ArtFit.Contain, PlaceholderStyle.Badge)
    class Avatar(id: String) : ArtKey("avatar/$id", ArtFit.Cover, PlaceholderStyle.Portrait)
    class Event(id: String) : ArtKey("event/$id", ArtFit.Cover, PlaceholderStyle.Scenery)

    override fun equals(other: Any?): Boolean = other is ArtKey && other.path == path
    override fun hashCode(): Int = path.hashCode()
    override fun toString(): String = "ArtKey($path)"
}

enum class ArtFit { Contain, Cover }

/** Which native icon/gradient the placeholder uses; purely presentational. */
enum class PlaceholderStyle { Creature, Brand, Map, Scenery, Portrait, Item, Badge }

object ArtCatalog {
    const val ART_ROOT = "art"
    private val extensions = listOf("png", "webp")

    /** Candidate asset paths, in priority order, for the platform loader to try. */
    fun candidates(key: ArtKey): List<String> = when (key) {
        is ArtKey.CreatureArt -> listOfNotNull(key.creature.assetPath)
        else -> extensions.map { "$ART_ROOT/${key.path}.$it" }
    }
}
