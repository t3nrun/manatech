package com.vital.manatech.client;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.entity.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ManatechMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void openCanvas() {
        Minecraft.getInstance().setScreen(new RuneCanvasScreen(1, net.minecraft.core.BlockPos.ZERO));
    }

    public static void openCanvas(int stylusLevel, int tableLevel, net.minecraft.core.BlockPos pos) {
        Minecraft.getInstance().setScreen(new RuneCanvasScreen(com.vital.manatech.rune.RuneTier.effectiveTier(stylusLevel, tableLevel), stylusLevel, pos));
    }

    public static void openGrimoire(net.minecraft.world.item.ItemStack book) {
        Minecraft.getInstance().setScreen(new GrimoireScreen(book));
    }
    public static void openOverlay(com.vital.manatech.block.entity.OverlayTableBlockEntity table) {
        Minecraft.getInstance().setScreen(new OverlayScreen(table));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_PLATE.get(), ManaPlateRenderer::new);
    }

    @SubscribeEvent
    public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.vital.manatech.menu.ModMenus.PROCESSING.get(), ProcessingScreen::new);
        event.register(com.vital.manatech.menu.ModMenus.FURNACE.get(), AdamantiteScreen::new);
    }
}
