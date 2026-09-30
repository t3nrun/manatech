package com.vital.manatech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.vital.manatech.rune.hex.SpellElement;
import com.vital.manatech.rune.RuneTier;
import com.vital.manatech.rune.ElementSchemes;
import com.vital.manatech.rune.DiagramLayer;
import com.vital.manatech.rune.CircleEffectMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import com.vital.manatech.network.ExportLayerPayload;
import com.vital.manatech.network.SaveCanvasPayload;
import com.vital.manatech.block.RuneTableBlock;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Each layer is an independent ink canvas bounded by its visible guides. */
public final class RuneCanvasScreen extends Screen {
    private final int tier;
    private final int layerCount;
    private final int symbolCount;
    private final int slotsPerLayer;
    private final int symbolLimit;
    private final List<Integer> visibleSlots;
    private final List<Layer> layers = new ArrayList<>();
    private final BlockPos tablePos;
    private int activeLayer;
    private static int selectedSymbol;
    private GuidePath activePath;
    private double pathPosition;
    private double pathTravel;
    private boolean strokeClosed;
    private static SpellElement selectedElement = SpellElement.VOID;
    private static boolean symbolMode;
    private boolean showOtherLayers;
    private Stroke currentStroke;
    private final List<Spark> sparks = new ArrayList<>();
    private final Random sparkRandom = new Random();
    private BufferBuilder inkBuffer;
    private boolean restored;
    private static final float INK_RADIUS = .72f;
    private static final double GUIDE_REACH = 14;
    private float symbolRadius() { return Math.min(12f, guideRadius() * .07f); }

    private static final class Layer {
        SpellElement element = selectedElement;
        final List<Stroke> strokes = new ArrayList<>();
        final List<PlacedSymbol> symbols = new ArrayList<>();
    }
    private record Point(float x, float y) {}
    private record Stroke(SpellElement element, List<Point> points) {}
    private record PlacedSymbol(int id, SpellElement element, int slot, Stroke drawing) {}
    private record GuidePath(List<Point> vertices, boolean closed) {}
    private record Projection(Point point, double position, double distance) {}
    private record Spark(float x, float y, float vx, float vy, long born, int color) {}

