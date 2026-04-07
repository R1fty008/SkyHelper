package com.skyhelper.features.deployables;

import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Detects and tracks Skyblock deployables. Three detection paths:
 *  1. Server-entity scan every 5 ticks (orbs, lanterns, black holes — they spawn armor stands)
 *  2. Client right-click for items that have NO entity (flares)
 *  3. Chat despawn messages for early removal
 */
public class DeployableTracker {

    public static class TrackedDeployable {
        public final DeployableType type;
        public final int entityId;        // negative ID for client-tracked entries
        public Vec3 position;
        public long firstSeenTime;
        public long fadeStartTime = -1;
        public boolean alive = true;
        public final double initialDistanceToPlayer;
        public int serverCountdown;       // -1 if not derivable from name
        public final boolean clientTracked; // true: no real entity, use wall-clock timer

        /** Constructor for entity-tracked deployables. */
        public TrackedDeployable(DeployableType type, int entityId, Vec3 position,
                                 double distToPlayer, int serverCountdown) {
            this.type = type;
            this.entityId = entityId;
            this.position = position;
            this.firstSeenTime = System.currentTimeMillis();
            this.initialDistanceToPlayer = distToPlayer;
            this.serverCountdown = serverCountdown;
            this.clientTracked = false;
        }

        /** Constructor for client-tracked deployables (e.g., flares). */
        public TrackedDeployable(DeployableType type, int syntheticId, Vec3 position) {
            this.type = type;
            this.entityId = syntheticId;
            this.position = position;
            this.firstSeenTime = System.currentTimeMillis();
            this.initialDistanceToPlayer = 0;
            this.serverCountdown = -1;
            this.clientTracked = true;
        }

        public long getElapsedSeconds() {
            if (!clientTracked && serverCountdown >= 0) {
                long elapsed = type.baseDurationSeconds - serverCountdown;
                return Math.max(0, elapsed);
            }
            return (System.currentTimeMillis() - firstSeenTime) / 1000;
        }

        public boolean isFading() {
            return fadeStartTime > 0;
        }

        public float getFadeAlpha() {
            if (fadeStartTime < 0) return 1.0f;
            float elapsed = (System.currentTimeMillis() - fadeStartTime) / 500.0f;
            return Math.max(0.0f, 1.0f - elapsed);
        }
    }

    /** Single shared map — both scan thread and renderer read from this. */
    private static final Map<Integer, TrackedDeployable> tracked = new LinkedHashMap<>();
    private static int tickCounter = 0;
    private static long lastDebugLogTime = 0;

    // ── Client-side flare detection state ──
    private static int nextSyntheticId = -1;
    private static boolean lastUseKeyDown = false;
    /** Recently placed flares awaiting mana confirmation (id + timestamp). */
    private static final Deque<RecentFlare> recentFlares = new ArrayDeque<>();
    private static final long MANA_CONFIRM_WINDOW_MS = 500;

    private static class RecentFlare {
        final int id;
        final long timestamp;
        RecentFlare(int id, long timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }

    // \u00a7 = §
    private static final Pattern COLOR_CODE = Pattern.compile("\u00a7.");

    public static void init() {
        // ── Chat / action bar listener ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String text = message.getString();
            if (overlay) {
                // Action bar — used to cancel a flare on "Not enough mana"
                handleActionBar(text);
            } else {
                // Chat — used to remove deployables when server announces despawn
                handleChatMessage(text);
            }
            return true; // never cancel
        });

