package com.vital.manatech.rune.hex;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HexGrid {
    public static final int MAX_RADIUS = 2;
    private static final Direction[] AXES = {
            Direction.NORTH, Direction.EAST, Direction.UP,
            Direction.SOUTH, Direction.WEST, Direction.DOWN
    };

    private final Map<HexCoord, RuneGlyph> cells = new HashMap<>();

    public boolean set(HexCoord coord, RuneGlyph glyph) {
        if (!inBounds(coord)) {
            return false;
        }
        if (glyph == null || glyph == RuneGlyph.BLANK) {
            cells.remove(coord);
        } else {
            cells.put(coord, glyph);
        }
        return true;
    }

    public RuneGlyph get(HexCoord coord) {
        return cells.getOrDefault(coord, RuneGlyph.BLANK);
    }

    public void clear() {
        cells.clear();
    }

    public int size() {
        return cells.size();
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public boolean inBounds(HexCoord coord) {
        return coord.distance(HexCoord.ORIGIN) <= MAX_RADIUS;
    }

    public Map<HexCoord, RuneGlyph> cells() {
        return cells;
    }

    public HexGrid copy() {
        HexGrid copy = new HexGrid();
        copy.cells.putAll(cells);
        return copy;
    }

    public HexGrid normalized() {
        if (cells.isEmpty()) {
            return new HexGrid();
        }
        int minQ = Integer.MAX_VALUE;
        int minR = Integer.MAX_VALUE;
        for (HexCoord coord : cells.keySet()) {
            if (coord.q() < minQ) {
                minQ = coord.q();
            }
            if (coord.r() < minR) {
                minR = coord.r();
            }
        }
        HexCoord shift = new HexCoord(-minQ, -minR);
        HexGrid shifted = new HexGrid();
        for (Map.Entry<HexCoord, RuneGlyph> entry : cells.entrySet()) {
            shifted.cells.put(entry.getKey().add(shift), entry.getValue());
        }
        return shifted;
    }

    public HexGrid rotated() {
        HexGrid rotated = new HexGrid();
        for (Map.Entry<HexCoord, RuneGlyph> entry : cells.entrySet()) {
            HexCoord coord = entry.getKey();
            rotated.cells.put(new HexCoord(-coord.r(), -coord.s()), entry.getValue());
        }
        return rotated;
    }

    public boolean matches(HexGrid other) {
        HexGrid left = normalized();
        HexGrid right = other.normalized();
        for (int turn = 0; turn < 6; turn++) {
            if (left.cells.equals(right.cells)) {
                return true;
            }
            right = right.rotated().normalized();
        }
        return false;
    }

    public List<HexCoord> disk(int radius) {
        List<HexCoord> coords = new ArrayList<>();
        for (int q = -radius; q <= radius; q++) {
            int rMin = Math.max(-radius, -q - radius);
            int rMax = Math.min(radius, -q + radius);
            for (int r = rMin; r <= rMax; r++) {
                coords.add(new HexCoord(q, r));
            }
        }
        return coords;
    }

    public static Direction axis(int index) {
        return AXES[Math.floorMod(index, AXES.length)];
    }
}
