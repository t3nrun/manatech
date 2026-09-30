package com.vital.manatech.rune.schematic;

import com.vital.manatech.rune.hex.HexCoord;
import com.vital.manatech.rune.hex.RuneGlyph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpellCircle {
    private final List<RuneSchematic> schematics = new ArrayList<>();

    public boolean add(RuneSchematic schematic) {
        if (schematics.size() >= RuneSchematic.MAX_SLOTS || !schematic.isComplete()) {
            return false;
        }
        for (RuneSchematic existing : schematics) {
            if (existing.grid().matches(schematic.grid())) {
                return false;
            }
        }
        schematics.add(schematic);
        return true;
    }

    public boolean isFormed() {
        return !schematics.isEmpty();
    }

    public int size() {
        return schematics.size();
    }

    public List<RuneSchematic> schematics() {
        return Collections.unmodifiableList(schematics);
    }

    public int manaPerSecond() {
        int cost = 0;
        for (RuneSchematic schematic : schematics) {
            for (RuneGlyph glyph : schematic.grid().cells().values()) {
                cost += switch (glyph) {
                    case AMPLIFY -> 4;
                    case BIND -> 3;
                    case LINK, VENT -> 2;
                    case FOCUS -> 1;
                    case BLANK -> 0;
                };
            }
        }
        return cost;
    }

    public int radius() {
        int links = 0;
        for (RuneSchematic schematic : schematics) {
            for (RuneGlyph glyph : schematic.grid().cells().values()) {
                if (glyph == RuneGlyph.LINK) {
                    links++;
                }
            }
        }
        return Math.min(8, 2 + links);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (RuneSchematic schematic : schematics) {
            list.add(schematic.save());
        }
        tag.put("schematics", list);
        return tag;
    }

    public static SpellCircle load(CompoundTag tag) {
        SpellCircle circle = new SpellCircle();
        ListTag list = tag.getList("schematics", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < RuneSchematic.MAX_SLOTS; i++) {
            circle.schematics.add(RuneSchematic.fromTag(list.getCompound(i)));
        }
        return circle;
    }

    public boolean covers(HexCoord coord) {
        return coord.distance(HexCoord.ORIGIN) <= radius();
    }

    public void copyFrom(SpellCircle other) {
        schematics.clear();
        schematics.addAll(other.schematics);
    }
}
