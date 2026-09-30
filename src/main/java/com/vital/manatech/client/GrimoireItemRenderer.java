package com.vital.manatech.client;

import com.vital.manatech.item.RuneGrimoireItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class GrimoireItemRenderer extends GeoItemRenderer<RuneGrimoireItem> {
    public GrimoireItemRenderer() {
        super(new GrimoireGeoModel());
        addRenderLayer(new GrimoireCircleLayer(this));
    }
}
