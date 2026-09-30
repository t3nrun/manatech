package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vital.manatech.block.entity.ManaPlateBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ManaPlateRenderer implements BlockEntityRenderer<ManaPlateBlockEntity> {
    public ManaPlateRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ManaPlateBlockEntity plate,float partialTick,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        var spell=plate.displaySpell();
        if(!spell.isEmpty()) WorldCircleRenderer.render(spell.diagrams(),pose,buffer,.5f,.3f,.5f,plate.getLevel()==null?0:plate.getLevel().getGameTime()+partialTick);
    }
}
