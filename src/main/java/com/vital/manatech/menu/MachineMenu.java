package com.vital.manatech.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class MachineMenu extends AbstractContainerMenu {
    protected MachineMenu(MenuType<?> type, int id) { super(type, id); }
    protected void playerSlots(Inventory inv) {
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inv,col+row*9+9,8+col*18,154+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inv,col,8+col*18,212));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source=slot.getItem(), copy=source.copy();
        if(index<2) { if(!moveItemStackTo(source,2,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!moveItemStackTo(source,0,1,false)) {
            if(index<29) { if(!moveItemStackTo(source,29,38,false)) return ItemStack.EMPTY; }
            else if(!moveItemStackTo(source,2,29,false)) return ItemStack.EMPTY;
        }
        if(source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if(copy.getCount()==source.getCount()) return ItemStack.EMPTY;
        slot.onTake(player,source);
        return copy;
    }
}
