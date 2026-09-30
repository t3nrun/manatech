package com.vital.manatech.rune.hex;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public final class HexCoord {
    public static final HexCoord ORIGIN = new HexCoord(0, 0);

    private final int q;
    private final int r;

    public HexCoord(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public int q() {
        return q;
    }

    public int r() {
        return r;
    }

    public int s() {
        return -q - r;
    }

    public HexCoord add(HexCoord other) {
        return new HexCoord(q + other.q, r + other.r);
    }

    public HexCoord neighbor(Direction direction) {
        return switch (direction) {
            case NORTH -> new HexCoord(q, r - 1);
            case SOUTH -> new HexCoord(q, r + 1);
            case EAST -> new HexCoord(q + 1, r);
            case WEST -> new HexCoord(q - 1, r);
            case UP -> new HexCoord(q + 1, r - 1);
            case DOWN -> new HexCoord(q - 1, r + 1);
            default -> this;
        };
    }

    public int distance(HexCoord other) {
        int dq = q - other.q;
        int dr = r - other.r;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("q", q);
        tag.putInt("r", r);
        return tag;
    }

    public static HexCoord load(CompoundTag tag) {
        return new HexCoord(tag.getInt("q"), tag.getInt("r"));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HexCoord other)) {
            return false;
        }
        return q == other.q && r == other.r;
    }

    @Override
    public int hashCode() {
        return q * 31 + r;
    }

    @Override
    public String toString() {
        return q + "," + r;
    }
}
