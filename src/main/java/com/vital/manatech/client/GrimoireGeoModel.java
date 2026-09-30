package com.vital.manatech.client;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.item.RuneGrimoireItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GrimoireGeoModel extends GeoModel<RuneGrimoireItem> {
    @Override public ResourceLocation getModelResource(RuneGrimoireItem item) {
        return ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "geo/rune_grimoire.geo.json");
    }
    @Override public ResourceLocation getTextureResource(RuneGrimoireItem item) {
        return ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "textures/item/rune_grimoire.png");
    }
    @Override public ResourceLocation getAnimationResource(RuneGrimoireItem item) {
        return ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "animations/rune_grimoire.animation.json");
    }
}
