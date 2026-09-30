package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vital.manatech.component.ModComponents;
import com.vital.manatech.item.RuneGrimoireItem;
import com.vital.manatech.rune.AssembledSpell;
import com.vital.manatech.rune.CircleEffectMesh;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

/** Projects the selected spell's authored layers over the animated book. */
public final class GrimoireCircleLayer extends GeoRenderLayer<RuneGrimoireItem> {
    public GrimoireCircleLayer(GeoRenderer<RuneGrimoireItem> renderer) { super(renderer); }

    @Override
    public void render(PoseStack pose, RuneGrimoireItem item, BakedGeoModel model, RenderType type,
                       MultiBufferSource buffers, VertexConsumer ignored, float partialTick, int light, int overlay) {
        if (!(renderer instanceof GrimoireItemRenderer grimoire)) return;
        ItemStack stack = grimoire.getCurrentItemStack();
        List<String> spells = stack.getOrDefault(ModComponents.SPELLS, List.of());
        List<DiagramLayer> layers;
        if (!spells.isEmpty()) {
            int selected = Math.clamp(stack.getOrDefault(ModComponents.SPELL_SELECTION, 0), 0, spells.size() - 1);
            layers = AssembledSpell.read(spells.get(selected)).diagrams();
        } else {
            List<String> pages = stack.getOrDefault(ModComponents.GRIMOIRE, List.of());
            if (pages.isEmpty()) return;
            int selected = Math.clamp(stack.getOrDefault(ModComponents.GRIMOIRE_SELECTION, 0), 0, pages.size() - 1);
            layers = List.of(DiagramLayer.decode(pages.get(selected)));
        }
        CircleEffectMesh mesh = CircleEffectMesh.generate(layers);
        pose.pushPose();
        pose.translate(0, 0.18f, 0);
        pose.scale(.16f, .16f, .16f);
        DiagramRenderer.render(mesh, pose, buffers, 1f, (float)(item.getTick(null) + partialTick) * .08f);
        pose.popPose();
    }
}
