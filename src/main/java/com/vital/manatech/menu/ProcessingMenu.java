package com.vital.manatech.menu;

import com.vital.manatech.block.ModBlocks;
import com.vital.manatech.item.ModItems;
import com.vital.manatech.machine.CuttingGame;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ProcessingMenu extends MachineMenu {
    private final BlockPos pos;
    private final Player owner;
    private final SimpleContainer items=new SimpleContainer(2);
    private final SimpleContainerData data=new SimpleContainerData(4);
    private int step,seed,status;
    private long lastCut=Long.MIN_VALUE/2;
    private boolean completing;
    public ProcessingMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenus.PROCESSING.get(),id); this.pos=pos; owner=inv.player;
        seed=owner.getRandom().nextInt(10000);
        items.addListener(container -> { if(!completing) { step=0; status=0; seed=owner.getRandom().nextInt(10000); } });
        addSlot(new Slot(items,0,22,58) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.RAW_MANASTONE); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(items,1,138,58) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        playerSlots(inv); addDataSlots(data);
    }
    public int step() { return data.get(0); }
    public int cursor() { return data.get(1); }
    public int target() { return data.get(2); }
    public int status() { return data.get(3); }
    @Override public void broadcastChanges() {
        if(!owner.level().isClientSide) {
            data.set(0,step); data.set(1,CuttingGame.cursor(owner.level().getGameTime()));
            data.set(2,CuttingGame.target(seed,step)); data.set(3,status);
        }
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(player!=owner || player.level().isClientSide || !stillValid(player) || !items.getItem(0).is(ModItems.RAW_MANASTONE) || !items.getItem(1).isEmpty()) return false;
        if(button!=0 || step>=CuttingGame.CUTS) return false;
        long now=player.level().getGameTime();
        if(now-lastCut<6) return false;
        lastCut=now;
        if(CuttingGame.hits(CuttingGame.cursor(now),CuttingGame.target(seed,step))) {
            step++; status=0;
            if(step==CuttingGame.CUTS) finish();
        } else { step=0; status=1; }
        broadcastChanges(); return true;
    }
    private void finish() {
        completing=true;
        items.removeItem(0,1); items.setItem(1,new ItemStack(ModItems.MANASTONE.get()));
        completing=false; status=3;
    }
    @Override public boolean stillValid(Player player) { return player==owner && player.level().getBlockState(pos).is(ModBlocks.MANASTONE_PROCESSING_TABLE) && player.distanceToSqr(pos.getCenter())<=64; }
    @Override public void removed(Player player) { super.removed(player); if(!player.level().isClientSide) clearContainer(player,items); }
}
