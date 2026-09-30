package com.vital.manatech.block.entity;

import com.vital.manatech.block.HorizontalMachineBlock;
import com.vital.manatech.item.ModItems;
import com.vital.manatech.machine.AdamantiteStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class AdamantiteFurnaceBlockEntity extends BlockEntity {
    public static final int SMELT_TICKS = 200;
    public static final int HEAT_TICKS = 1200;
    public static final long HEAT_MANA = 16384;
    private int heat, progress;
    private boolean formed;
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && isOre(stack); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final IItemHandler input = new IItemHandler() {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { checkSlot(slot); return inventory.getStackInSlot(0); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { checkSlot(slot); return validate() ? inventory.insertItem(0, stack, simulate) : stack; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { checkSlot(slot); return ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { checkSlot(slot); return 64; }
        public boolean isItemValid(int slot, ItemStack stack) { checkSlot(slot); return isOre(stack); }
    };
    private final IItemHandler output = new IItemHandler() {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { checkSlot(slot); return inventory.getStackInSlot(1); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { checkSlot(slot); return stack; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { checkSlot(slot); return validate() ? inventory.extractItem(1, amount, simulate) : ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { checkSlot(slot); return 64; }
        public boolean isItemValid(int slot, ItemStack stack) { checkSlot(slot); return false; }
    };
    private static void checkSlot(int slot) { if (slot != 0) throw new IndexOutOfBoundsException("Slot " + slot); }
    public AdamantiteFurnaceBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ADAMANTITE_FURNACE.get(), pos, state); }
    public static boolean isOre(ItemStack stack) { return stack.is(ModItems.RAW_MITHRIL) || stack.is(ModItems.MITHRIL_ORE) || stack.is(ModItems.DEEPSLATE_MITHRIL_ORE); }
    public IItemHandler input() { return input; }
    public IItemHandler output() { return output; }
    public int heat() { return heat; }
    public int progress() { return progress; }
    public void ignite() { heat = HEAT_TICKS; setChanged(); }
    public boolean validate() {
        formed = level != null && AdamantiteStructure.find(level, worldPosition, getBlockState()).valid();
        return formed;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, AdamantiteFurnaceBlockEntity furnace) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 == 0) furnace.validate();
        boolean lit = furnace.formed && furnace.heat > 0;
        if (state.getValue(HorizontalMachineBlock.LIT) != lit) level.setBlock(pos, state.setValue(HorizontalMachineBlock.LIT, lit), 3);
        boolean changed = furnace.heat > 0 || furnace.progress > 0;
        if (furnace.heat > 0) furnace.heat--;
        ItemStack ore = furnace.inventory.getStackInSlot(0), out = furnace.inventory.getStackInSlot(1);
        boolean room = out.isEmpty() || (out.is(ModItems.MITHRIL_INGOT) && out.getCount() < out.getMaxStackSize());
        if (lit && isOre(ore) && room) {
            furnace.progress++;
            if (furnace.progress >= SMELT_TICKS) {
                if (furnace.validate()) {
                    furnace.inventory.extractItem(0, 1, false);
                    if (out.isEmpty()) furnace.inventory.setStackInSlot(1, new ItemStack(ModItems.MITHRIL_INGOT.get()));
                    else { out.grow(1); furnace.setChanged(); }
                }
                furnace.progress = 0;
            }
        } else if (!furnace.formed || !isOre(ore)) furnace.progress = 0;
        if (changed) furnace.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("heat", heat); tag.putInt("progress", progress);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if (inventory.getSlots() != 2) inventory.setSize(2);
        heat = Math.clamp(tag.getInt("heat"), 0, HEAT_TICKS);
        progress = Math.clamp(tag.getInt("progress"), 0, SMELT_TICKS - 1);
        formed = false;
    }
}