        // ── Tick handler: scan, right-click detect, expire client-tracked, prune ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                tracked.clear();
                recentFlares.clear();
                lastUseKeyDown = false;
                return;
            }

            if (!SkyHelperConfig.get().deployablesEnabled) {
                tracked.clear();
                recentFlares.clear();
                return;
            }

            long now = System.currentTimeMillis();

            // Drop fully-faded entries
            tracked.values().removeIf(dep -> dep.isFading() && dep.getFadeAlpha() <= 0.0f);

            // Expire client-tracked entries that hit baseDuration
            for (TrackedDeployable dep : tracked.values()) {
                if (dep.clientTracked && dep.alive && !dep.isFading()) {
                    long elapsed = (now - dep.firstSeenTime) / 1000;
                    if (elapsed >= dep.type.baseDurationSeconds) {
                        dep.alive = false;
                        dep.fadeStartTime = now;
                    }
                }
            }

            // Prune mana-confirmation queue
            while (!recentFlares.isEmpty() && now - recentFlares.peekFirst().timestamp > MANA_CONFIRM_WINDOW_MS) {
                recentFlares.pollFirst();
            }

            // Right-click rising edge → check held item for a flare
            boolean useDown = client.options.keyUse.isDown();
            if (useDown && !lastUseKeyDown) {
                handleRightClick(client);
            }
            lastUseKeyDown = useDown;

            tickCounter++;

            // Entity scan every 5 ticks (~250 ms)
            if (tickCounter % 5 == 0) {
                scanEntities(client);
            }
        });
    }

    // ── Right-click flare detection ───────────────────────────

    private static void handleRightClick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        String rawItemName = mainHand.getHoverName().getString();
        DeployableType type = matchFlareItem(rawItemName);
        if (type == null) return;

        // Place at player's feet position
        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());

        int id = nextSyntheticId--;
        TrackedDeployable dep = new TrackedDeployable(type, id, pos);
        tracked.put(id, dep);
        recentFlares.addLast(new RecentFlare(id, System.currentTimeMillis()));

        System.out.println("[SkyHelper] Client-placed flare: type=" + type.displayName
                + " item=\"" + rawItemName + "\""
                + " pos=" + pos
                + " id=" + id);
    }

    /** Match a Hypixel flare item name (case-insensitive contains). */
    private static DeployableType matchFlareItem(String rawItemName) {
        if (rawItemName == null || rawItemName.isEmpty()) return null;
        String stripped = COLOR_CODE.matcher(rawItemName).replaceAll("").trim().toUpperCase();
        if (stripped.contains("WARNING FLARE")) return DeployableType.WARNING_FLARE;
        if (stripped.contains("ALERT FLARE"))   return DeployableType.ALERT_FLARE;
        if (stripped.contains("SOS FLARE"))     return DeployableType.SOS_FLARE;
        return null;
    }

    // ── Action bar / chat message handling ─────────────────────

    private static void handleActionBar(String rawText) {
        String text = COLOR_CODE.matcher(rawText).replaceAll("").toLowerCase();
        if (!text.contains("not enough mana")) return;

        // Cancel the most recently placed flare if it's still in the confirm window
        long now = System.currentTimeMillis();
        RecentFlare last = recentFlares.peekLast();
        if (last != null && now - last.timestamp < MANA_CONFIRM_WINDOW_MS) {
            recentFlares.pollLast();
            TrackedDeployable removed = tracked.remove(last.id);
            if (removed != null) {
                System.out.println("[SkyHelper] Flare cancelled (not enough mana): " + removed.type.displayName);
            }
        }
    }

    private static void handleChatMessage(String rawText) {
        String text = COLOR_CODE.matcher(rawText).replaceAll("");
        String lower = text.toLowerCase();

        boolean isDespawn = lower.contains("has expired")
                || lower.contains("was removed")
                || lower.contains("despawned");
        if (!isDespawn) return;

        // Find the first matching alive deployable whose prefix appears in the message
        for (TrackedDeployable dep : tracked.values()) {
            if (!dep.alive) continue;
            if (lower.contains(dep.type.prefix.toLowerCase())) {
                dep.alive = false;
                dep.fadeStartTime = System.currentTimeMillis();
                System.out.println("[SkyHelper] Despawn message removed: " + dep.type.displayName
                        + " (msg=\"" + text + "\")");
                return;
            }
        }
    }

    // ── Entity scan ────────────────────────────────────────────

    private static void scanEntities(Minecraft client) {
        Set<Integer> foundIds = new HashSet<>();
        Vec3 playerPos = client.player.position();

        boolean shouldDebugLog = System.currentTimeMillis() - lastDebugLogTime > 5_000;
        if (shouldDebugLog) {
            lastDebugLogTime = System.currentTimeMillis();
            String selfName = client.player.getDisplayName().getString();
            for (Entity entity : client.level.entitiesForRendering()) {
                double dist = entity.position().distanceTo(playerPos);
                if (dist > 50.0) continue;
                String debugName = entity.getDisplayName().getString();
                if (debugName.isEmpty() || debugName.equals(selfName)) continue;
                DeployableType.Match m = DeployableType.match(debugName);
                System.out.println("[SkyHelper] Nearby entity: \"" + debugName
                        + "\" type=" + entity.getType().toShortString()
                        + " dist=" + String.format("%.1f", dist)
                        + " match=" + (m == null ? "NO" : m.type.displayName));
            }
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            double dist = entity.position().distanceTo(playerPos);
            if (dist > 60.0) continue;

            String rawName = entity.getDisplayName().getString();
            if (rawName.isEmpty()) continue;

            DeployableType.Match match = DeployableType.match(rawName);
            if (match == null) continue;

            DeployableType type = match.type;

            if (!isCategoryEnabled(type.category)) continue;

            int id = entity.getId();

            if (!SkyHelperConfig.get().showOtherPlayersDeployables) {
                TrackedDeployable existing = tracked.get(id);
                if (existing == null && dist > 5.0) continue;
                if (existing != null && existing.initialDistanceToPlayer > 5.0) continue;
            }

            foundIds.add(id);

            TrackedDeployable existing = tracked.get(id);
            if (existing != null) {
                existing.position = entity.position();
                existing.alive = true;
                if (match.serverCountdown >= 0) {
                    existing.serverCountdown = match.serverCountdown;
                }
                if (existing.isFading()) {
                    existing.fadeStartTime = -1;
                }
            } else {
                TrackedDeployable newDep = new TrackedDeployable(
                        type, id, entity.position(), dist, match.serverCountdown);
                tracked.put(id, newDep);
                System.out.println("[SkyHelper] MATCHED deployable: type=" + type.displayName
                        + " rawName=\"" + rawName + "\""
                        + " id=" + id
                        + " pos=" + entity.position()
                        + " radius=" + type.radius
                        + " serverCountdown=" + match.serverCountdown
                        + " trackedTotal=" + tracked.size());
            }
        }

        // Mark missing entity-tracked deployables as fading (skip client-tracked)
        for (TrackedDeployable dep : tracked.values()) {
            if (dep.clientTracked) continue;
            if (!foundIds.contains(dep.entityId) && dep.alive) {
                dep.alive = false;
                dep.fadeStartTime = System.currentTimeMillis();
            }
        }

        if (shouldDebugLog) {
            System.out.println("[SkyHelper] Scan complete: tracked=" + tracked.size()
                    + " foundThisScan=" + foundIds.size());
        }
    }

    private static boolean isCategoryEnabled(DeployableType.Category category) {
        SkyHelperConfig config = SkyHelperConfig.get();
        return switch (category) {
            case POWER_ORB -> config.showPowerOrbRadius;
            case FLARE -> config.showFlareRadius;
            case BLACK_HOLE -> config.showBlackHoleRadius;
            case LANTERN -> config.showLanternRadius;
            case FISHING -> config.showFishingDeployableRadius;
        };
    }

    public static Collection<TrackedDeployable> getTracked() {
        return Collections.unmodifiableCollection(tracked.values());
    }
}
