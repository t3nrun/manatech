package com.vital.manatech.machine;

import com.vital.manatech.block.HorizontalMachineBlock;
import com.vital.manatech.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Four-by-four cube: brick edges, adamantite walls, four front inputs and four rear outputs. */
public record AdamantiteStructure(BlockPos origin, Direction facing, BlockPos error, String expected, int matched) {
    public boolean valid() { return error == null; }
    public BlockPos at(int x, int y, int z) {
        return origin.relative(facing.getClockWise(), x).relative(facing.getOpposite(), z).above(y);
    }
    public static char cell(int x, int y, int z) {
        return FurnacePattern.cell(x, y, z);
    }
    public static AdamantiteStructure check(Level level, BlockPos origin, Direction facing) {
        AdamantiteStructure layout = new AdamantiteStructure(origin.immutable(), facing, null, "", 0);
        var result = FurnacePattern.inspect((x, y, z, cell) -> {
            BlockPos pos = layout.at(x, y, z);
            boolean loaded = level.hasChunkAt(pos);
            BlockState state = loaded ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
            return loaded && switch (cell) {
                case 'B' -> state.is(Blocks.POLISHED_BLACKSTONE_BRICKS);
                case 'A' -> state.is(ModBlocks.ADAMANTITE_BLOCK);
                case 'F' -> state.is(ModBlocks.ADAMANTITE_FURNACE) && state.getValue(HorizontalMachineBlock.FACING) == facing;
                case 'D' -> state.is(ModBlocks.ADAMANTITE_DRAIN) && state.getValue(HorizontalMachineBlock.FACING) == facing.getOpposite();
                default -> state.isAir();
            };
        });
        BlockPos error = result.valid() ? null : layout.at(result.x(), result.y(), result.z());
        String expected = error == null ? "" : !level.hasChunkAt(error) ? "loaded" : switch (cell(result.x(), result.y(), result.z())) {
            case 'B' -> "brick"; case 'A' -> "adamantite"; case 'F' -> "furnace"; case 'D' -> "drain"; default -> "air";
        };
        return new AdamantiteStructure(origin.immutable(), facing, error, expected, result.matched());
    }
    /** Infer the corner from any of the four furnaces or drains. */
    public static AdamantiteStructure find(Level level, BlockPos part, BlockState state) {
        boolean drain = state.is(ModBlocks.ADAMANTITE_DRAIN);
        Direction facing = state.getValue(HorizontalMachineBlock.FACING);
        if (drain) facing = facing.getOpposite();
        AdamantiteStructure best = null;
        for (int x = 1; x <= 2; x++) for (int y = 1; y <= 2; y++) {
            BlockPos origin = part.relative(facing.getCounterClockWise(), x).below(y);
            if (drain) origin = origin.relative(facing, 3);
            AdamantiteStructure found = check(level, origin, facing);
            if (found.valid()) return found;
            if (best == null || found.matched() > best.matched()) best = found;
        }
        return best;
    }
    public BlockPos correspondingFurnace(BlockPos drain) { return drain.relative(facing, 3); }
}
