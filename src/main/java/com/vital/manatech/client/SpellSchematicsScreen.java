package com.vital.manatech.client;

import com.vital.manatech.magic.SpellSchematics;
import com.vital.manatech.magic.MagicModifier;
import com.vital.manatech.rune.SchematicDiagrams;
import com.vital.manatech.rune.CircleEffectMesh;
import com.vital.manatech.rune.hex.SpellElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A reference book: viewing does not author, export, unlock or cast a spell. */
public final class SpellSchematicsScreen extends Screen {
    private final Screen parent;
    private int element,selected,first,page;
    private float zoom=1;
    private Button elementButton,pageButton;
    private SpellSchematics.Recipe cachedRecipe;
    private List<com.vital.manatech.rune.DiagramLayer> cachedPages=List.of();
    private List<CircleEffectMesh> cachedMeshes=List.of();
    public SpellSchematicsScreen(Screen parent) { super(Component.translatable("screen.manatech.schematics"));this.parent=parent; }
    private List<SpellSchematics.Recipe> entries() { return SpellSchematics.ALL.stream().filter(r->r.element()==element).toList(); }
    private int leftWidth() { return Math.clamp(width/3,90,210); }
    private int rows() { return Math.max(1,(height-100)/20); }
    private Component elementName() { return Component.translatable("element.manatech."+SpellElement.values()[element].id()); }
    private List<com.vital.manatech.rune.DiagramLayer> diagrams() {
        var recipe=entries().get(selected);
        if(recipe!=cachedRecipe) {
            cachedRecipe=recipe;cachedPages=SchematicDiagrams.pages(recipe);
            var meshes=new java.util.ArrayList<CircleEffectMesh>();
            for(var layer:cachedPages)meshes.add(CircleEffectMesh.generate(List.of(layer)));
            meshes.add(CircleEffectMesh.generate(cachedPages));cachedMeshes=List.copyOf(meshes);
        }
        return cachedPages;
    }
    @Override protected void init() {
        elementButton=addRenderableWidget(Button.builder(elementName(),b->{element=(element+1)%6;selected=first=page=0;elementButton.setMessage(elementName());updatePage();}).bounds(12,32,leftWidth()-20,20).build());
        pageButton=addRenderableWidget(Button.builder(Component.literal("< >"),b->{page=(page+1)%(diagrams().size()+1);updatePage();}).bounds(leftWidth()+10,height-28,Math.max(60,width-leftWidth()-110),20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.manatech.schematic_back"),b->onClose()).bounds(width-85,height-28,73,20).build());
        updatePage();
    }
    private void updatePage() {
        var recipe=entries().get(selected);int size=diagrams().size();
        page=Math.min(page,size);
        pageButton.setMessage(page==size?Component.translatable("screen.manatech.schematic_combined"):Component.translatable("screen.manatech.schematic_layer",page+1,size,page+1));
        pageButton.active=true;
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick) { g.fill(0,0,width,height,0xFF10121C); }
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        super.render(g,x,y,tick);
        g.drawCenteredString(font,title,width/2,14,0xFFE7DCF7);
        int left=leftWidth();g.fill(left,32,left+1,height-32,0xFF564466);
        var entries=entries();
        for(int row=0;row<rows()&&first+row<entries.size();row++) {
            int index=first+row,top=60+row*20;
            if(index==selected)g.fill(10,top-2,left-8,top+17,0xFF3D304F);
            var recipe=entries.get(index);
            String label=recipe.circle()+" · "+Component.translatable(recipe.key()).getString();
            g.drawString(font,font.plainSubstrByWidth(label,left-30),16,top+3,index==selected?0xFFFFFFFF:0xFFBFB6CE,false);
        }
        var recipe=entries.get(selected);int cx=(left+width)/2;
        var name=Component.translatable(recipe.key());
        g.drawCenteredString(font,font.plainSubstrByWidth(name.getString(),width-left-18),cx,36,0xFFE9DAFF);
        g.drawCenteredString(font,Component.translatable("screen.manatech.schematic_circle",recipe.circle(),recipe.toolTier()),cx,51,0xFFBAACCA);
        int symbolRows=(recipe.symbols().size()+1)/2;
        int radius=Math.max(12,Math.min((width-left-30)/2,(height-120-symbolRows*10)/2));
        int cy=68+radius;
        var diagrams=diagrams();
        int textTop=cy+radius+5;
        g.enableScissor(left+6,66,width-6,textTop-2);
        boolean combined=page==diagrams.size();
        CirclePreview.draw(g,cachedMeshes.get(page),cx,cy,Math.round(radius*zoom/(combined?1:.62f+page*.025f)));
        g.disableScissor();
        var legend=combined?diagrams.get(recipe.layer()):diagrams.get(page);
        int color=SpellElement.values()[legend.schemeElement()].color();
        int symbolIndex=0;
        for(var symbol:legend.symbols()) {
            var modifier=MagicModifier.bySymbol(symbol.id());
            String slot=symbol.slot()==8?"• ":(symbol.slot()+1)+": ";
            int columnWidth=(width-left-20)/2;
            g.drawString(font,font.plainSubstrByWidth(slot+Component.translatable("modifier.manatech."+modifier.id()).getString(),columnWidth-4),left+10+symbolIndex%2*columnWidth,textTop+symbolIndex/2*10,color,false);
            symbolIndex++;
        }
        Component status=Component.translatable(recipe.circle()==7?"screen.manatech.schematic_future":recipe.implemented()?"screen.manatech.schematic_ready":"screen.manatech.schematic_planned");
        g.drawCenteredString(font,font.plainSubstrByWidth(status.getString(),width-left-18),cx,height-43,0xFFBFB1C9);
        if(x>left&&y>66&&y<textTop-2)g.renderTooltip(font,Component.translatable("screen.manatech.schematic_hint"),x,y);
        if(x<left&&y>=60&&y<60+rows()*20) {
            int index=first+(y-60)/20;
            if(index<entries.size())g.renderTooltip(font,Component.translatable(entries.get(index).key()),x,y);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&x>=10&&x<leftWidth()-8&&y>=58&&y<60+rows()*20) {
            int index=first+(int)((y-60)/20);
            if(index>=0&&index<entries().size()) { selected=index;page=0;updatePage();return true; }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x<leftWidth()) { first=Math.clamp(first-(int)Math.signum(vertical)*3,0,Math.max(0,entries().size()-rows()));return true; }
        zoom=Math.clamp(zoom+(float)vertical*.2f,1,3);return true;
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
