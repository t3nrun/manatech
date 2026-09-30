package com.vital.manatech.rune.hex;

/** Base palette for rune canvas beams. Gameplay properties can be added later. */
public enum SpellElement {
    FIRE("fire", 0xFFFF1824),
    WATER("water", 0xFF168DCE),
    ENERGY("energy", 0xFF2AA66A),
    AIR("air", 0xFFAA7928),
    VOID("void", 0xFF6423B6),
    EARTH("earth", 0xFF947047);

    private final String id;
    private final int color;

    SpellElement(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }
}
