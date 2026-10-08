package com.zeus97x.zpet;
/** Four original species variants per tradition; variants reuse approved family artwork. */
public final class SpeciesCatalog {
    public static final String[] RARITIES={"Common","Heroic","Mythic","Celestial"};
    public static final String[][] NAMES={
        {"Sparklit","Laurelclaw","Nimbusfang","Aethercub"},
        {"Inkling","Runescout","Auroraven","Starquill"},
        {"Cindlet","Jadeflare","Cloudcoil","Pearlheart"},
        {"Dunepup","Goldward","Veilkeeper","Starguard"},
        {"Wispkit","Lanternkin","Mirrorheart","Sunveil"},
        {"Plumeling","Windcoil","Jadeplume","Skydream"},
        {"Glintgrub","Dawnkeeper","Solarwing","Haloheart"},
        {"Foalfoam","Reeftide","Mistmane","Pearlcourser"},
        {"Moonbun","Crescentkin","Dreamjade","Moonheart"},
        {"Ashpeep","Emberdawn","Ashradiance","Sunreborn"},
        {"Threadbit","Silktale","Riddlekin","Starweaver"},
        {"Budfawn","Grovekin","Thornbloom","Elderstar"}
    };
    public static int family(String id) { return index(id)/4; }
    public static int rarity(String id) { return index(id)%4; }
    public static int index(String id) {
        for(int f=0;f<NAMES.length;f++) for(int r=0;r<4;r++) if(id.equals(f+":"+r)) return f*4+r;
        throw new IllegalArgumentException("Unknown species");
    }
    public static String id(int family,int rarity) { String id=family+":"+rarity; index(id); return id; }
    public static String name(String id) { return NAMES[family(id)][rarity(id)]; }
    public static String starter(String id) { return MonsterCatalog.FAMILIES[family(id)][0]; }
    public static String form(String id,int form) { return MonsterCatalog.FAMILIES[family(id)][form]; }
    public static String[] giftPool(int area) {
        int f=RegionCatalog.family(area), max=RegionCatalog.stage(area)==0?0:1;
        String[] ids=new String[max+1]; for(int i=0;i<ids.length;i++) ids[i]=id(f,i); return ids;
    }
    public static String roll(int area,int roll) {
        int stage=RegionCatalog.stage(area),tier=0;
        if(stage==0) tier=roll<85?0:1;
        else if(stage==1) tier=roll<65?0:roll<92?1:2;
        else tier=roll<50?0:roll<82?1:roll<97?2:3;
        return id(RegionCatalog.family(area),tier);
    }
    public static int catchChance(String id) { return new int[]{80,65,50,35}[rarity(id)]; }
    private SpeciesCatalog() {}
}
