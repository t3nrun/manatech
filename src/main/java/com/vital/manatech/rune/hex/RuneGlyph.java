package com.vital.manatech.rune.hex;

import net.minecraft.util.StringRepresentable;

public enum RuneGlyph implements StringRepresentable {
    BLANK("blank"),
    FOCUS("focus"),
    LINK("link"),
    AMPLIFY("amplify"),
    BIND("bind"),
    VENT("vent");

    private final String name;

    RuneGlyph(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static RuneGlyph byName(String name) {
        for (RuneGlyph glyph : values()) {
            if (glyph.name.equals(name)) {
                return glyph;
            }
        }
        return BLANK;
    }
}
