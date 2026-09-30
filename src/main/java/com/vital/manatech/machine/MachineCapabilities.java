package com.vital.manatech.machine;

import com.vital.manatech.block.ModBlocks;
import com.vital.manatech.block.entity.ModBlockEntities;
import com.vital.manatech.block.entity.AdamantiteFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

public final class MachineCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ADAMANTITE_FURNACE.get(), (f, side) -> f.input());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> new IItemHandler() {
            private IItemHandler resolve() {
                var current = level.getBlockState(pos);
                if (!current.is(ModBlocks.ADAMANTITE_DRAIN)) return null;
                var structure = AdamantiteStructure.find(level, pos, current);
                if (!structure.valid()) return null;
                return level.getBlockEntity(structure.correspondingFurnace(pos)) instanceof AdamantiteFurnaceBlockEntity f ? f.output() : null;
            }
            private void check(int slot) { if (slot != 0) throw new IndexOutOfBoundsException("Slot " + slot); }
            public int getSlots() { return 1; }
            public ItemStack getStackInSlot(int slot) { check(slot); var h = resolve(); return h == null ? ItemStack.EMPTY : h.getStackInSlot(0); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { check(slot); return stack; }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { check(slot); var h = resolve(); return h == null ? ItemStack.EMPTY : h.extractItem(0, amount, simulate); }
            public int getSlotLimit(int slot) { check(slot); return 64; }
            public boolean isItemValid(int slot, ItemStack stack) { check(slot); return false; }
        }, ModBlocks.ADAMANTITE_DRAIN.get());
    }
}