    public RuneCanvasScreen(int tier, BlockPos tablePos) {
        this(tier, tier, tablePos);
    }
    public RuneCanvasScreen(int tier, int stylusTier, BlockPos tablePos) {
        super(Component.translatable("screen.manatech.rune_canvas"));
        this.tablePos = tablePos;
        this.tier = Math.max(1, Math.min(RuneTier.MAX, tier));
        this.layerCount = RuneTier.layers(this.tier);
        this.symbolCount = RuneTier.symbols(this.tier);
        this.slotsPerLayer = RuneTier.slots(stylusTier);
        this.symbolLimit = RuneTier.slots(stylusTier);
        this.visibleSlots = RuneTier.visibleSlots(stylusTier);
        for (int i = 0; i < layerCount; i++) layers.add(new Layer());
    }
    private int cx() { return width / 2; }
    private int cy() { return height / 2 + 35; }
    private int radius() { return Math.max(70, Math.min(190, (height - 150) / 2)); }
    private float guideRadius() { return radius() * (.62f + activeLayer * .025f); }
    private float inkRadius() { return guideRadius() * INK_RADIUS; }
    private int guideSides() { return switch (activeLayer % 7) {
        case 1 -> 5; case 2 -> 6; case 4 -> 7; case 5 -> 8; case 6 -> 10; default -> 0;
    }; }
    private float guideLimit(double angle, float r, int sides) {
        if (sides == 0) return r;
        double sector = 2 * Math.PI / sides;
        double offset = Math.IEEEremainder(angle + Math.PI / 2 - sector / 2, sector);
        return (float)(r * Math.cos(Math.PI / sides) / Math.cos(offset));
    }
    private Point clipped(double x, double y, float centerX, float centerY, float r, int sides) {
        double dx = x - centerX, dy = y - centerY, distance = Math.hypot(dx, dy);
        double limit = guideLimit(Math.atan2(dy, dx), r, sides);
        double scale = distance > limit ? limit / distance : 1;
        return new Point(centerX + (float)(dx * scale), centerY + (float)(dy * scale));
    }
    private GuidePath ring(float x, float y, float r) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            double a = i * 2 * Math.PI / 96;
            points.add(new Point(x + (float)Math.cos(a) * r, y + (float)Math.sin(a) * r));
        }
        return new GuidePath(points, true);
    }
    private GuidePath polygonPath(float x, float y, float r, int sides) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / sides;
            points.add(new Point(x + (float)Math.cos(a) * r, y + (float)Math.sin(a) * r));
        }
        return new GuidePath(points, true);
    }
    private GuidePath linePath(float x1, float y1, float x2, float y2) {
        return new GuidePath(List.of(new Point(x1, y1), new Point(x2, y2)), false);
    }
    private void addBoundary(List<GuidePath> paths, float x, float y, float r, int sides) {
        paths.add(sides == 0 ? ring(x, y, r) : polygonPath(x, y, r, sides));
    }
    private GuidePath guide(ElementSchemes.Path path) {
        return new GuidePath(path.points().stream().map(p -> new Point(cx()+p.x()*guideRadius(), cy()+p.y()*guideRadius())).toList(), path.closed());
    }
    private List<ElementSchemes.Path> schemePaths() { return ElementSchemes.paths(layers.get(activeLayer).element, activeLayer); }
    private List<GuidePath> canvasPaths() {
        List<GuidePath> paths = new ArrayList<>(schemePaths().stream().map(this::guide).toList());
        paths.addAll(slotRingPaths()); return paths;
    }
    private List<GuidePath> slotRingPaths() {
        List<GuidePath> paths = new ArrayList<>();
        for (int i : visibleSlots) {
            Point slot = slotPoint(i);
            paths.add(ring(slot.x(), slot.y(), symbolRadius()));
            paths.add(ring(slot.x(), slot.y(), symbolRadius() * .8f));
        }
        return paths;
    }
    private boolean matchesCircle(Stroke stroke, GuidePath circle) {
        List<Point> vertices = circle.vertices();
        Point a = vertices.get(0), opposite = vertices.get(vertices.size() / 2);
        double centerX = (a.x() + opposite.x()) / 2.0;
        double centerY = (a.y() + opposite.y()) / 2.0;
        double radius = Math.hypot(a.x() - centerX, a.y() - centerY);
        int checked = 0, matched = 0;
        List<Point> points = stroke.points();
        for (int i = 0; i < points.size(); i += Math.max(1, points.size() / 24)) {
            Point p = points.get(i);
            if (!Float.isFinite(p.x()) || !Float.isFinite(p.y())) return false;
            if (Math.abs(Math.hypot(p.x() - centerX, p.y() - centerY) - radius) <= 1.5) matched++;
            checked++;
        }
        return checked >= 2 && matched >= Math.ceil(checked * .9);
    }
    private List<GuidePath> symbolPaths(int slot, int id) {
        List<GuidePath> paths = new ArrayList<>();
        Point center = slotPoint(slot);
        float x = center.x(), y = center.y(), r = symbolRadius();
        paths.addAll(occultPaths(x, y, r * .78f, id));
        return paths;
    }
    private List<Point> stamp(int slot, int id) {
        List<Point> trace=new ArrayList<>();
        for(GuidePath path:symbolPaths(slot,id)) {
            if(!trace.isEmpty()) trace.add(new Point(Float.NaN,Float.NaN));
            trace.addAll(path.vertices());
            if(path.closed()) trace.add(path.vertices().get(0));
        }
        return trace;
    }
    private Stroke relocateOldSlotRing(Stroke stroke) {
        if(!isClosedStroke(stroke,25)) return stroke;
        for(int slot=0;slot<8;slot++) {
            double angle=-Math.PI/2+layers.get(activeLayer).element.ordinal()*Math.PI/24+activeLayer*Math.PI/48+slot*Math.PI/4;
            double oldR=(.43+((slot+activeLayer+layers.get(activeLayer).element.ordinal())%2)*.055)*guideRadius();
            float oldSize=Math.min(15,guideRadius()*.14f);
            for(int inner=0;inner<2;inner++) {
                GuidePath old=ring(cx()+(float)(Math.cos(angle)*oldR),cy()+(float)(Math.sin(angle)*oldR),oldSize-inner*3);
                if(matchesCircle(stroke,old)) {
                    Point center=slotPoint(slot);
                    var points=new ArrayList<>(ring(center.x(),center.y(),symbolRadius()*(inner==0?1:.8f)).vertices());
                    points.add(points.get(0)); return new Stroke(stroke.element(),points);
                }
            }
        }
        return stroke;
    }

    private List<GuidePath> occultPaths(float x,float y,float r,int id) {
        return com.vital.manatech.rune.SymbolGlyph.paths(x,y,r,id).stream()
            .map(path -> new GuidePath(path.vertices().stream().map(p -> new Point(p.x(),p.y())).toList(),path.closed())).toList();
    }
    private double pathLength(GuidePath path) {
        double length = 0;
        for (int i = 0; i < path.vertices().size() - (path.closed() ? 0 : 1); i++) {
            Point a = path.vertices().get(i), b = path.vertices().get((i+1) % path.vertices().size());
            length += Math.hypot(b.x()-a.x(), b.y()-a.y());
        }
        return length;
    }
    private Projection project(GuidePath path, double x, double y) {
        Projection best = null;
        double along = 0;
        List<Point> vertices = path.vertices();
        for (int i = 0; i < vertices.size() - (path.closed() ? 0 : 1); i++) {
            Point a = vertices.get(i), b = vertices.get((i+1) % vertices.size());
            double dx = b.x()-a.x(), dy = b.y()-a.y(), length = Math.hypot(dx,dy);
            if (length < .001) continue;
            double t = Math.max(0, Math.min(1, ((x-a.x())*dx+(y-a.y())*dy)/(length*length)));
            Point p = new Point(a.x()+(float)(t*dx),a.y()+(float)(t*dy));
            double distance = Math.hypot(x-p.x(),y-p.y());
            if (best == null || distance < best.distance()) best = new Projection(p,along+t*length,distance);
            along += length;
        }
        return best;
    }
    private Point pointAt(GuidePath path, double position) {
        double total = pathLength(path);
        double remaining = path.closed() ? position - Math.floor(position / total) * total
                : Math.max(0,Math.min(total,position));
        List<Point> vertices = path.vertices();
        for (int i = 0; i < vertices.size() - (path.closed() ? 0 : 1); i++) {
            Point a = vertices.get(i), b = vertices.get((i+1) % vertices.size());
            double length = Math.hypot(b.x()-a.x(),b.y()-a.y());
            if (remaining <= length) {
                float t = length < .001 ? 0 : (float)(remaining / length);
                return new Point(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t);
            }
            remaining -= length;
        }
        return vertices.get(vertices.size()-1);
    }
    private Point startPath(double x, double y, List<GuidePath> paths, double tolerance) {
        activePath = null;
        pathTravel = 0;
        strokeClosed = false;
        Projection best = null;
        for (GuidePath path : paths) {
            Projection candidate = project(path,x,y);
            if (candidate != null && candidate.distance() < tolerance && (best == null || candidate.distance() < best.distance())) {
                activePath = path;
                best = candidate;
            }
        }
        if (best != null) { pathPosition = best.position(); return best.point(); }
        return boundedPoint(x,y);
    }
    private void followPath(double x, double y) {
        if (strokeClosed) return;
        List<Point> points = currentStroke.points();
        if (activePath == null) {
            Point next = boundedPoint(x,y);
            if (points.size() < 2048 && Math.hypot(next.x()-points.get(points.size()-1).x(),next.y()-points.get(points.size()-1).y()) > 1) points.add(next);
            return;
        }
        Projection next = project(activePath,x,y);
        if (next == null) return;
        double target = next.position(), total = pathLength(activePath);
        if (activePath.closed()) {
            while (target-pathPosition > total/2) target -= total;
            while (target-pathPosition < -total/2) target += total;
        }
        double delta = target-pathPosition;
        if (activePath.closed() && Math.abs(pathTravel + delta) >= total - 3) {
            delta = Math.copySign(total, pathTravel + delta) - pathTravel;
            tracePath(delta);
            closeStroke();
            return;
        }
        tracePath(delta);
    }
    private void tracePath(double delta) {
        List<Point> points = currentStroke.points();
        int steps = Math.max(1,(int)Math.ceil(Math.abs(delta)/2));
        for (int i = 1; i <= steps && points.size() < 2048; i++) {
            Point p = pointAt(activePath,pathPosition+delta*i/steps);
            Point last = points.get(points.size()-1);
            if (Math.hypot(p.x()-last.x(),p.y()-last.y()) > .5) points.add(p);
        }
        pathPosition += delta;
        pathTravel += delta;
    }
    private void closeStroke() {
        List<Point> points = currentStroke.points();
        Point first = points.get(0);
        if (points.size() < 2048 && Math.hypot(first.x() - points.get(points.size()-1).x(),
                first.y() - points.get(points.size()-1).y()) > .001) points.add(first);
        strokeClosed = true;
    }
    private boolean inCanvas(double x, double y) {
        double dx = x - cx(), dy = y - cy();
        return dx * dx + dy * dy <= Math.pow(guideRadius() + GUIDE_REACH, 2);
    }
    private int tabWidth() { return Math.min(38, 270 / layerCount - 2); }
    private int tabStep() { return 270 / layerCount; }
    private int tabStart() { return cx() - tabStep() * layerCount / 2; }
    private int layerSymbol(int paletteIndex) { return paletteIndex; }

    @Override protected void init() {
        super.init();
        if (restored || Minecraft.getInstance().level == null) return;
        restored = true;
        var level = Minecraft.getInstance().level;
        var state = level.getBlockState(tablePos);
        if (!(state.getBlock() instanceof RuneTableBlock)
                || !(level.getBlockEntity(RuneTableBlock.firstPos(tablePos, state)) instanceof RuneTableBlockEntity table)) return;
        List<String> saved = table.canvasLayers();
        int previousLayer=activeLayer;
        for (int i = 0; i < Math.min(saved.size(), layerCount); i++) {
            activeLayer=i;
            DiagramLayer diagram = DiagramLayer.decode(saved.get(i));
            Layer layer = layers.get(i);
            int schemaElement = diagram.schemeElement();
            if (schemaElement < 0 && !diagram.strokes().isEmpty()) schemaElement = diagram.strokes().get(0).element();
            if (schemaElement >= 0 && schemaElement < SpellElement.values().length) layer.element = SpellElement.values()[schemaElement];
            for (DiagramLayer.Stroke stroke : diagram.strokes()) {
                if (stroke.element() >= 0 && stroke.element() < SpellElement.values().length)
                    layer.strokes.add(relocateOldSlotRing(new Stroke(SpellElement.values()[stroke.element()], restorePoints(stroke.points()))));
            }
            for (DiagramLayer.Symbol symbol : diagram.symbols()) {
                if (symbol.element() < 0 || symbol.element() >= SpellElement.values().length
                        || symbol.slot() < 0 || symbol.slot() >= DiagramLayer.MAX_SYMBOLS) continue;
                SpellElement element = SpellElement.values()[symbol.element()];
                layer.symbols.add(new PlacedSymbol(symbol.id(), element, symbol.slot(),
                        new Stroke(element, stamp(symbol.slot(),symbol.id()))));
            }
        }
        activeLayer=previousLayer;
        selectedElement=layers.get(activeLayer).element;
    }

    @Override public void resize(Minecraft minecraft, int newWidth, int newHeight) {
        int oldCenterX = cx(), oldCenterY = cy(), oldRadius = radius();
        super.resize(minecraft, newWidth, newHeight);
        if (oldCenterX == cx() && oldCenterY == cy() && oldRadius == radius()) return;
        for (Layer layer : layers) {
            for (Stroke stroke : layer.strokes) reproject(stroke.points(), oldCenterX, oldCenterY, oldRadius);
            for (PlacedSymbol symbol : layer.symbols)
                reproject(symbol.drawing().points(), oldCenterX, oldCenterY, oldRadius);
        }
        currentStroke = null;
        activePath = null;
        sparks.clear();
    }

    private void reproject(List<Point> points, int oldCenterX, int oldCenterY, int oldRadius) {
        float scale = (float) radius() / oldRadius;
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            if (Float.isFinite(point.x()) && Float.isFinite(point.y()))
                points.set(i, new Point(cx() + (point.x() - oldCenterX) * scale,
                        cy() + (point.y() - oldCenterY) * scale));
        }
    }

    private List<Point> restorePoints(List<DiagramLayer.Point> points) {
        List<Point> restoredPoints = new ArrayList<>();
        for (DiagramLayer.Point p : points) restoredPoints.add(p.x() == 1001 && p.y() == 1001
                ? new Point(Float.NaN, Float.NaN)
                : new Point(cx() + p.x() * radius() / 1000f, cy() + p.y() * radius() / 1000f));
        return restoredPoints;
    }

    private DiagramLayer diagram(Layer layer) {
        List<DiagramLayer.Stroke> ink = new ArrayList<>();
        for (Stroke stroke : layer.strokes) ink.add(new DiagramLayer.Stroke(stroke.element().ordinal(), normalise(stroke.points())));
        List<DiagramLayer.Symbol> marks = new ArrayList<>();
        for (PlacedSymbol symbol : layer.symbols) marks.add(new DiagramLayer.Symbol(symbol.id(), symbol.element().ordinal(),
                symbol.slot(), normalise(symbol.drawing().points())));
        return new DiagramLayer(ink, marks, layer.element.ordinal(), layers.indexOf(layer));
    }

    private void saveCanvas() {
        List<String> saved = new ArrayList<>();
        for (Layer layer : layers) {
            String encoded = diagram(layer).encode();
            if (encoded.length() > 32767) return;
            saved.add(encoded);
        }
        PacketDistributor.sendToServer(new SaveCanvasPayload(tablePos, saved));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, cx(), 8, 0xFFE8D7FF);
        for (int i = 0; i < layerCount; i++) {
            int left = tabStart() + i * tabStep();
            int width = tabWidth();
            g.fill(left, 25, left + width, 45, i == activeLayer ? 0xFF714C96 : 0xFF393241);
            g.drawCenteredString(font, "" + (i + 1), left + width / 2, 31, 0xFFFFFFFF);
        }
        int modeLeft = cx() - 75;
        g.fill(modeLeft, 50, modeLeft + 70, 70, symbolMode ? 0xFF393241 : 0xFF714C96);
        g.fill(cx() + 5, 50, cx() + 75, 70, symbolMode ? 0xFF714C96 : 0xFF393241);
        g.drawCenteredString(font, Component.translatable("screen.manatech.mode_draw"), modeLeft + 35, 56, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.manatech.mode_symbol"), cx() + 40, 56, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("screen.manatech.tier", tier, layerCount, symbolLimit), 8, 8, 0xFF694A2E);
        if (symbolMode) renderSymbolPalette(g); else renderElementPalette(g);

        renderCanvas(g);
        g.drawCenteredString(font, Component.translatable("screen.manatech.canvas_keys"), cx(), height - 64, 0xFF755182);
        g.drawCenteredString(font, Component.translatable("screen.manatech.export_layer"), cx(), height - 48, 0xFFD6B96E);
        if (inCanvas(mouseX, mouseY)) {
            int color = selectedElement.color();
            g.fill(mouseX - 3, mouseY - 3, mouseX + 4, mouseY + 4, color);
        }
        if (symbolMode) {
            int left = cx() - 132;
            if (mouseY >= 74 && mouseY < 93 && mouseX >= left && mouseX < left + symbolCount * 22) {
                int index = (mouseX - left) / 22;
                var modifier = com.vital.manatech.magic.MagicModifier.bySymbol(index);
                if (modifier != null) g.renderComponentTooltip(font, List.of(
                        Component.translatable("modifier.manatech." + modifier.id()),
                        Component.translatable("modifier.manatech." + modifier.id() + ".description")), mouseX, mouseY);
            }
        }
        String help = symbolMode ? "screen.manatech.help_symbol" : "screen.manatech.help_draw";
        g.drawCenteredString(font, Component.translatable(help), cx(), height - 27, 0xFFBEB3CB);
    }

    private void renderElementPalette(GuiGraphics g) {
        SpellElement[] all = SpellElement.values();
        for (int i = 0; i < all.length; i++) {
            int left = cx() - all.length * 19 + i * 38;
            g.fill(left, 75, left + 34, 95, all[i] == selectedElement ? all[i].color() : 0xFF393241);
            g.drawCenteredString(font, Component.translatable("element.manatech." + all[i].id()), left + 17, 80, all[i].color());
        }
    }

    private void renderSymbolPalette(GuiGraphics g) {
        for (int i = 0; i < symbolCount; i++) {
            int row = i / 12, col = i % 12;
            int left = cx() - 132 + col * 22;
            int top = 74 + row * 22;
            int symbol = layerSymbol(i);
            g.fill(left, top, left + 19, top + 19, symbol == selectedSymbol ? 0xFF714C96 : 0xFF393241);
        }
        g.flush();
        beginInk();
        for (int i = 0; i < symbolCount; i++) {
            int row = i / 12, col = i % 12;
            int left = cx() - 132 + col * 22;
            int top = 74 + row * 22;
            int symbol = layerSymbol(i);
            drawOccult(g.pose(), left + 9, top + 9, 7, symbol, 0xFFEEDCFF, 0.9f);
        }
        endInk();
    }

    private void renderCanvas(GuiGraphics g) {
        g.flush();
        beginInk();
        {
            if (showOtherLayers) {
                for (int i = 0; i < layers.size(); i++) {
                    if (i == activeLayer) continue;
                    for (Stroke stroke : layers.get(i).strokes) drawStroke(g, stroke, false);
                    for (PlacedSymbol symbol : layers.get(i).symbols) drawSymbolStroke(g, symbol.drawing(), false);
                }
            }
            boolean active = true;
            // Guides are a quiet underlay. Bright element-colored guides looked like duplicate ink.
            drawFrame(g.pose(), cx(), cy(), guideRadius(), 0xFF8D7252);
            for (Stroke stroke : layers.get(activeLayer).strokes) drawStroke(g, stroke, active);
            if (currentStroke != null) drawStroke(g, currentStroke, true);
            for (PlacedSymbol symbol : layers.get(activeLayer).symbols) {
                if(symbol.slot()!=DiagramLayer.CENTER_SLOT && !visibleSlots.contains(symbol.slot())) continue;
                Point slot = slotPoint(symbol.slot());
                if (symbol.slot() != DiagramLayer.CENTER_SLOT) drawSlot(g.pose(), slot.x(), slot.y(), symbolRadius(), symbol.element().color(), active ? .28f : .10f);
                drawSymbolStroke(g, symbol.drawing(), active);
            }
            if (symbolMode) {
                int preview = selectedSymbol < symbolCount ? selectedSymbol : layerSymbol(0);
                for (int i = 0; i < DiagramLayer.MAX_SYMBOLS; i++) {
                    if ((preview == 0) != (i == DiagramLayer.CENTER_SLOT)) continue;
                    if (i != DiagramLayer.CENTER_SLOT && !visibleSlots.contains(i)) continue;
                    if (i != DiagramLayer.CENTER_SLOT && layers.get(activeLayer).symbols.stream().filter(s -> s.slot() != DiagramLayer.CENTER_SLOT).count() >= symbolLimit) continue;
                    final int slotIndex = i;
                    boolean occupied = layers.get(activeLayer).symbols.stream().anyMatch(s -> s.slot() == slotIndex);
                    if (!occupied) drawGuide(g.pose(), slotPoint(i).x(), slotPoint(i).y(), symbolRadius(),
                            preview, selectedElement.color(), .30f);
                }
            }
        }
        if (symbolMode) {
            for (int i : visibleSlots) {
                Point slot = slotPoint(i);
                boolean occupied = false;
                for (PlacedSymbol placed : layers.get(activeLayer).symbols) {
                    if (placed.slot() == i) { occupied = true; break; }
                }
                drawSlot(g.pose(), slot.x(), slot.y(), symbolRadius(), occupied ? 0xFF8A6B45 : selectedElement.color(), occupied ? .25f : .55f);
            }
        } else if (inCanvas(lastMouseX, lastMouseY)) {
            int slot = nearestSlot(lastMouseX, lastMouseY);
            Point target = slotPoint(slot);
            if (slot != DiagramLayer.CENTER_SLOT) drawSlot(g.pose(), target.x(), target.y(), symbolRadius(), selectedElement.color(), .7f);
        }
        if (layers.get(activeLayer).symbols.stream().noneMatch(s -> s.slot() == DiagramLayer.CENTER_SLOT))
            drawGuide(g.pose(), cx(), cy(), symbolRadius(), 0, selectedElement.color(), .45f);
        endInk();
        renderSparks(g);
    }

    private void spark(Point point, int color) {
        if (!Float.isFinite(point.x()) || !Float.isFinite(point.y())) return;
        long now = System.nanoTime();
        for (int i = 0; i < 3; i++) {
            float angle = sparkRandom.nextFloat() * (float)(Math.PI * 2);
            float speed = 18 + sparkRandom.nextFloat() * 55;
            sparks.add(new Spark(point.x(), point.y(), (float)Math.cos(angle) * speed,
                    (float)Math.sin(angle) * speed, now, color));
        }
        if (sparks.size() > 96) sparks.subList(0, sparks.size() - 96).clear();
    }

    private void renderSparks(GuiGraphics g) {
        long now = System.nanoTime();
        sparks.removeIf(s -> now - s.born() > 350_000_000L);
        for (Spark spark : sparks) {
            float age = (now - spark.born()) / 1_000_000_000f;
            int alpha = Math.max(0, Math.min(255, (int)(255 * (1 - age / .35f))));
            int x = Math.round(spark.x() + spark.vx() * age);
            int y = Math.round(spark.y() + spark.vy() * age);
            g.fill(x, y, x + 2, y + 2, (alpha << 24) | HologramStyle.core(spark.color()));
        }
    }

    /** Builds exactly the same effect model that a world emitter will consume. */
    public CircleEffectMesh effectMesh() {
        List<DiagramLayer> diagrams = new ArrayList<>();
        for (Layer layer : layers) {
            List<DiagramLayer.Stroke> strokes = new ArrayList<>();
            for (Stroke stroke : layer.strokes) strokes.add(new DiagramLayer.Stroke(stroke.element().ordinal(), normalise(stroke.points())));
            List<DiagramLayer.Symbol> symbols = new ArrayList<>();
            for (PlacedSymbol symbol : layer.symbols) symbols.add(new DiagramLayer.Symbol(symbol.id(), symbol.element().ordinal(), symbol.slot(), normalise(symbol.drawing().points())));
            diagrams.add(new DiagramLayer(strokes, symbols));
        }
        return CircleEffectMesh.generate(diagrams);
    }

    private double lastMouseX, lastMouseY;
    private Point slotPoint(int slot) {
        var point = ElementSchemes.slot(layers.get(activeLayer).element, activeLayer, slot);
        return new Point(cx()+point.x()*guideRadius(), cy()+point.y()*guideRadius());
    }
    private int nearestSlot(double x, double y) {
        int best = DiagramLayer.CENTER_SLOT; double distance = Math.hypot(x-cx(), y-cy());
        for (int i : visibleSlots) {
            Point p = slotPoint(i); double d = Math.hypot(x - p.x(), y - p.y());
            if (d < distance) { distance = d; best = i; }
        }
        return best;
    }
    private void drawSlot(PoseStack p, float x, float y, float r, int color, float alpha) {
        circle(p, x, y, r, color, alpha);
        circle(p, x, y, r * .8f, color, alpha * .35f);
    }

    /** Structural ink follows the reference rings and polygonal frame, one layer at a time. */
    private void drawFrame(PoseStack p, float x, float y, float r, int color) {
        for (GuidePath path : canvasPaths()) {
            List<Point> points = path.vertices();
            for (int i=0; i<points.size()-(path.closed()?0:1); i++) {
                Point a=points.get(i), b=points.get((i+1)%points.size());
                beam(p,a.x(),a.y(),b.x(),b.y(),color,.23f);
            }
        }
    }
    private void polygon(PoseStack p,float x,float y,float r,int sides,int color,float alpha) {
        for (int i=0;i<sides;i++) {
            double a=-Math.PI/2+i*Math.PI*2/sides, b=-Math.PI/2+(i+1)*Math.PI*2/sides;
            beam(p,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,
                    x+(float)Math.cos(b)*r,y+(float)Math.sin(b)*r,color,alpha);
        }
    }

    private void drawGuide(PoseStack p, float x, float y, float r, int id, int color, float alpha) {
        drawOccult(p,x,y,r*.78f,id,color,alpha);
    }
    private void drawStroke(GuiGraphics g, Stroke stroke, boolean active) {
        List<Point> p = stroke.points();
        for (int i = 1; i < p.size(); i++) if (Float.isFinite(p.get(i-1).x()) && Float.isFinite(p.get(i).x()))
            beam(g.pose(), p.get(i - 1).x(), p.get(i - 1).y(), p.get(i).x(), p.get(i).y(), stroke.element().color(), active ? 1f : .32f);
    }
    private void drawSymbolStroke(GuiGraphics g, Stroke stroke, boolean active) {
        List<Point> points = stroke.points();
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1), b = points.get(i);
            if (Float.isFinite(a.x()) && Float.isFinite(a.y()) && Float.isFinite(b.x()) && Float.isFinite(b.y()))
                HologramStyle.fineLine(inkBuffer, g.pose(), a.x(), a.y(), b.x(), b.y(),
                        stroke.element().color(), active ? .9f : .32f);
        }
    }

    private void circle(PoseStack p, float x, float y, float r, int color, float a) {
        for (int i = 0; i < 96; i++) { double a1 = i * Math.PI * 2 / 96, a2 = (i + 1) * Math.PI * 2 / 96;
            beam(p, x + (float)Math.cos(a1)*r, y + (float)Math.sin(a1)*r, x + (float)Math.cos(a2)*r, y + (float)Math.sin(a2)*r, color, a); }
    }

    /** Each layer gets its own deterministic pack of procedural symbol previews. */
    private void drawOccult(PoseStack p, float x, float y, float r, int id, int color, float alpha) {
        boolean compact = r <= 12f;
        for (GuidePath path : occultPaths(x,y,r,id)) {
            List<Point> points = path.vertices();
            int step = compact && points.size() >= 48 ? (path.closed() ? 4 : 2) : 1;
            for (int i = 0; i < points.size() - (path.closed() ? 0 : 1); i += step) {
                int next = path.closed() ? (i + step) % points.size() : Math.min(i + step, points.size() - 1);
                Point a = points.get(i), b = points.get(next);
                if (compact) HologramStyle.fineLine(inkBuffer, p, a.x(), a.y(), b.x(), b.y(), color, alpha);
                else beam(p, a.x(), a.y(), b.x(), b.y(), color, alpha);
            }
        }
    }
    private void line(PoseStack p,float x1,float y1,float x2,float y2,int c,float a){ beam(p,x1,y1,x2,y2,c,a); }
    private void beam(PoseStack p,float x1,float y1,float x2,float y2,int color,float alpha) {
        HologramStyle.line(inkBuffer, p, x1, y1, x2, y2, color, alpha);
    }
    private void beginInk() {
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        inkBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }
    private void endInk() {
        BufferUploader.drawWithShader(inkBuffer.buildOrThrow());
        inkBuffer = null;
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }
    private void disc(PoseStack p,float x,float y,float r,int c,float a){ for(int i=0;i<12;i++){double a1=i*Math.PI*2/12,a2=(i+1)*Math.PI*2/12; beam(p,x,y,x+(float)Math.cos(a1)*r,y+(float)Math.sin(a1)*r,c,a); beam(p,x,y,x+(float)Math.cos(a2)*r,y+(float)Math.sin(a2)*r,c,a);}}

    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0 && y>=25&&y<45){for(int i=0;i<layerCount;i++){int l=tabStart()+i*tabStep();if(x>=l&&x<l+tabWidth()){activeLayer=i;selectedElement=layers.get(i).element;selectedSymbol=layerSymbol(0);return true;}}}
        if(button==0&&y>=50&&y<70){if(x>=cx()-75&&x<cx()-5){symbolMode=false;return true;}if(x>=cx()+5&&x<cx()+75){symbolMode=true;return true;}}
        if (button == 0 && y >= height - 58 && y < height - 35 && x >= cx() - 80 && x <= cx() + 80) { exportLayer(); return true; }
        if(button==0&&y>=74&&symbolMode&&x>=cx()-132&&x<cx()+132&&y<74+22*((symbolCount+11)/12)){int col=(int)((x-(cx()-132))/22),row=(int)((y-74)/22),index=row*12+col;if(index<symbolCount){selectedSymbol=layerSymbol(index);return true;}}
        if(button==0&&y>=75&&y<95&&!symbolMode&&x>=cx()-SpellElement.values().length*19&&x<cx()+SpellElement.values().length*19){int i=(int)((x-(cx()-SpellElement.values().length*19))/38);Layer layer=layers.get(activeLayer);
            if (!layer.strokes.isEmpty() || !layer.symbols.isEmpty()) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.manatech.scheme_in_use"), true);
                return true;
            }
            selectedElement=SpellElement.values()[i];layer.element=selectedElement;return true;}
        if(!inCanvas(x,y))return super.mouseClicked(x,y,button);
        if(symbolMode&&button==0){
            int slot=nearestSlot(x,y);
            if(Math.hypot(x-slotPoint(slot).x(),y-slotPoint(slot).y())>symbolRadius())return true;
            if(layers.get(activeLayer).symbols.stream().anyMatch(s->s.slot()==slot))return true;
            int id=selectedSymbol<symbolCount?selectedSymbol:layerSymbol(0);
            if ((id == 0) != (slot == DiagramLayer.CENTER_SLOT)) return true;
            if (slot != DiagramLayer.CENTER_SLOT && layers.get(activeLayer).symbols.stream().filter(s -> s.slot() != DiagramLayer.CENTER_SLOT).count() >= symbolLimit) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.manatech.symbol_limit", symbolLimit), true); return true;
            }
            // Keep one symbol per slot, with breaks between disconnected paths.
            Point center=slotPoint(slot);
            List<Point> trace=new ArrayList<>();
            for(GuidePath path: symbolPaths(slot,id)) {
                if(!trace.isEmpty()) trace.add(new Point(Float.NaN,Float.NaN));
                trace.addAll(path.vertices());
                if(path.closed()) trace.add(path.vertices().get(0));
            }
            layers.get(activeLayer).symbols.add(new PlacedSymbol(id,selectedElement,slot,
                    new Stroke(selectedElement,trace)));
            return true;
        }
        if(symbolMode&&button==1){removeSymbol(x,y);return true;}
        if(button==0){
            Point first = startPath(x, y, canvasPaths(), GUIDE_REACH);
            currentStroke = new Stroke(selectedElement, new ArrayList<>(List.of(first)));
            return true;
        }
        if(button==1){removeStroke(x,y);return true;} return true;
    }
    private void removeSymbol(double x,double y){List<PlacedSymbol>s=layers.get(activeLayer).symbols;for(int i=s.size()-1;i>=0;i--){Point p=slotPoint(s.get(i).slot());if(Math.hypot(p.x()-x,p.y()-y)<22){s.remove(i);return;}}}
    private void removeStroke(double x,double y){List<Stroke>s=layers.get(activeLayer).strokes;for(int i=s.size()-1;i>=0;i--){for(Point p:s.get(i).points())if(Math.hypot(p.x()-x,p.y()-y)<12){s.remove(i);return;}}}
    private static boolean isClosedStroke(Stroke stroke, double minArea) {
        List<Point> points = stroke.points();
        if (points.size() < 8) return false;
        Point first = points.get(0), last = points.get(points.size() - 1);
        if (Math.hypot(first.x() - last.x(), first.y() - last.y()) > 6) return false;
        double area = 0;
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1), b = points.get(i);
            area += a.x() * b.y() - b.x() * a.y();
        }
        return Math.abs(area) > minArea;
    }
    private void exportLayer() {
        DiagramLayer exported = diagram(layers.get(activeLayer));
        if (exported.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.manatech.layer_empty"), true);
        } else if (!exported.hasCentralCreation()) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.manatech.creation_required"), true);
        } else if (exported.encode().length() <= 32767) {
            saveCanvas();
            PacketDistributor.sendToServer(new ExportLayerPayload(tablePos, exported.encode()));
        } else {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.manatech.layer_too_large"), true);
        }
    }
    private List<DiagramLayer.Point> normalise(List<Point> points) {
        List<DiagramLayer.Point> result = new ArrayList<>();
        // Preserve each layer's size and position when independently exported pages are overlaid.
        for (Point p : points) result.add(Float.isFinite(p.x()) && Float.isFinite(p.y())
                ? new DiagramLayer.Point(Math.round((p.x()-cx())*1000/radius()), Math.round((p.y()-cy())*1000/radius()))
                : new DiagramLayer.Point(1001,1001));
        return result;
    }
    @Override public void mouseMoved(double x,double y){lastMouseX=x;lastMouseY=y;}
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && hasControlDown()) {
            Layer layer=layers.get(activeLayer); layer.strokes.clear(); layer.symbols.clear(); currentStroke=null; activePath=null; return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
            showOtherLayers = !showOtherLayers;
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_Z && hasControlDown()) {
            Layer layer = layers.get(activeLayer);
            if (symbolMode && !layer.symbols.isEmpty()) layer.symbols.remove(layer.symbols.size() - 1);
            else if (!layer.strokes.isEmpty()) layer.strokes.remove(layer.strokes.size() - 1);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        lastMouseX=x;lastMouseY=y;
        if(button==0&&currentStroke!=null){
            int before=currentStroke.points().size();
            followPath(x,y);
            if(currentStroke.points().size()>before)
                spark(currentStroke.points().get(currentStroke.points().size()-1),currentStroke.element().color());
            return true;
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    private Point boundedPoint(double x,double y){return clipped(x,y,cx(),cy(),guideRadius(),0);}
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&currentStroke!=null){
            followPath(x,y);
            if (activePath != null && activePath.closed() && !strokeClosed) {
                double total = pathLength(activePath);
                if (Math.abs(pathTravel) >= total * .85) {
                    tracePath(Math.copySign(total, pathTravel) - pathTravel);
                    closeStroke();
                }
            }
            if(activePath==null)smooth(currentStroke.points());
            if (strokeClosed && activePath != null) {
                GuidePath completed = activePath;
                // Replace older unfinished attempts on this exact guide instead of stacking them.
                layers.get(activeLayer).strokes.removeIf(old -> followsGuide(old, completed));
            }
            addNewInk(layers.get(activeLayer),currentStroke);
            currentStroke=null;activePath=null;
        }
        return super.mouseReleased(x,y,button);
    }

    /** Keep the entire gesture; segment-level de-duplication can erase visible parts of a contour. */
    private static void addNewInk(Layer layer, Stroke stroke){
        if (stroke.points().size() > 1 && layer.strokes.size() < 128) layer.strokes.add(stroke);
    }
    private boolean followsGuide(Stroke stroke, GuidePath guide) {
        int close = 0, checked = 0;
        List<Point> points = stroke.points();
        for (int i = 0; i < points.size(); i += Math.max(1, points.size() / 24)) {
            Point p = points.get(i);
            if (!Float.isFinite(p.x()) || !Float.isFinite(p.y())) return false;
            Projection projected = project(guide, p.x(), p.y());
            if (projected != null && projected.distance() <= 1.5) close++;
            checked++;
        }
        return checked >= 2 && close >= Math.ceil(checked * .9);
    }
    /** Light inking assistant: averages each point with its neighbours, removing shaky hand jitter. */
    private static void smooth(List<Point> points){if(points.size()<3)return;for(int pass=0;pass<2;pass++)for(int i=1;i<points.size()-1;i++){Point a=points.get(i-1),b=points.get(i),c=points.get(i+1);points.set(i,new Point((a.x()+b.x()*2+c.x())/4f,(a.y()+b.y()*2+c.y())/4f));}}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float p){
        g.fill(0,0,width,height,0xFF2A1B14);
        g.fill(18,18,width-18,height-18,0xFFE5C98F);
        g.fill(24,24,width-24,height-24,0xFFF0D9A7);
        for(int i=0;i<9;i++){int y0=35+i*27;g.fill(30,y0,width-30,y0+1,0x120B0804);}
    }
    @Override public boolean isPauseScreen(){return false;}

    @Override public void removed() {
        if (restored && Minecraft.getInstance().getConnection() != null) saveCanvas();
        super.removed();
    }
}
