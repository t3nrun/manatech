package com.vital.manatech.component;

import com.mojang.serialization.Codec;
import com.vital.manatech.ManatechMod;
import com.vital.manatech.item.GlyphCell;
import com.vital.manatech.rune.hex.HexCoord;
import com.vital.manatech.rune.hex.RuneGlyph;
import com.vital.manatech.rune.schematic.RuneSchematic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ManatechMod.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> MANASTONE_CHARGE =
            COMPONENTS.register("manastone_charge", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    public static final Codec<List<GlyphCell>> CELLS_CODEC = GlyphCell.CODEC.listOf();
    public static final StreamCodec<ByteBuf, List<GlyphCell>> CELLS_STREAM =
            GlyphCell.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GlyphCell>>> SCHEMATIC =
            COMPONENTS.register("schematic", () -> DataComponentType.<List<GlyphCell>>builder()
                    .persistent(CELLS_CODEC)
                    .networkSynchronized(CELLS_STREAM)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LAYER_PAGE =
            COMPONENTS.register("layer_page", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final Codec<List<String>> PAGES_CODEC = Codec.STRING.listOf();
    public static final StreamCodec<ByteBuf, List<String>> PAGES_STREAM = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> GRIMOIRE =
            COMPONENTS.register("grimoire_pages", () -> DataComponentType.<List<String>>builder()
                    .persistent(PAGES_CODEC).networkSynchronized(PAGES_STREAM).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GRIMOIRE_SELECTION =
            COMPONENTS.register("grimoire_selection", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> SPELLS =
            COMPONENTS.register("assembled_spells", () -> DataComponentType.<List<String>>builder()
                    .persistent(PAGES_CODEC).networkSynchronized(PAGES_STREAM).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SPELL_SELECTION =
            COMPONENTS.register("spell_selection", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ModComponents() {
    }

    public static List<GlyphCell> from(RuneSchematic schematic) {
        return schematic.grid().cells().entrySet().stream()
                .map(entry -> new GlyphCell(entry.getKey().q(), entry.getKey().r(), entry.getValue()))
                .toList();
    }

    public static RuneSchematic toSchematic(List<GlyphCell> cells) {
        RuneSchematic schematic = new RuneSchematic();
        if (cells != null) {
            for (GlyphCell cell : cells) {
                schematic.imprint(new HexCoord(cell.q(), cell.r()), cell.glyph());
            }
        }
        return schematic;
    }

    public static boolean hasFocus(List<GlyphCell> cells) {
        return cells != null && cells.stream().anyMatch(cell -> cell.q() == 0 && cell.r() == 0 && cell.glyph() == RuneGlyph.FOCUS);
    }

    public static String encodeLayer(List<GlyphCell> cells) {
        return cells.stream().map(c -> c.q() + ":" + c.r() + ":" + c.glyph().getSerializedName()).collect(Collectors.joining(","));
    }

    public static List<GlyphCell> decodeLayer(String raw) {
        List<GlyphCell> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String cell : raw.split(",")) {
            String[] p = cell.split(":", 3);
            if (p.length == 3) result.add(new GlyphCell(Integer.parseInt(p[0]), Integer.parseInt(p[1]), RuneGlyph.byName(p[2])));
        }
        return result;
    }

    public static String mergeLayers(String first, String second) {
        List<GlyphCell> merged = new ArrayList<>(decodeLayer(first));
        merged.addAll(decodeLayer(second));
        return encodeLayer(merged);
    }
}
