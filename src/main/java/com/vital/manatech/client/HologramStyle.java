package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

/** Shared colour treatment for authored ink in the canvas and holograms. */
public final class HologramStyle {
    private HologramStyle() {}

    public static int core(int color) {
        int red = ((color >> 16) & 255) * 4 / 5 + 51;
        int green = ((color >> 8) & 255) * 4 / 5 + 51;
        int blue = (color & 255) * 4 / 5 + 51;
        return (red << 16) | (green << 8) | blue;
    }

    public static void line(BufferBuilder buffer, PoseStack pose, float x1, float y1,
                            float x2, float y2, int color, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float)Math.hypot(dx, dy);
        if (length < .1f) return;
        float nx = -dy / length, ny = dx / length;
        quad(buffer, pose, x1, y1, x2, y2, nx, ny, 3f, color, .12f * alpha);
        quad(buffer, pose, x1, y1, x2, y2, nx, ny, 1.6f, color, .86f * alpha);
        quad(buffer, pose, x1, y1, x2, y2, nx, ny, .65f, core(color), .96f * alpha);
    }

    /** Crisp preview strokes: the full glow would fill a seven-pixel sigil. */
    public static void fineLine(BufferBuilder buffer, PoseStack pose, float x1, float y1,
                                float x2, float y2, int color, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float)Math.hypot(dx, dy);
        if (length < .1f) return;
        quad(buffer, pose, x1, y1, x2, y2, -dy / length, dx / length,
                .8f, color, alpha, .15f);
    }

    private static void quad(BufferBuilder buffer, PoseStack pose, float x1, float y1,
                             float x2, float y2, float nx, float ny, float width,
                             int color, float alpha) {
        quad(buffer, pose, x1, y1, x2, y2, nx, ny, width, color, alpha, .75f);
    }

    private static void quad(BufferBuilder buffer, PoseStack pose, float x1, float y1,
                             float x2, float y2, float nx, float ny, float width,
                             int color, float alpha, float maxCap) {
        float half = width / 2;
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float)Math.hypot(dx, dy);
        float cap = Math.min(maxCap, length / 2);
        x1 -= dx / length * cap;
        y1 -= dy / length * cap;
        x2 += dx / length * cap;
        y2 += dy / length * cap;
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;
        PoseStack.Pose transform = pose.last();
        buffer.addVertex(transform, x1 + nx * half, y1 + ny * half, 0).setColor(red, green, blue, alpha);
        buffer.addVertex(transform, x2 + nx * half, y2 + ny * half, 0).setColor(red, green, blue, alpha);
        buffer.addVertex(transform, x2 - nx * half, y2 - ny * half, 0).setColor(red, green, blue, alpha);
        buffer.addVertex(transform, x1 - nx * half, y1 - ny * half, 0).setColor(red, green, blue, alpha);
    }
}
