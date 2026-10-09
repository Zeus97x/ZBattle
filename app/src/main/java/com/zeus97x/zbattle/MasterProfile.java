package com.zeus97x.zbattle;

/** Appearance has no skill restrictions. Profile contains no ZPet progression. */
public final class MasterProfile {
    public static final String[] STYLES = {"Ranger", "Dragon Disciple", "Knight", "Mystic", "Artificer"};
    public static final String[] GENDERS = {"Male", "Female"};
    public static final String[] STARTERS = {"Sparklit", "Inkling", "Cindlet"};
    public final String name;
    public final int style, gender, starter;
    public MasterProfile(String name, int style, int gender, int starter) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 24) throw new IllegalArgumentException("Use a name with 1–24 characters.");
        if (style < 0 || style >= STYLES.length || gender < 0 || gender >= GENDERS.length || starter < 0 || starter >= STARTERS.length)
            throw new IllegalArgumentException("Choose a valid style, gender and companion.");
        this.name = normalized; this.style = style; this.gender = gender; this.starter = starter;
    }
}
