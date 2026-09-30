package com.vital.manatech.client;

import com.vital.manatech.block.entity.OverlayTableBlockEntity;
import com.vital.manatech.block.entity.OverlayTableBlockEntity.Placement;
import com.vital.manatech.network.OverlayEditPayload;
import com.vital.manatech.rune.CircleEffectMesh;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** A page library and a separate, editable bottom-to-top composition. */
public final class OverlayScreen extends Screen {
    private int panelWidth() { return Math.clamp(width / 4, 70, 120); }
    private static final int ROW_HEIGHT = 20;
    private final OverlayTableBlockEntity table;
    private final List<String> pages;
    private final List<Placement> placements;
    private CircleEffectMesh preview;
    private com.vital.manatech.rune.SpellAssembly.Result assembly;
    private int selectedSource;
    private int selectedPlacement;
    private int sourceScroll;
    private int placementScroll;
    private boolean dragging;

    public OverlayScreen(OverlayTableBlockEntity table) {
        super(Component.translatable("block.manatech.overlay_table"));
        this.table = table;
        pages = table.pages();
        placements = new ArrayList<>(table.placements());
        selectedSource = pages.isEmpty() ? -1 : 0;
        selectedPlacement = placements.isEmpty() ? -1 : placements.size() - 1;
        rebuildPreview();
    }

