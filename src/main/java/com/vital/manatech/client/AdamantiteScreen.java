package com.vital.manatech.client;

import com.vital.manatech.menu.AdamantiteMenu;
import com.vital.manatech.block.entity.AdamantiteFurnaceBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdamantiteScreen extends MachineScreen<AdamantiteMenu> {
    public AdamantiteScreen(AdamantiteMenu menu,Inventory inv,Component title) { super(menu,inv,title); }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        panel(g);
        text(g,menu.formed()?"screen.manatech.furnace_formed":"screen.manatech.furnace_broken",20,menu.formed()?0xFF287342:0xFFAC2D2D);
        text(g,"screen.manatech.furnace_heat",34,0xFF683C37,menu.heat()/20);
        int bx=leftPos+75,by=topPos+62;
        g.fill(bx,by,bx+27,by+8,0xFF3C3841);
        g.fill(bx,by,bx+27*menu.progress()/AdamantiteFurnaceBlockEntity.SMELT_TICKS,by+8,0xFFE7A04B);
        g.fill(leftPos+22,topPos+89,leftPos+154,topPos+96,0xFF3C3841);
        g.fill(leftPos+22,topPos+89,leftPos+22+132*menu.heat()/AdamantiteFurnaceBlockEntity.HEAT_TICKS,topPos+96,0xFFE25B31);
        text(g,"screen.manatech.furnace_progress",104,0xFF483F46,menu.progress()*100/AdamantiteFurnaceBlockEntity.SMELT_TICKS);
        text(g,"screen.manatech.furnace_heat_hint",123,0xFF683C37);
    }
}
