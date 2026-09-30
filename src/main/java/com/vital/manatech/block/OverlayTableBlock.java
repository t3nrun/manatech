package com.vital.manatech.block;

import com.mojang.serialization.MapCodec;
import com.vital.manatech.block.entity.OverlayTableBlockEntity;
import com.vital.manatech.item.RuneGrimoireItem;
import com.vital.manatech.item.RuneLayerPageItem;
import com.vital.manatech.item.ModItems;
import com.vital.manatech.component.ModComponents;
import com.vital.manatech.client.ClientSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class OverlayTableBlock extends BaseEntityBlock {
    public static final MapCodec<OverlayTableBlock> CODEC = simpleCodec(OverlayTableBlock::new);
    public OverlayTableBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new OverlayTableBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OverlayTableBlockEntity table)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof RuneLayerPageItem) {
            if (!level.isClientSide) {
                if (table.importPage(stack.getOrDefault(ModComponents.LAYER_PAGE, ""))) {
                    stack.shrink(1);
                    player.displayClientMessage(Component.translatable("message.manatech.page_imported"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.manatech.overlay_full"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof RuneGrimoireItem) {
            if (!level.isClientSide) RuneGrimoireItem.insertSpell(stack, table.layers(), player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OverlayTableBlockEntity table)) return InteractionResult.PASS;
        if (level.isClientSide) ClientSetup.openOverlay(table);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OverlayTableBlockEntity table) {
            for (String raw : table.takePages()) {
                ItemStack page = new ItemStack(ModItems.RUNE_LAYER_PAGE.get());
                page.set(ModComponents.LAYER_PAGE, raw);
                popResource(level, pos, page);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