    private int centerX() { return width / 2; }
    private int centerY() { return height / 2 + 4; }
    private int radius() { return Math.max(35, Math.min((width - 2 * panelWidth() - 70) / 2, (height - 150) / 2)); }
    private int rightX() { return width - panelWidth() - 12; }
    private int rows() { return Math.max(1, (height - 178) / ROW_HEIGHT); }

    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) {
        g.fill(0, 0, width, height, 0xFF11121D);
        g.fill(8, 48, 12 + panelWidth(), height - 105, 0xFF242335);
        g.fill(rightX(), 48, width - 8, height - 105, 0xFF242335);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        super.render(g, mouseX, mouseY, tick);
        g.drawCenteredString(font, title, centerX(), 20, 0xFFE8D6FF);
        g.drawString(font, Component.translatable("screen.manatech.overlay_pages"), 16, 54, 0xFFE8D6FF);
        g.drawString(font, Component.translatable("screen.manatech.overlay_layers"), rightX() + 5, 54, 0xFFE8D6FF);
        int visible = rows();
        sourceScroll = Math.clamp(sourceScroll, 0, Math.max(0, pages.size() - visible));
        placementScroll = Math.clamp(placementScroll, 0, Math.max(0, placements.size() - visible));
        for (int row = 0; row < visible && sourceScroll + row < pages.size(); row++) {
            int index = sourceScroll + row;
            int top = 72 + row * ROW_HEIGHT;
            g.fill(12, top, 12 + panelWidth(), top + 18, index == selectedSource ? 0xFF57467B : 0xFF343248);
            g.drawString(font, Component.translatable("screen.manatech.overlay_page", index + 1), 17, top + 5, 0xFFE9E2F8);
        }
        for (int row = 0; row < visible && placementScroll + row < placements.size(); row++) {
            int index = placementScroll + row;
            Placement placement = placements.get(index);
            int top = 72 + row * ROW_HEIGHT;
            g.fill(rightX(), top, rightX() + panelWidth(), top + 18,
                    index == selectedPlacement ? 0xFF57467B : 0xFF343248);
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("screen.manatech.overlay_layer", index + 1,
                    placement.source() + 1).getString(), panelWidth() - 10), rightX() + 5, top + 5, 0xFFE9E2F8);
        }

        CirclePreview.draw(g, preview, centerX(), centerY(), radius());
        if (selectedPlacement >= 0 && selectedPlacement < placements.size()) {
            Placement selected = placements.get(selectedPlacement);
            float automatic = automaticScale(selectedPlacement);
            float angle=com.vital.manatech.rune.LayerLayout.rotation(selectedPlacement,placements.size());
            float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            int markerX=centerX()+Math.round((selected.x()*c-selected.y()*s)*radius()*automatic/1000f);
            int markerY=centerY()+Math.round((selected.x()*s+selected.y()*c)*radius()*automatic/1000f);
            g.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFFFFD66C);
            g.drawCenteredString(font, selected.scale() + "%  /  " + selected.rotation() + "°",
                    centerX(), height - 93, 0xFFE7D8FF);
        }

        button(g, 12, height - 91, panelWidth(), 23,
                Component.translatable("screen.manatech.overlay_add").getString());
        String[] orderButtons = {"↑", "↓", "×"}, editButtons = {"−", "+", "↶", "↷"};
        for (int i = 0; i < 3; i++) button(g, rightX() + i * panelWidth() / 3, height - 91, panelWidth() / 3 - 2, 23, orderButtons[i]);
        for (int i = 0; i < 4; i++) button(g, rightX() + i * panelWidth() / 4, height - 64, panelWidth() / 4 - 2, 22, editButtons[i]);
        button(g,centerX()-42,height-63,84,21,Component.translatable("screen.manatech.overlay_align").getString());
        g.drawCenteredString(font,Component.translatable("screen.manatech.assembly_count",assembly.present(),assembly.required()),centerX(),height-38,assembly.valid()?0xFF83D6B0:0xFFF2A56A);
        g.drawCenteredString(font, font.plainSubstrByWidth(Component.translatable("screen.manatech.overlay_hint").getString(), width - 16),
                centerX(), height - 24, 0xFFD2C7E6);
    }

    private void button(GuiGraphics g, int x, int y, int width, int height, String label) {
        g.fill(x, y, x + width, y + height, 0xFF4C3D68);
        g.drawCenteredString(font, label, x + width / 2, y + 7, 0xFFFFFFFF);
    }

    private void send(int action, int index, int first, int second) {
        PacketDistributor.sendToServer(new OverlayEditPayload(table.getBlockPos(), action, index, first, second));
    }

    private void rebuildPreview() {
        List<String> assembled = OverlayTableBlockEntity.assemble(pages, placements);
        preview = CircleEffectMesh.generate(assembled.stream().map(DiagramLayer::decode).toList());
        assembly=com.vital.manatech.rune.SpellAssembly.inspect(new com.vital.manatech.rune.AssembledSpell(assembled));
    }

    private float automaticScale(int index) {
        var page=DiagramLayer.decode(pages.get(placements.get(index).source()));
        return com.vital.manatech.rune.LayerLayout.scale(index,placements.size(),page);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return super.mouseClicked(x, y, button);
        if(y>=height-63&&y<height-42&&x>=centerX()-42&&x<centerX()+42) {
            placements.sort(java.util.Comparator.comparingInt(p->DiagramLayer.decode(pages.get(p.source())).schemeLayer()));
            for(int i=0;i<placements.size();i++)placements.set(i,new Placement(placements.get(i).source(),0,0,100,0));
            rebuildPreview();send(OverlayEditPayload.ALIGN,0,0,0);return true;
        }
        if (y >= 72 && y < 72 + rows() * ROW_HEIGHT) {
            int row = (int)(y - 72) / ROW_HEIGHT;
            if (x >= 12 && x < 12 + panelWidth() && sourceScroll + row < pages.size()) {
                selectedSource = sourceScroll + row;
                return true;
            }
            if (x >= rightX() && x < rightX() + panelWidth() && placementScroll + row < placements.size()) {
                selectedPlacement = placementScroll + row;
                return true;
            }
        }
        if (y >= height - 91 && y < height - 68) {
            if (x >= 12 && x < 12 + panelWidth()) {
                int limit = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild ? 16 : 12;
                if (selectedSource >= 0 && placements.size() < limit) {
                    placements.add(new Placement(selectedSource, 0, 0, 100, 0));
                    selectedPlacement = placements.size() - 1;
                    placementScroll = Math.max(0, placements.size() - rows());
                    rebuildPreview();
                    send(OverlayEditPayload.ADD, selectedSource, 0, 0);
                }
                return true;
            }
            if (selectedPlacement >= 0 && x >= rightX() && x < rightX() + panelWidth()) {
                int action = (int)((x - rightX()) * 3 / panelWidth());
                if (action == 0) reorder(-1);
                else if (action == 1) reorder(1);
                else removeSelected();
                return true;
            }
        }
        if (y >= height - 64 && y < height - 42 && selectedPlacement >= 0
                && x >= rightX() && x < rightX() + panelWidth()) {
            int action = (int)((x - rightX()) * 4 / panelWidth());
            if (action == 0) resize(-10);
            else if (action == 1) resize(10);
            else if (action == 2) rotate(-15);
            else rotate(15);
            return true;
        }
        if (selectedPlacement >= 0 && Math.hypot(x - centerX(), y - centerY()) <= radius()) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private void reorder(int direction) {
        int other = selectedPlacement + direction;
        if (other < 0 || other >= placements.size()) return;
        Placement selected = placements.get(selectedPlacement);
        placements.set(selectedPlacement, placements.get(other));
        placements.set(other, selected);
        rebuildPreview();
        send(OverlayEditPayload.REORDER, selectedPlacement, direction, 0);
        selectedPlacement = other;
    }

    private void removeSelected() {
        send(OverlayEditPayload.REMOVE, selectedPlacement, 0, 0);
        placements.remove(selectedPlacement);
        selectedPlacement = Math.min(selectedPlacement, placements.size() - 1);
        rebuildPreview();
    }

    private void resize(int change) {
        Placement old = placements.get(selectedPlacement);
        int scale = Math.clamp(old.scale() + change, 25, 200);
        placements.set(selectedPlacement, new Placement(old.source(), old.x(), old.y(), scale, old.rotation()));
        rebuildPreview();
        send(OverlayEditPayload.SCALE, selectedPlacement, scale, 0);
    }

    private void rotate(int change) {
        Placement old = placements.get(selectedPlacement);
        int rotation = Math.floorMod(old.rotation() + change, 360);
        placements.set(selectedPlacement, new Placement(old.source(), old.x(), old.y(), old.scale(), rotation));
        rebuildPreview();
        send(OverlayEditPayload.ROTATE, selectedPlacement, rotation, 0);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= 12 && x < 12 + panelWidth()) {
            sourceScroll = Math.clamp(sourceScroll - (int)Math.signum(vertical), 0,
                    Math.max(0, pages.size() - rows()));
            return true;
        }
        if (x >= rightX() && x < rightX() + panelWidth()) {
            placementScroll = Math.clamp(placementScroll - (int)Math.signum(vertical), 0,
                    Math.max(0, placements.size() - rows()));
            return true;
        }
        if (selectedPlacement >= 0 && vertical != 0) {
            if (hasShiftDown()) rotate(vertical > 0 ? 15 : -15);
            else resize(vertical > 0 ? 10 : -10);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && dragging && selectedPlacement >= 0) {
            Placement old = placements.get(selectedPlacement);
            float step = 1000f / (radius() * automaticScale(selectedPlacement));
            float angle=com.vital.manatech.rune.LayerLayout.rotation(selectedPlacement,placements.size());
            float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            int movedX=Math.clamp(old.x()+Math.round(((float)dx*c+(float)dy*s)*step),-1000,1000);
            int movedY=Math.clamp(old.y()+Math.round((-(float)dx*s+(float)dy*c)*step),-1000,1000);
            placements.set(selectedPlacement, new Placement(old.source(), movedX, movedY,
                    old.scale(), old.rotation()));
            rebuildPreview();
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            if (selectedPlacement >= 0) {
                Placement placement = placements.get(selectedPlacement);
                send(OverlayEditPayload.MOVE, selectedPlacement, placement.x(), placement.y());
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
