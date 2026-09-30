package com.vital.manatech.block.entity;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.item.GlyphCell;
import com.vital.manatech.item.ModItems;
import com.vital.manatech.rune.hex.HexCoord;
import com.vital.manatech.rune.hex.RuneGlyph;
import com.vital.manatech.rune.schematic.RuneSchematic;
import com.vital.manatech.rune.RuneTier;
import com.vital.manatech.block.RuneTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class RuneTableBlockEntity extends BlockEntity {
    private final RuneSchematic schematic = new RuneSchematic();
    private List<String> canvasLayers = List.of();

    public List<String> canvasLayers() { return canvasLayers; }

    public void saveCanvasLayers(List<String> pages) {
        int tier = getBlockState().getBlock() instanceof RuneTableBlock table ? table.upgradeLevel() : 1;
        int count = Math.min(pages.size(), RuneTier.layers(tier));
        List<String> merged = new ArrayList<>(canvasLayers);
        for (int i = 0; i < count; i++) {
            if (i < merged.size()) merged.set(i, pages.get(i));
            else merged.add(pages.get(i));
        }
        // A weaker stylus edits only accessible layers; higher drafts remain stored.
        canvasLayers = List.copyOf(merged);
        setChanged();
        sync();
    }

    public RuneTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_TABLE.get(), pos, state);
    }

    public RuneSchematic schematic() {
        return schematic;
    }

    public boolean imprint(HexCoord coord, RuneGlyph glyph) {
        if (!schematic.imprint(coord, glyph)) {
            return false;
        }
        setChanged();
        sync();
        return true;
    }

    public void clear() {
        schematic.grid().clear();
        setChanged();
        sync();
    }

    public ItemStack takeSchematic() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag cells = new ListTag();
        for (GlyphCell cell : ModComponents.from(schematic)) {
            cells.add(StringTag.valueOf(cell.q() + ":" + cell.r() + ":" + cell.glyph().getSerializedName()));
        }
        tag.put("cells", cells);
        ListTag savedLayers = new ListTag();
        for (String layer : canvasLayers) savedLayers.add(StringTag.valueOf(layer));
        tag.put("canvasLayers", savedLayers);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        schematic.grid().clear();
        List<GlyphCell> cells = new ArrayList<>();
        ListTag list = tag.getList("cells", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String[] parts = list.getString(i).split(":", 3);
            if (parts.length == 3) {
                cells.add(new GlyphCell(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), RuneGlyph.byName(parts[2])));
            }
        }
        for (GlyphCell cell : cells) {
            schematic.imprint(cell.coord(), cell.glyph());
        }
        ListTag savedLayers = tag.getList("canvasLayers", Tag.TAG_STRING);
        List<String> restored = new ArrayList<>();
        for (int i = 0; i < savedLayers.size(); i++) restored.add(savedLayers.getString(i));
        canvasLayers = List.copyOf(restored);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
