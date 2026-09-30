package com.vital.manatech.rune.schematic;

import com.vital.manatech.rune.hex.HexCoord;
import com.vital.manatech.rune.hex.HexGrid;
import com.vital.manatech.rune.hex.RuneGlyph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayDeque;

public final class RuneSchematic {
    public static final int MAX_SLOTS = 6;

    private final HexGrid grid = new HexGrid();

    public HexGrid grid() {
        return grid;
    }

    public boolean imprint(HexCoord coord, RuneGlyph glyph) {
        return grid.set(coord, glyph);
    }

    public boolean isComplete() {
        if (grid.size() == 0) {
            return false;
        }
        Set<HexCoord> visited = new HashSet<>();
        var pending = new ArrayDeque<HexCoord>();
        HexCoord start = grid.cells().keySet().iterator().next();
        pending.add(start);
        visited.add(start);
        int[][] steps = {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}};
        while (!pending.isEmpty()) {
            HexCoord current = pending.removeFirst();
            for (int[] step : steps) {
                HexCoord neighbor = new HexCoord(current.q() + step[0], current.r() + step[1]);
                if (grid.get(neighbor) != RuneGlyph.BLANK && visited.add(neighbor)) {
                    pending.addLast(neighbor);
                }
            }
        }
        return visited.size() == grid.size();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag cells = new ListTag();
        grid.cells().forEach((coord, glyph) -> {
            CompoundTag cell = coord.save();
            cell.putString("glyph", glyph.getSerializedName());
            cells.add(cell);
        });
        tag.put("cells", cells);
        return tag;
    }

    public void load(CompoundTag tag) {
        grid.clear();
        ListTag cells = tag.getList("cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < cells.size(); i++) {
            CompoundTag cell = cells.getCompound(i);
            grid.set(HexCoord.load(cell), RuneGlyph.byName(cell.getString("glyph")));
        }
    }

    public static RuneSchematic fromTag(CompoundTag tag) {
        RuneSchematic schematic = new RuneSchematic();
        schematic.load(tag);
        return schematic;
    }
}
