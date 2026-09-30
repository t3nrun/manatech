package com.vital.manatech.client;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.rune.DiagramLayer;
import com.vital.manatech.rune.CircleEffectMesh;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.vital.manatech.network.SelectGrimoireLayerPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Page-by-page viewer; other layers stay hidden until explicitly selected. */
public final class GrimoireScreen extends Screen {
    private final List<String> pages;
    private final List<String> spells;
    private int selected;
    private boolean viewingSpells;

    public GrimoireScreen(ItemStack book) {
        super(Component.translatable("item.manatech.rune_grimoire"));
        pages = List.copyOf(book.getOrDefault(ModComponents.GRIMOIRE, List.of()));
        spells = List.copyOf(book.getOrDefault(ModComponents.SPELLS, List.of()));
        viewingSpells = !spells.isEmpty();
        selected = viewingSpells ? book.getOrDefault(ModComponents.SPELL_SELECTION, 0) : book.getOrDefault(ModComponents.GRIMOIRE_SELECTION, 0);
        selected = Math.max(0, Math.min(selected, Math.max(0, (viewingSpells ? spells : pages).size()-1)));
    }

    @Override protected void init() {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("screen.manatech.schematics"),
            button -> minecraft.setScreen(new SpellSchematicsScreen(this))).bounds(width-120,25,100,20).build());
    }

    @Override public void renderBackground(GuiGraphics g,int x,int y,float p) {
        g.fill(0,0,width,height,0xFF1A151A);
        g.fill(20,18,width-20,height-18,0xFF46334E);
        g.fill(25,23,width-25,height-23,0xFF181522);
    }

    @Override public void render(GuiGraphics g,int x,int y,float p) {
        super.render(g,x,y,p);
        g.drawCenteredString(font,title,width/2,32,0xFFE5D6FA);
        List<String> entries = viewingSpells ? spells : pages;
        g.drawCenteredString(font, Component.translatable(viewingSpells ? "screen.manatech.spells_tab" : "screen.manatech.pages_tab"), width / 2, 49, 0xFFC5A5F4);
        if (entries.isEmpty()) {
            g.drawCenteredString(font,Component.translatable("message.manatech.grimoire_empty"),width/2,height/2,0xFFE5D6FA);
        } else {
            CircleEffectMesh mesh = CircleEffectMesh.generate(viewingSpells
                    ? com.vital.manatech.rune.AssembledSpell.read(entries.get(selected)).diagrams()
                    : List.of(DiagramLayer.decode(entries.get(selected))));
            int cx=width/2, cy=height/2, radius=Math.max(30,Math.min(width/2-50,height/2-75));
            // Preview the generated EFFECT model, not a picture of saved points.
            CirclePreview.draw(g, mesh, cx, cy, radius);
            if(viewingSpells) {
                var assembly=com.vital.manatech.rune.SpellAssembly.inspect(com.vital.manatech.rune.AssembledSpell.read(entries.get(selected)));
                g.drawCenteredString(font,assembly.valid()?Component.translatable("screen.manatech.assembly_count",assembly.present(),assembly.required()):Component.translatable("message.manatech.assembly_"+assembly.issue(),assembly.layer(),assembly.required()),cx,height-70,assembly.valid()?0xFF83D6B0:0xFFF2A56A);
                var fire=com.vital.manatech.magic.FireSpells.resolve(com.vital.manatech.rune.AssembledSpell.read(entries.get(selected)));
                if(fire!=null) g.drawCenteredString(font,Component.translatable("spell.manatech.fire."+fire.id()),cx,height-82,0xFFFFB69B);
            }
            g.drawCenteredString(font,Component.translatable("message.manatech.layer_selected",selected+1,entries.size()),cx,height-38,0xFFE5D6FA);
            g.drawCenteredString(font,"<                                        >",cx,height-56,0xFFC5A5F4);
        }
    }

    @Override public boolean mouseClicked(double x,double y,int button) {
        if (button == 0 && y >= 42 && y < 65) {
            viewingSpells = !viewingSpells;
            List<String> entries = viewingSpells ? spells : pages;
            selected = Math.min(selected, Math.max(0, entries.size() - 1));
            if (!entries.isEmpty()) PacketDistributor.sendToServer(new SelectGrimoireLayerPayload(selected, viewingSpells));
            return true;
        }
        List<String> entries = viewingSpells ? spells : pages;
        if(button==0&&!entries.isEmpty()) {
            if(y>height-72&&y<height-42) {
                if(x<width/2)selected=(selected+entries.size()-1)%entries.size();
                else selected=(selected+1)%entries.size();
                PacketDistributor.sendToServer(new SelectGrimoireLayerPayload(selected, viewingSpells));
                return true;
            }
        }
        return super.mouseClicked(x,y,button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
