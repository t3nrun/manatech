package com.vital.manatech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vital.manatech.rune.CircleEffectMesh;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

public final class CirclePreview {
    private CirclePreview() {}
    public static void draw(GuiGraphics g, CircleEffectMesh mesh, int cx, int cy, int radius) {
        if (radius <= 0 || mesh.segments().isEmpty()) return;
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (CircleEffectMesh.Segment segment : mesh.segments()) {
            float x1 = cx + segment.x1() * radius, y1 = cy + segment.y1() * radius;
            float x2 = cx + segment.x2() * radius, y2 = cy + segment.y2() * radius;
            HologramStyle.line(buffer, g.pose(), x1, y1, x2, y2, segment.color(), 1f);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
