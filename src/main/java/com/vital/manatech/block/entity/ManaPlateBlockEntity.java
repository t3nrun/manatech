package com.vital.manatech.block.entity;

import com.vital.manatech.rune.AssembledSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary visualization plate; old rune fuel and casting were retired. */
public class ManaPlateBlockEntity extends BlockEntity {
    private String displaySpell="";
    public ManaPlateBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.MANA_PLATE.get(),pos,state); }
    public AssembledSpell displaySpell() { return AssembledSpell.read(displaySpell); }
    public void display(AssembledSpell spell) {
        displaySpell=spell.encode(); setChanged();
        if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,ManaPlateBlockEntity plate) {}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries); tag.putString("displaySpell",displaySpell); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.loadAdditional(tag,registries); displaySpell=tag.getString("displaySpell"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
