package com.zeus97x.zpet;

/** Playable family evolution catalog; stable ordering preserves existing saves. */
public final class MonsterCatalog {
    public static final String[][] FAMILIES = {
        {"Sparklit","Voltmaw","Crownstorm","Astrapex","Galestride","Tempestral"},
        {"Inkling","Runebeak","Glyphwing","Oracrow","Ironquill","Wargraven"},
        {"Cindlet","Kilnback","Forgehide","Vulcarion","Flarecrest","Pyrelisk"},
        {"Dunepup","Sandward","Giltguard","Tombwarden","Veilfang","Duskjudge"},
        {"Wispkit","Emberveil","Mirrortail","Veilnine","Lanternfox","Dawnflare"},
        {"Plumeling","Plumeserp","Galeplume","Skyweaver","Jadecoil","Verdantcrest"},
        {"Glintgrub","Dawnscarab","Sunplate","Solcarapace","Halohover","Aurorabeetle"},
        {"Foalfoam","Tidecanter","Reefmane","Abysscourser","Mistgallop","Crestcharger"},
        {"Moonbun","Crescenthop","Jadebound","Lunarwarden","Dreamskip","Moondancer"},
        {"Ashpeep","Cinderwing","Blazepinion","Pyre Sovereign","Dawnfeather","Aurorise"},
        {"Threadbit","Taleweaver","Riddleweb","Mythspinner","Silkguard","Loomkeeper"},
        {"Budfawn","Mossantler","Grovecrest","Elderbloom","Briarstep","Thornhart"}
    };
    public static String family(String name) {
        for(int i=0;i<FAMILIES.length;i++) if(PrototypeState.contains(FAMILIES[i],name))
            return MonsterJournal.FAMILIES[i][0]+" · "+MonsterJournal.FAMILIES[i][1];
        throw new IllegalArgumentException("Unknown monster");
    }
    public static String stage(String name) {
        for(String[] family:FAMILIES) for(int i=0;i<family.length;i++) if(family[i].equals(name))
            return i==0?"Baby":i==1?"Young":i==2?"Branch A · advanced":i==3?"Branch A · final":i==4?"Branch B · advanced":"Branch B · final";
        throw new IllegalArgumentException("Unknown monster");
    }
    public static String asset(String name) { return "monsters/"+name.toLowerCase(java.util.Locale.ROOT)+".png"; }
    private MonsterCatalog() {}
}
