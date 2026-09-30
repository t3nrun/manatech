package com.vital.manatech.client;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.magic.ManaProgression;
import com.vital.manatech.network.ManaSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ManatechMod.MOD_ID, value = Dist.CLIENT)
public final class ManaHud {
    private static double mana = 128, maximum = 128;
    private static int level = 1;
    private static long absorbed;
    private ManaHud() {}
    public static void sync(ManaSyncPayload packet) {
        mana = packet.mana(); maximum = packet.maximum(); level = Math.clamp(packet.level(), 1, 100); absorbed = packet.absorbed();
    }
    public static int playerLevel() { return level; }
    public static int circle() { return ManaProgression.rank(level); }
    public static String number(double value) {
        return com.vital.manatech.util.CompactNumbers.format(value);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = 8, bottom = g.guiHeight() - 10, top = bottom - 76;
        g.fill(x + 4, top - 5, x + 18, top, 0xEFA0B4CF);
        g.fill(x + 6, top - 3, x + 16, top, 0xFF07152B);
        g.fill(x + 2, top, x + 20, top + 5, 0xEFA0B4CF);
        g.fill(x, top + 5, x + 22, bottom - 4, 0xDFA0B4CF);
        g.fill(x + 2, bottom - 4, x + 20, bottom, 0xEFA0B4CF);
        g.fill(x + 2, top + 5, x + 20, bottom - 4, 0xE8071024);
        int height = bottom - 4 - (top + 6);
        int fill = Math.round((float)Math.clamp(mana / Math.max(1, maximum), 0, 1) * height);
        if (fill > 0) {
            int surface = bottom - 4 - fill;
            g.fillGradient(x + 3, surface, x + 19, bottom - 4, 0xFF164EB9, 0xFF040B42);
            g.fill(x + 3, surface, x + 19, surface + 1, 0xFF4484ED);
            g.fill(x + 4, surface + 1, x + 5, bottom - 5, 0x703B6CD4);
        }
        g.fill(x + 3, top + 6, x + 4, bottom - 5, 0x60DAE8F7);
        for (int i = 1; i < 4; i++) g.fill(x + 15, top + 6 + height * i / 4, x + 19, top + 7 + height * i / 4, 0x90CADCF4);
        g.drawString(mc.font, Component.translatable("hud.manatech.level", level), x + 28, bottom - 31, 0xFFC4D9F8);
        g.drawString(mc.font, number(mana) + "/" + number(maximum), x + 28, bottom - 18, 0xFF8EAEE5);
        int rank = circle();
        int progress = level >= 100 ? 46 : (int)Math.clamp(46d * absorbed / ManaProgression.rankAbsorption(rank), 0, 46);
        g.fill(x + 28, bottom - 5, x + 74, bottom - 3, 0xA0243047);
        g.fill(x + 28, bottom - 5, x + 28 + progress, bottom - 3, 0xFF638ACC);
    }
}
