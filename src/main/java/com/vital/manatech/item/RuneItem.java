package com.vital.manatech.item;

import com.vital.manatech.rune.hex.RuneGlyph;
import net.minecraft.world.item.Item;

public class RuneItem extends Item {
    private final RuneGlyph glyph;

    public RuneItem(RuneGlyph glyph, Properties properties) {
        super(properties);
        this.glyph = glyph;
    }

    public RuneGlyph glyph() {
        return glyph;
    }
}
