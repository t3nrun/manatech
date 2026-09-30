package com.vital.manatech.item;

import com.vital.manatech.block.RuneTableBlock;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import com.vital.manatech.rune.RuneTier;
import com.vital.manatech.client.ClientSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RuneStylusItem extends Item {
    private final int upgradeLevel;

    public RuneStylusItem(Properties properties) {
        this(properties, 1);
    }

    public RuneStylusItem(Properties properties, int upgradeLevel) {
        super(properties);
        this.upgradeLevel = upgradeLevel;
    }

    public int upgradeLevel() { return upgradeLevel; }

    @Override
    public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
        return upgradeLevel > 1;
    }

    /** The stylus only works on a rune table; it must not open the canvas in mid-air. */
    @Override
    public InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var state = level.getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof RuneTableBlock)
                || !(level.getBlockEntity(RuneTableBlock.firstPos(context.getClickedPos(), state)) instanceof RuneTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() == net.minecraft.core.Direction.DOWN) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            ClientSetup.openCanvas(upgradeLevel, ((RuneTableBlock) state.getBlock()).upgradeLevel(), context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
}
