package com.vital.manatech.item;

import com.mojang.serialization.Codec;
import com.vital.manatech.rune.hex.HexCoord;
import com.vital.manatech.rune.hex.RuneGlyph;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

public record GlyphCell(int q, int r, RuneGlyph glyph) {
    public static final Codec<GlyphCell> CODEC = Codec.STRING.xmap(GlyphCell::parse, GlyphCell::write);
    public static final StreamCodec<ByteBuf, GlyphCell> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(GlyphCell::parse, GlyphCell::write);

    public HexCoord coord() {
        return new HexCoord(q, r);
    }

    private static GlyphCell parse(String raw) {
        String[] parts = raw.split(":", 3);
        return new GlyphCell(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), RuneGlyph.byName(parts[2]));
    }

    private String write() {
        return q + ":" + r + ":" + glyph.getSerializedName();
    }
}
