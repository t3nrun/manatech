package com.vital.manatech;

import com.vital.manatech.block.ModBlocks;
import com.vital.manatech.block.entity.ModBlockEntities;
import com.vital.manatech.component.ModComponents;
import com.vital.manatech.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.vital.manatech.network.ExportLayerPayload;
import com.vital.manatech.network.SelectGrimoireLayerPayload;
import com.vital.manatech.network.SaveCanvasPayload;
import com.vital.manatech.network.ManaSyncPayload;
import com.vital.manatech.network.OverlayEditPayload;
import com.vital.manatech.network.CastBeamPayload;

@Mod(ManatechMod.MOD_ID)
public class ManatechMod {
    public static final String MOD_ID = "manatech";

    public ManatechMod(IEventBus bus) {
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModComponents.COMPONENTS.register(bus);
        ModTabs.TABS.register(bus);
        com.vital.manatech.menu.ModMenus.MENUS.register(bus);
        bus.addListener(com.vital.manatech.machine.MachineCapabilities::register);
        bus.addListener(ExportLayerPayload::register);
        bus.addListener(SelectGrimoireLayerPayload::register);
        bus.addListener(SaveCanvasPayload::register);
        bus.addListener(ManaSyncPayload::register);
        bus.addListener(OverlayEditPayload::register);
        bus.addListener(CastBeamPayload::register);
    }
}
