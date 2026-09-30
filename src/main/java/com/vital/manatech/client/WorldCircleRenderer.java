package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vital.manatech.rune.CircleEffectMesh;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.List;

/** Renders the same authored segments used by the canvas and grimoire preview. */
public final class WorldCircleRenderer {
    private WorldCircleRenderer() {}

    public static void render(List<DiagramLayer> layers, PoseStack pose, MultiBufferSource buffers,
                              float x, float y, float z, float time) {
        CircleEffectMesh mesh = CircleEffectMesh.generate(layers);
        if (mesh.segments().isEmpty()) return;
        pose.pushPose();
        pose.translate(x, y, z);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        float rotation = time * .0032f;
        for (CircleEffectMesh.Segment segment : mesh.segments()) {
            float angle = rotation * (segment.layer() % 2 == 0 ? 1 : -1);
            float c = (float)Math.cos(angle), s = (float)Math.sin(angle);
            float ax = segment.x1() * .54f, az = segment.y1() * .54f;
            float bx = segment.x2() * .54f, bz = segment.y2() * .54f;
            float x1 = ax * c - az * s, z1 = ax * s + az * c;
            float x2 = bx * c - bz * s, z2 = bx * s + bz * c;
            float height = .12f + segment.layer() * .025f;
            ribbon(out, matrix, x1, z1, x2, z2, height, .012f, segment.color(), 20);
            ribbon(out, matrix, x1, z1, x2, z2, height + .0005f, .006f, segment.color(), 136);
            ribbon(out, matrix, x1, z1, x2, z2, height + .001f, .0025f,
                    HologramStyle.core(segment.color()), 245);
        }
        pose.popPose();
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, float x1, float z1,
                               float x2, float z2, float y, float width, int color, int alpha) {
        float dx = x2 - x1, dz = z2 - z1;
        float length = (float)Math.hypot(dx, dz);
        if (length < .0001f) return;
        float cap = Math.min(width / 2, length / 2);
        x1 -= dx / length * cap; z1 -= dz / length * cap;
        x2 += dx / length * cap; z2 += dz / length * cap;
        float nx = -dz / length * width / 2, nz = dx / length * width / 2;
        vertex(out, matrix, x1 - nx, y, z1 - nz, color, alpha);
        vertex(out, matrix, x1 + nx, y, z1 + nz, color, alpha);
        vertex(out, matrix, x2 + nx, y, z2 + nz, color, alpha);
        vertex(out, matrix, x2 - nx, y, z2 - nz, color, alpha);
        vertex(out, matrix, x2 - nx, y, z2 - nz, color, alpha);
        vertex(out, matrix, x2 + nx, y, z2 + nz, color, alpha);
        vertex(out, matrix, x1 + nx, y, z1 + nz, color, alpha);
        vertex(out, matrix, x1 - nx, y, z1 - nz, color, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float y,
                               float z, int color, int alpha) {
        out.addVertex(matrix, x, y, z).setColor((color >> 16) & 255, (color >> 8) & 255,
                color & 255, alpha).setUv(0, 0).setOverlay(0).setLight(0xF000F0).setNormal(0, 1, 0);
    }
}
