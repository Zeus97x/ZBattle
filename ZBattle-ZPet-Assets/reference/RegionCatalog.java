package com.zeus97x.zpet;
/** Original game locations grouped by distinct mythological traditions. */
public final class RegionCatalog {
    public static final String[] TRADITIONS={"Greek","Norse","Chinese","Egyptian","Japanese","Mesoamerican","Egyptian","Greek","Chinese moon folklore","Greek phoenix inspiration","Akan storytelling inspiration","Celtic-inspired fantasy"};
    public static final String[] AREAS={"Olympian Foothills","Thunderpeak","Underworld Gates","Elysian Horizon","Yggdrasil Roots","Rune Ruins","Frostbound Fjord","Aurora Citadel","Jade Forest","Celestial Peaks","Dragon Palace","Heavenly Gate",
    "Desert Crossing","Golden Necropolis","Veiled Dunes","Guardian Horizon",
    "Lantern Path","Mirror Grove","Foxfire Shrine","Dawn Sanctuary",
    "Feathered Canopy","Wind Terrace","Jade Garden","Skywoven Summit",
    "Dawn Sands","Solar Orchard","Halo Oasis","Sunrise Vault",
    "Foaming Shore","Coral Passage","Abyssal Reef","Crest Horizon",
    "Moonlit Meadow","Crescent Garden","Jade Moon Terrace","Lunar Sanctuary",
    "Ash Nest","Cinder Ridge","Dawn Roost","Rebirth Summit",
    "Story Grove","Riddle Crossing","Silk Canopy","Taleweaver Haven",
    "Moss Trail","Briar Grove","Elder Woodland","Bloom Sanctuary"};
    public static int family(int area) { if(area<0 || area>=AREAS.length) throw new IllegalArgumentException("Unknown area"); return area/4; }
    public static int stage(int area) { family(area); return area%4; }
    public static int next(int area) { return stage(area)==3?-1:area+1; }
    public static long walkingCost(int target) { return stage(target)*1500L; }
    public static String theme(int area) { return TRADITIONS[family(area)]; }
    public static String scenery(int area) {
        return family(area)==0?"Marble terraces · olive groves · storm-lit peaks":family(area)==1?"Ancient roots · runestones · frost and aurora":family(area)==2?"Jade groves · cloud terraces · lantern-lit waters":TRADITIONS[family(area)]+" · "+AREAS[area];
    }
    private RegionCatalog() {}
}
