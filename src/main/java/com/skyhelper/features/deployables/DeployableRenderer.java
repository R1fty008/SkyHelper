package com.skyhelper.features.deployables;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders radius circles and billboard labels for tracked deployables.
 * Uses the official Fabric pattern: push pose, translate by -camPos, draw with world coordinates.
 */
public class DeployableRenderer {

    private static final int CIRCLE_SEGMENTS = 64;

    private static long lastDebugLog = 0;

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(DeployableRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        if (!SkyHelperConfig.get().deployablesEnabled) return;

        Camera camera = context.gameRenderer().getMainCamera();
        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource consumers = (MultiBufferSource.BufferSource) context.consumers();
        PoseStack poseStack = context.matrices();

        int trackedCount = DeployableTracker.getTracked().size();

        // Periodic debug logging to confirm renderer is firing and tracked entities exist
        if (System.currentTimeMillis() - lastDebugLog > 5000) {
            lastDebugLog = System.currentTimeMillis();
            System.out.println("[SkyHelper] Render fired: tracked=" + trackedCount
                    + " camPos=" + camPos
                    + " consumers=" + (consumers != null)
                    + " poseStack=" + (poseStack != null));
        }

        if (trackedCount == 0) return;

        // OFFICIAL FABRIC PATTERN:
        // 1. Push pose stack
        // 2. Translate by -camPos so we can use world coordinates
        // 3. Draw with world-space coordinates
        // 4. Pop pose stack
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        renderCircles(poseStack, consumers);

        poseStack.popPose();

        if (SkyHelperConfig.get().showElapsedTimerLabel) {
            renderLabels(camPos, camera, consumers);
        }
    }

    private static void renderCircles(PoseStack poseStack, MultiBufferSource.BufferSource consumers) {
        VertexConsumer buffer = consumers.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        float opacity = getOpacityMultiplier();

        for (DeployableTracker.TrackedDeployable dep : DeployableTracker.getTracked()) {
            if (!dep.alive) continue;

            float fadeAlpha = dep.getFadeAlpha();
            if (fadeAlpha <= 0) continue;

            int alpha = (int) (opacity * fadeAlpha * 255);
            if (alpha < 10) continue;

            float radius = dep.type.radius;

            // WORLD coordinates - the pose stack has the -camPos translation applied
            float cx = (float) dep.position.x;
            float cy = (float) (dep.position.y + 0.1);
            float cz = (float) dep.position.z;

            int r = dep.type.r;
            int g = dep.type.g;
            int b = dep.type.b;

            // Draw two rings at slightly different heights for visibility
            drawCircle(buffer, matrix, cx, cy, cz, radius, r, g, b, alpha);
            drawCircle(buffer, matrix, cx, cy + 1.0f, cz, radius, r, g, b, alpha);
        }

        // Force the lines to flush
        consumers.endBatch(RenderType.lines());
    }

    private static void drawCircle(VertexConsumer buffer, Matrix4f matrix,
                                    float cx, float cy, float cz, float radius,
                                    int r, int g, int b, int alpha) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / CIRCLE_SEGMENTS);
            float angle2 = (float) (2 * Math.PI * ((i + 1) % CIRCLE_SEGMENTS) / CIRCLE_SEGMENTS);

            float x1 = cx + radius * (float) Math.cos(angle1);
            float z1 = cz + radius * (float) Math.sin(angle1);
            float x2 = cx + radius * (float) Math.cos(angle2);
            float z2 = cz + radius * (float) Math.sin(angle2);

            // Normal along line direction (used by line shader for screen-space width)
            float dx = x2 - x1;
            float dz = z2 - z1;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            float nx = dx / len;
            float nz = dz / len;

            buffer.addVertex(matrix, x1, cy, z1)
                    .setColor(r, g, b, alpha)
                    .setNormal(nx, 0, nz);
            buffer.addVertex(matrix, x2, cy, z2)
                    .setColor(r, g, b, alpha)
                    .setNormal(nx, 0, nz);
        }
    }

    private static void renderLabels(Vec3 camPos, Camera camera, MultiBufferSource consumers) {
        float opacityMult = getOpacityMultiplier();

        for (DeployableTracker.TrackedDeployable dep : DeployableTracker.getTracked()) {
            float fadeAlpha = dep.getFadeAlpha();
            if (fadeAlpha <= 0) continue;

            float alpha = opacityMult * fadeAlpha;
            renderLabel(camPos, camera, consumers, dep, alpha);
        }
    }

    private static void renderLabel(Vec3 camPos, Camera camera, MultiBufferSource consumers,
                                     DeployableTracker.TrackedDeployable dep, float alpha) {
        long elapsed = dep.getElapsedSeconds();
        String timeStr = elapsed >= 60
                ? (elapsed / 60) + "m " + (elapsed % 60) + "s"
                : elapsed + "s";

        String label = dep.type.displayName + " | " + timeStr;

        int color;
        if (elapsed < dep.type.baseDurationSeconds) {
            color = 0xFF55FF55;
        } else {
            color = 0xFFFFAA00;
        }
        int labelAlpha = (int) (alpha * 255);
        color = (color & 0x00FFFFFF) | (labelAlpha << 24);

        Font font = Minecraft.getInstance().font;
        float textWidth = font.width(label);

        // Build a fresh PoseStack for the billboard label (camera-relative)
        PoseStack labelStack = new PoseStack();
        labelStack.translate(
                dep.position.x - camPos.x,
                dep.position.y - camPos.y + 2.5,
                dep.position.z - camPos.z
        );
        labelStack.mulPose(camera.rotation());
        labelStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f labelMatrix = labelStack.last().pose();
        int bgColor = (int) (alpha * 100) << 24;

        font.drawInBatch(label, -textWidth / 2, 0, color, false, labelMatrix, consumers,
                Font.DisplayMode.SEE_THROUGH, bgColor, LightTexture.FULL_BRIGHT);
    }

    private static float getOpacityMultiplier() {
        return switch (SkyHelperConfig.get().deployableCircleOpacity) {
            case 0 -> 0.25f;
            case 1 -> 0.50f;
            case 3 -> 1.00f;
            default -> 0.75f;
        };
    }
}
