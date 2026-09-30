package com.vital.manatech.client;

import com.vital.manatech.menu.ProcessingMenu;
import com.vital.manatech.machine.CuttingGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ProcessingScreen extends MachineScreen<ProcessingMenu> {
    private Button cut;
    public ProcessingScreen(ProcessingMenu menu,Inventory inv,Component title) { super(menu,inv,title); }
    @Override protected void init() {
        super.init();
        cut=addRenderableWidget(Button.builder(Component.translatable("screen.manatech.cut"),b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+38,topPos+116,100,20).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        cut.active=menu.getSlot(0).hasItem() && !menu.getSlot(1).hasItem();
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        panel(g);
        text(g,"screen.manatech.cut_step",18,0xFF283C58,Math.min(menu.step(),6),6);
        // An irregular crystal becomes six clean facets as cuts succeed.
        int cx=leftPos+88,cy=topPos+61;
        for(int dy=-23;dy<=23;dy++) {
            int half=27-Math.abs(dy)/2;
            g.fill(cx-half,cy+dy,cx+half,cy+dy+1,dy<0?0xFF43BDD5:0xFF247FA8);
        }
        for(int i=0;i<6;i++) {
            double a=i*Math.PI/3-Math.PI/2,b=(i+1)*Math.PI/3-Math.PI/2;
            int ax=cx+(int)(Math.cos(a)*25),ay=cy+(int)(Math.sin(a)*23);
            int bx=cx+(int)(Math.cos(b)*25),by=cy+(int)(Math.sin(b)*23);
            line(g,ax,ay,bx,by,i<menu.step()?0xFFCAF9FF:0xFF205573);
            if(i<menu.step()) line(g,cx,cy,ax,ay,0xFF86E6F7);
        }
        int bar=leftPos+23, by=topPos+94;
        g.fill(bar,by,bar+130,by+12,0xFF252B3C);
        int target=menu.target()*126/100;
        g.fill(bar+Math.max(0,target-15),by+1,bar+Math.min(130,target+15),by+11,0xFF469C71);
        int cursor=bar+2+menu.cursor()*126/100;
        g.fill(cursor,by-2,cursor+2,by+14,0xFFF6EECE);
        String key=switch(menu.status()) { case 1 -> "screen.manatech.cut_miss"; case 3 -> "screen.manatech.cut_done"; default -> "screen.manatech.cut_hint"; };
        text(g,key,107,0xFF3E4B60);
    }
    private void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color) {
        int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));
        for(int i=0;i<=steps;i++) { int x=x1+(x2-x1)*i/Math.max(1,steps),y=y1+(y2-y1)*i/Math.max(1,steps); g.fill(x,y,x+1,y+1,color); }
    }
}
