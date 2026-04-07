package com.skyhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkyHelperConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skyhelper.json");

    private static SkyHelperConfig INSTANCE;

    // === Storage ===
    public boolean calculatorEnabled = true;

    // === Chat ===
    public boolean copyChatEnabled = true;
    public boolean hyperionFilterEnabled = true;

    // === HUD ===
    public boolean skillOverlayEnabled = true;
    public boolean potionOverlayEnabled = true;

    // === Alerts ===
    public boolean alertsEnabled = true;
    public boolean rebootAlertEnabled = true;
    public boolean transferAlertEnabled = true;
    public boolean adminCheckAlertEnabled = true;
    public int alertVolume = 2; // 0=Low, 1=Medium, 2=Max
    public boolean rebootFlashScreen = true;
    public boolean adminCheckChatAlert = true;
    public boolean alertsOnlyWhileFarming = false;

    // === Pest Repellent Timer ===
    public boolean pestRepellentTimerEnabled = true;
    public boolean pestRepellentSoundAt5Min = true;
    public boolean pestRepellentSoundOnExpire = true;
    public boolean pestRepellentShowToast = true;
    public long pestRepellentStartTime = 0; // epoch millis when repellent was applied

    // === Deployables ===
    public boolean deployablesEnabled = true;
    public boolean showPowerOrbRadius = true;
    public boolean showFlareRadius = true;
    public boolean showBlackHoleRadius = true;
    public boolean showLanternRadius = true;
    public boolean showFishingDeployableRadius = true;
    public boolean showOtherPlayersDeployables = true;
    public boolean showElapsedTimerLabel = true;
    public int deployableParticleDensity = 1; // 0=Low, 1=Medium, 2=High
    public int deployableCircleOpacity = 2;   // 0=25%, 1=50%, 2=75%, 3=100%

    // === Keybinds ===
    public boolean keybindsEnabled = true;
    public List<KeybindEntry> keybinds = new ArrayList<>();

    // === HUD Layout ===
    public Map<String, HudPanelLayout> hudLayout = new LinkedHashMap<>();

    // ── Keybind entry ──
    public static class KeybindEntry {
        public String key;
        public String command;
        public String label;
        public boolean enabled;

        public KeybindEntry() {}

        public KeybindEntry(String key, String command, String label, boolean enabled) {
            this.key = key;
            this.command = command;
            this.label = label;
            this.enabled = enabled;
        }
    }

    // ── HUD panel layout ──
    public static class HudPanelLayout {
        public double x;
        public double y;
        public int width;
        public int height;
        public boolean visible;
        public double scale;
        public String customLabel;

        public HudPanelLayout() {}

        public HudPanelLayout(double x, double y, int width, int height, boolean visible, double scale) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.visible = visible;
            this.scale = scale;
            this.customLabel = null;
        }
    }

    public static SkyHelperConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, SkyHelperConfig.class);
                if (INSTANCE == null) {
                    System.out.println("[SkyHelper] Config parsed as null, creating fresh config.");
                    INSTANCE = new SkyHelperConfig();
                }
                // Ensure lists/maps are never null after deserialization
                if (INSTANCE.keybinds == null) INSTANCE.keybinds = new ArrayList<>();
                if (INSTANCE.hudLayout == null) INSTANCE.hudLayout = new LinkedHashMap<>();
                INSTANCE.ensureDefaults();
                System.out.println("[SkyHelper] Config loaded from " + CONFIG_PATH + " (" + INSTANCE.keybinds.size() + " keybinds)");
                return;
            } catch (Exception e) {
                System.err.println("[SkyHelper] ERROR: Failed to load config from " + CONFIG_PATH);
                e.printStackTrace();
            }
        } else {
            System.out.println("[SkyHelper] No config file found at " + CONFIG_PATH + ", creating defaults.");
        }
        INSTANCE = new SkyHelperConfig();
        INSTANCE.populateDefaultKeybinds();
        INSTANCE.ensureDefaultHudLayout();
        save();
    }

    private void ensureDefaults() {
        // If keybinds list is empty, populate defaults (first install)
        if (keybinds.isEmpty()) {
            populateDefaultKeybinds();
        }
        ensureDefaultHudLayout();
    }

    private void populateDefaultKeybinds() {
        keybinds.add(new KeybindEntry("B", "/wd", "Wardrobe", true));
        keybinds.add(new KeybindEntry("N", "/pets", "Pets", true));
        keybinds.add(new KeybindEntry("M", "/storage", "Storage", true));
        keybinds.add(new KeybindEntry("H", "/bz", "Bazaar", true));
    }

    private void ensureDefaultHudLayout() {
        hudLayout.putIfAbsent("skill_xp", new HudPanelLayout(0.02, 0.02, 160, 32, true, 1.0));
        hudLayout.putIfAbsent("potion_effects", new HudPanelLayout(0.75, 0.05, 160, 120, true, 1.0));
        hudLayout.putIfAbsent("pest_repellent", new HudPanelLayout(0.02, 0.12, 140, 38, true, 1.0));
    }

    public static HudPanelLayout getPanel(String id) {
        SkyHelperConfig cfg = get();
        cfg.ensureDefaultHudLayout();
        return cfg.hudLayout.get(id);
    }

    public static void resetHudDefaults() {
        SkyHelperConfig cfg = get();
        cfg.hudLayout.clear();
        cfg.ensureDefaultHudLayout();
        save();
    }

    public static void resetPanel(String id) {
        SkyHelperConfig cfg = get();
        switch (id) {
            case "skill_xp" -> cfg.hudLayout.put(id, new HudPanelLayout(0.02, 0.02, 160, 32, true, 1.0));
            case "potion_effects" -> cfg.hudLayout.put(id, new HudPanelLayout(0.75, 0.05, 160, 120, true, 1.0));
            case "pest_repellent" -> cfg.hudLayout.put(id, new HudPanelLayout(0.02, 0.12, 140, 38, true, 1.0));
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(get());
            Files.writeString(CONFIG_PATH, json);
            System.out.println("[SkyHelper] Config saved to " + CONFIG_PATH + " (" + get().keybinds.size() + " keybinds, " + json.length() + " bytes)");
        } catch (IOException e) {
            System.err.println("[SkyHelper] ERROR: Failed to save config to " + CONFIG_PATH);
            e.printStackTrace();
        }
    }
}
