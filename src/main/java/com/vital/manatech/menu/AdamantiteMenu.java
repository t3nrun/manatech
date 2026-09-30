package com.vital.manatech.menu;

import com.vital.manatech.block.entity.AdamantiteFurnaceBlockEntity;
import com.vital.manatech.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class AdamantiteMenu extends MachineMenu {
    private final BlockPos pos;
    private final AdamantiteFurnaceBlockEntity furnace;
    private final ContainerData data;
    public AdamantiteMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenus.FURNACE.get(),id); this.pos=pos;
        furnace=inv.player.level().getBlockEntity(pos) instanceof AdamantiteFurnaceBlockEntity f ? f : null;
        var items=furnace==null ? new ItemStackHandler(2) : furnace.inventory;
        addSlot(new SlotItemHandler(items,0,44,58) { @Override public boolean mayPlace(ItemStack stack) { return AdamantiteFurnaceBlockEntity.isOre(stack); } });
        addSlot(new SlotItemHandler(items,1,116,58) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        data=new ContainerData() {
            private final int[] values=new int[3];
            public int get(int index) { if(furnace==null || inv.player.level().isClientSide) return values[index]; return switch(index) { case 0 -> furnace.heat(); case 1 -> furnace.progress(); default -> furnace.validate()?1:0; }; }
            public void set(int index,int value) { values[index]=value; }
            public int getCount() { return 3; }
        };
        playerSlots(inv); addDataSlots(data);
    }
    public int heat() { return data.get(0); }
    public int progress() { return data.get(1); }
    public boolean formed() { return data.get(2)!=0; }
    @Override public boolean stillValid(Player player) { return player.level().getBlockState(pos).is(ModBlocks.ADAMANTITE_FURNACE) && player.distanceToSqr(pos.getCenter())<=64 && (player.level().isClientSide || player.level().getBlockEntity(pos)==furnace); }
}
