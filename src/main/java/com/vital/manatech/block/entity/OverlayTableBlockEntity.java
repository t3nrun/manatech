package com.vital.manatech.block.entity;

import com.vital.manatech.rune.AssembledSpell;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Stores imported pages separately from the freely arranged composition. */
public final class OverlayTableBlockEntity extends BlockEntity {
    public static final int MAX_PAGES = 32;
    public record Placement(int source, int x, int y, int scale, int rotation) {}

    private final List<String> pages = new ArrayList<>();
    private final List<Placement> placements = new ArrayList<>();

    public OverlayTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OVERLAY_TABLE.get(), pos, state);
    }

    public List<String> pages() { return List.copyOf(pages); }
    public List<Placement> placements() { return List.copyOf(placements); }
    public List<String> layers() { return assemble(pages, placements); }

    public static List<String> assemble(List<String> pages, List<Placement> placements) {
        List<String> result = new ArrayList<>();
        for (Placement placement : placements) {
            if (placement.source() < 0 || placement.source() >= pages.size()) continue;
            result.add(transform(DiagramLayer.decode(pages.get(placement.source())), placement).encode());
        }
        return List.copyOf(result);
    }

    public boolean importPage(String raw) {
        if (raw == null || raw.length() > 32767 || pages.size() >= MAX_PAGES
                || pages.stream().mapToInt(String::length).sum() + raw.length() > 524272
                || DiagramLayer.decode(raw).isEmpty() || pages.contains(raw)) return false;
        pages.add(raw);
        update();
        return true;
    }

    public boolean addPlacement(int source, int limit) {
        if (source < 0 || source >= pages.size() || placements.size() >= limit) return false;
        placements.add(new Placement(source, 0, 0, 100, 0));
        update();
        return true;
    }

    public boolean editPlacement(int index, int action, int first, int second) {
        if(action==6) {
            placements.sort(java.util.Comparator.comparingInt(p->DiagramLayer.decode(pages.get(p.source())).schemeLayer()));
            for(int i=0;i<placements.size();i++)placements.set(i,new Placement(placements.get(i).source(),0,0,100,0));
            update();return true;
        }
        if (index < 0 || index >= placements.size()) return false;
        Placement current = placements.get(index);
        switch (action) {
            case 1 -> placements.set(index, new Placement(current.source(), Math.clamp(first, -1000, 1000),
                    Math.clamp(second, -1000, 1000), current.scale(), current.rotation()));
            case 2 -> placements.set(index, new Placement(current.source(), current.x(), current.y(),
                    Math.clamp(first, 25, 200), current.rotation()));
            case 3 -> placements.set(index, new Placement(current.source(), current.x(), current.y(),
                    current.scale(), Math.floorMod(first, 360)));
            case 4 -> {
                int other = index + Integer.signum(first);
                if (other < 0 || other >= placements.size()) return false;
                placements.set(index, placements.get(other));
                placements.set(other, current);
            }
            case 5 -> placements.remove(index);
            default -> { return false; }
        }
        update();
        return true;
    }

    public List<String> takePages() {
        List<String> removed = List.copyOf(pages);
        pages.clear();
        placements.clear();
        update();
        return removed;
    }

    private static DiagramLayer transform(DiagramLayer original, Placement placement) {
        List<DiagramLayer.Stroke> strokes = new ArrayList<>();
        for (DiagramLayer.Stroke stroke : original.strokes())
            strokes.add(new DiagramLayer.Stroke(stroke.element(), transformPoints(stroke.points(), placement)));
        List<DiagramLayer.Symbol> symbols = new ArrayList<>();
        for (DiagramLayer.Symbol symbol : original.symbols())
            symbols.add(new DiagramLayer.Symbol(symbol.id(), symbol.element(), symbol.slot(),
                    transformPoints(symbol.trace(), placement)));
        return new DiagramLayer(strokes, symbols, original.schemeElement(), original.schemeLayer());
    }

    private static List<DiagramLayer.Point> transformPoints(List<DiagramLayer.Point> points, Placement placement) {
        double angle = Math.toRadians(placement.rotation());
        double sine = Math.sin(angle), cosine = Math.cos(angle);
        double scale = placement.scale() / 100.0;
        List<DiagramLayer.Point> result = new ArrayList<>(points.size());
        for (DiagramLayer.Point point : points) {
            if (point.x() == 1001 && point.y() == 1001) {
                result.add(point);
                continue;
            }
            int x = (int)Math.round((point.x() * cosine - point.y() * sine) * scale + placement.x());
            int y = (int)Math.round((point.x() * sine + point.y() * cosine) * scale + placement.y());
            result.add(new DiagramLayer.Point(Math.clamp(x, -1000, 1000), Math.clamp(y, -1000, 1000)));
        }
        return result;
    }

    private void update() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag savedPages = new ListTag();
        pages.forEach(page -> savedPages.add(StringTag.valueOf(page)));
        tag.put("pages", savedPages);
        ListTag savedPlacements = new ListTag();
        for (Placement placement : placements) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("source", placement.source());
            entry.putInt("x", placement.x());
            entry.putInt("y", placement.y());
            entry.putInt("scale", placement.scale());
            entry.putInt("rotation", placement.rotation());
            savedPlacements.add(entry);
        }
        tag.put("placements", savedPlacements);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pages.clear();
        placements.clear();
        boolean oldFormat = !tag.contains("pages");
        ListTag savedPages = tag.getList(oldFormat ? "layers" : "pages", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(savedPages.size(), MAX_PAGES); i++) {
            String raw = savedPages.getString(i);
            if (raw.length() <= 32767 && !DiagramLayer.decode(raw).isEmpty()) pages.add(raw);
        }
        if (oldFormat) {
            for (int i = 0; i < Math.min(pages.size(), AssembledSpell.MAX_LAYERS); i++)
                placements.add(new Placement(i, 0, 0, 100, 0));
        } else {
            ListTag savedPlacements = tag.getList("placements", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(savedPlacements.size(), AssembledSpell.MAX_LAYERS); i++) {
                CompoundTag entry = savedPlacements.getCompound(i);
                int source = entry.getInt("source");
                if (source < 0 || source >= pages.size()) continue;
                placements.add(new Placement(source, Math.clamp(entry.getInt("x"), -1000, 1000),
                        Math.clamp(entry.getInt("y"), -1000, 1000),
                        Math.clamp(entry.getInt("scale"), 25, 200), Math.floorMod(entry.getInt("rotation"), 360)));
            }
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
