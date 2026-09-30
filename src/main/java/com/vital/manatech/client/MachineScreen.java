package com.vital.manatech.client;

import com.vital.manatech.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {
    protected MachineScreen(T menu,Inventory inv,Component title) { super(menu,inv,title); imageWidth=176; imageHeight=236; inventoryLabelY=142; }
    protected void panel(GuiGraphics g) {
        g.fill(leftPos,topPos,leftPos+176,topPos+236,0xFF222735);
        g.fill(leftPos+2,topPos+2,leftPos+174,topPos+234,0xFFE0D4BB);
        for(var slot:menu.slots) {
            int x=leftPos+slot.x,y=topPos+slot.y;
            g.fill(x-1,y-1,x+17,y+17,0xFF4A4B55); g.fill(x,y,x+16,y+16,0xFFAAA694);
        }
    }
    protected void text(GuiGraphics g,String key,int y,int color,Object...args) { g.drawCenteredString(font,Component.translatable(key,args),leftPos+88,topPos+y,color); }
    @Override public void render(GuiGraphics g,int x,int y,float partial) { super.render(g,x,y,partial); renderTooltip(g,x,y); }
}
