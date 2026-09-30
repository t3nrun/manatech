package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vital.manatech.block.RuneTableBlock;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import com.vital.manatech.rune.hex.RuneGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

public class RuneTableRenderer implements BlockEntityRenderer<RuneTableBlockEntity> {
    public RuneTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RuneTableBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (table.schematic().grid().isEmpty()) {
            return;
        }
        var facing = table.getBlockState().getValue(RuneTableBlock.FACING);
        var right = facing.getClockWise();
        pose.pushPose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        for (var entry : table.schematic().grid().cells().entrySet()) {
            float across = 0.5F + entry.getKey().q() / 4.0F;
            float forward = entry.getKey().r() / 4.0F;
            float x = 0.5F + across * right.getStepX() + forward * facing.getStepX();
            float z = 0.5F + across * right.getStepZ() + forward * facing.getStepZ();
            int color = color(entry.getValue());
            float red = ((color >> 16) & 255) / 255.0F;
            float green = ((color >> 8) & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            float half = 0.045F;
            vertex(consumer, matrix, x - half, z - half, red, green, blue);
            vertex(consumer, matrix, x - half, z + half, red, green, blue);
            vertex(consumer, matrix, x + half, z + half, red, green, blue);
            vertex(consumer, matrix, x + half, z - half, red, green, blue);
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float z, float r, float g, float b) {
        consumer.addVertex(matrix, x, 0.85F, z).setColor(r, g, b, 0.9F).setUv(0, 0).setOverlay(0).setLight(0xF000F0).setNormal(0, 1, 0);
    }

    private static int color(RuneGlyph glyph) {
        return switch (glyph) {
            case FOCUS -> 0xF2D36B;
            case LINK -> 0x6EC8FF;
            case AMPLIFY -> 0xFF6B6B;
            case BIND -> 0xC58BFF;
            case VENT -> 0x7DFFB2;
            case BLANK -> 0xFFFFFF;
        };
    }
}
