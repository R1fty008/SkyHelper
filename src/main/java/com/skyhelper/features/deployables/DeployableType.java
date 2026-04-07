package com.skyhelper.features.deployables;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines all known Hypixel Skyblock deployable types with their properties.
 * Hypixel entity names look like "Plasmaflux 38s" — a short prefix followed by
 * the live server countdown. We match by prefix.
 */
public enum DeployableType {
    // displayName            prefix (start of entity name)    category            radius dur  R    G    B
    // Power Orbs
    RADIANT_ORB    ("Radiant Orb",    "Radiant",    Category.POWER_ORB, 18, 30,  0,   255, 80),
    MANA_FLUX_ORB  ("Mana Flux Orb",  "Mana Flux",  Category.POWER_ORB, 18, 30,  30,  100, 255),
    OVERFLUX_ORB   ("Overflux Orb",   "Overflux",   Category.POWER_ORB, 18, 60,  180, 20,  20),
    PLASMAFLUX_ORB ("Plasmaflux Orb", "Plasmaflux", Category.POWER_ORB, 20, 60,  160, 0,   255),

    // Flares
    WARNING_FLARE("Warning Flare", "Warning Flare", Category.FLARE, 40, 180, 255, 140, 0),
    ALERT_FLARE  ("Alert Flare",   "Alert Flare",   Category.FLARE, 40, 180, 255, 140, 0),
    SOS_FLARE    ("SOS Flare",     "SOS Flare",     Category.FLARE, 40, 180, 255, 140, 0),

    // Black Holes
    SMALL_BLACK_HOLE ("Small Pocket Black Hole",  "Small Pocket Black Hole",  Category.BLACK_HOLE, 10, 180, 80,  0, 80),
    MEDIUM_BLACK_HOLE("Medium Pocket Black Hole", "Medium Pocket Black Hole", Category.BLACK_HOLE, 10, 180, 120, 0, 120),

    // Mining Lanterns
    DWARVEN_LANTERN ("Dwarven Lantern",  "Dwarven Lantern",  Category.LANTERN, 30, 300, 255, 200, 100),
    MITHRIL_LANTERN ("Mithril Lantern",  "Mithril Lantern",  Category.LANTERN, 30, 300, 100, 200, 255),
    TITANIUM_LANTERN("Titanium Lantern", "Titanium Lantern", Category.LANTERN, 30, 300, 180, 220, 255),
    GLACITE_LANTERN ("Glacite Lantern",  "Glacite Lantern",  Category.LANTERN, 30, 300, 150, 230, 255),
    WILL_O_WISP     ("Will-o'-wisp",     "Will-o'-wisp",     Category.LANTERN, 30, 300, 200, 255, 200),

    // Fishing
    UMBERELLA          ("Umberella",          "Umberella",          Category.FISHING, 30, 300, 100, 60,  30),
    TOTEM_OF_CORRUPTION("Totem of Corruption","Totem of Corruption",Category.FISHING, 15, 120, 80,  200, 80);

    public enum Category {
        POWER_ORB, FLARE, BLACK_HOLE, LANTERN, FISHING
    }

    public final String displayName;
    public final String prefix;
    public final Category category;
    public final int radius;
    public final int baseDurationSeconds;
    public final int r, g, b;

    DeployableType(String displayName, String prefix, Category category, int radius,
                   int baseDurationSeconds, int r, int g, int b) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.category = category;
        this.radius = radius;
        this.baseDurationSeconds = baseDurationSeconds;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Result of matching a raw entity display name. */
    public static class Match {
        public final DeployableType type;
        /** Server-side remaining seconds extracted from name (e.g. "Plasmaflux 38s" -> 38). -1 if absent. */
        public final int serverCountdown;

        public Match(DeployableType type, int serverCountdown) {
            this.type = type;
            this.serverCountdown = serverCountdown;
        }
    }

    // \u00a7 is the section sign § used by Minecraft formatting codes
    private static final Pattern COLOR_CODE = Pattern.compile("\u00a7.");
    private static final Pattern COUNTDOWN  = Pattern.compile("(\\d+)s\\s*$");

    /**
     * Match a raw entity display name against known deployables.
     * Strips Minecraft color codes, then prefix-matches against known names.
     * Returns null when no deployable matches.
     */
    public static Match match(String rawName) {
        if (rawName == null || rawName.isEmpty()) return null;

        // Strip color codes (§ + any character) and trim
        String name = COLOR_CODE.matcher(rawName).replaceAll("").trim();
        if (name.isEmpty()) return null;

        // Never match the bare "Flare" mob from Crimson Isle
        if (name.equals("Flare")) return null;

        // Find the longest-prefix match (handles overlapping prefixes safely)
        DeployableType matched = null;
        for (DeployableType type : values()) {
            if (name.startsWith(type.prefix)) {
                if (matched == null || type.prefix.length() > matched.prefix.length()) {
                    matched = type;
                }
            }
        }
        if (matched == null) return null;

        // Extract trailing "<digits>s" if present
        int countdown = -1;
        Matcher m = COUNTDOWN.matcher(name);
        if (m.find()) {
            try {
                countdown = Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                // leave as -1
            }
        }

        return new Match(matched, countdown);
    }

    /**
     * Match a held item's display name to a deployable type.
     * Item names look like "RADIANT POWER ORB", "WARNING FLARE", "DWARVEN LANTERN", etc.
     * (with rarity color codes). We strip codes and do a case-insensitive contains
     * against each known prefix, picking the longest match to avoid ambiguity.
     */
    public static DeployableType matchItem(String rawItemName) {
        if (rawItemName == null || rawItemName.isEmpty()) return null;
        String stripped = COLOR_CODE.matcher(rawItemName).replaceAll("").trim();
        if (stripped.isEmpty()) return null;

        String upper = stripped.toUpperCase();

        DeployableType matched = null;
        for (DeployableType type : values()) {
            String prefixUpper = type.prefix.toUpperCase();
            if (upper.contains(prefixUpper)) {
                if (matched == null || type.prefix.length() > matched.prefix.length()) {
                    matched = type;
                }
            }
        }
        return matched;
    }
}
