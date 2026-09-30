package com.vital.manatech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shape matches the tabletop and four legs of the Blockbench model. */
public final class ProcessingTableBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 13, 0, 16, 16, 16),
            box(1, 0, 1, 3, 13, 3), box(13, 0, 1, 15, 13, 3),
            box(1, 0, 13, 3, 13, 15), box(13, 0, 13, 15, 13, 15));
    public ProcessingTableBlock(Properties properties) { super(properties); }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new com.vital.manatech.menu.ProcessingMenu(id, inv, pos), net.minecraft.network.chat.Component.translatable("screen.manatech.processing")), pos);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        useWithoutItem(state,level,pos,player,hit);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
