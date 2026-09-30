package com.vital.manatech.block;

import com.mojang.serialization.MapCodec;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class RuneTableBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final MapCodec<RuneTableBlock> CODEC = simpleCodec(RuneTableBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 1, 16, 16, 16);
    private final int upgradeLevel;

    public RuneTableBlock(Properties properties) {
        this(properties, 1);
    }

    public RuneTableBlock(Properties properties, int upgradeLevel) {
        super(properties);
        this.upgradeLevel = upgradeLevel;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.FIRST));
    }

    public int upgradeLevel() { return upgradeLevel; }

    @Override
    public MapCodec<RuneTableBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.FIRST ? new RuneTableBlockEntity(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static BlockPos firstPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.FIRST ? pos : pos.relative(getSecondDirection(state.getValue(FACING)).getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().getItem() instanceof com.vital.manatech.item.RuneStylusItem) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(firstPos(pos, state)) instanceof RuneTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manatech.need_stylus"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        BlockPos second = pos.relative(getSecondDirection(facing));
        Level level = context.getLevel();
        if (!level.getBlockState(second).canBeReplaced(context)
                || !level.getWorldBorder().isWithinBounds(second)
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.getBlockState(second.below()).isFaceSturdy(level, second.below(), Direction.UP)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            level.setBlock(pos.relative(getSecondDirection(state.getValue(FACING))), state.setValue(PART, Part.SECOND), 3);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction other = state.getValue(PART) == Part.FIRST
                ? getSecondDirection(state.getValue(FACING)) : getSecondDirection(state.getValue(FACING)).getOpposite();
        if (direction == other && (!neighbor.is(this) || neighbor.getValue(FACING) != state.getValue(FACING)
                || neighbor.getValue(PART) == state.getValue(PART))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && state.getValue(PART) == Part.SECOND) {
            BlockState first = level.getBlockState(pos.relative(getSecondDirection(state.getValue(FACING)).getOpposite()));
            if (first.is(this) && first.getValue(PART) == Part.FIRST
                    && first.getValue(FACING) == state.getValue(FACING)) {
                popResource(level, pos, new ItemStack(this));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static Direction getSecondDirection(Direction facing) {
        return facing.getClockWise();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public enum Part implements StringRepresentable {
        FIRST, SECOND;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
