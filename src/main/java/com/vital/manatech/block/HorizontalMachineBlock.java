package com.vital.manatech.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import com.mojang.serialization.MapCodec;
import com.vital.manatech.block.entity.AdamantiteFurnaceBlockEntity;
import com.vital.manatech.block.entity.ModBlockEntities;
import com.vital.manatech.machine.AdamantiteStructure;
import com.vital.manatech.item.RuneGrimoireItem;
import com.vital.manatech.magic.PlayerMana;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Multiblock furnace inputs and paired rear drains. */
public final class HorizontalMachineBlock extends BaseEntityBlock {
    public static final MapCodec<HorizontalMachineBlock> CODEC = simpleCodec(HorizontalMachineBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public HorizontalMachineBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.is(ModBlocks.ADAMANTITE_FURNACE) ? new AdamantiteFurnaceBlockEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ADAMANTITE_FURNACE.get(), AdamantiteFurnaceBlockEntity::tick);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private AdamantiteFurnaceBlockEntity furnace(Level level, BlockPos pos, BlockState state, AdamantiteStructure structure) {
        BlockPos input = state.is(ModBlocks.ADAMANTITE_DRAIN) ? structure.correspondingFurnace(pos) : pos;
        return level.getBlockEntity(input) instanceof AdamantiteFurnaceBlockEntity found ? found : null;
    }
    private boolean report(Level level, BlockPos pos, BlockState state, Player player, AdamantiteStructure structure) {
        if (structure.valid()) return true;
        BlockPos error = structure.error();
        player.displayClientMessage(Component.translatable("message.manatech.furnace_error", error.getX(), error.getY(), error.getZ(),
                Component.translatable("structure.manatech." + structure.expected())), false);
        return false;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof RuneGrimoireItem) && !AdamantiteFurnaceBlockEntity.isOre(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        var structure = AdamantiteStructure.find(level, pos, state);
        if (!report(level, pos, state, player, structure)) return ItemInteractionResult.CONSUME;
        if (stack.getItem() instanceof RuneGrimoireItem) {
            var spell = RuneGrimoireItem.selectedSpell(stack);
            var assembly=com.vital.manatech.rune.SpellAssembly.inspect(spell);
            if(!assembly.valid()) {
                player.displayClientMessage(Component.translatable("message.manatech.assembly_"+assembly.issue(),assembly.layer(),assembly.required()),true);
                return ItemInteractionResult.CONSUME;
            }
            if (!spell.hasCentralCreation()) {
                player.displayClientMessage(Component.translatable("message.manatech.creation_required"), true); return ItemInteractionResult.CONSUME;
            }
            if (com.vital.manatech.magic.FireSpells.resolve(spell) != com.vital.manatech.magic.FireSpells.Form.PILLAR
                    || com.vital.manatech.magic.FireSpells.circle(spell) < 5) {
                player.displayClientMessage(Component.translatable("message.manatech.furnace_spell"), false);
                return ItemInteractionResult.CONSUME;
            }
            java.util.Set<Integer> symbols = new java.util.HashSet<>();
            int[] counts = new int[6];
            spell.diagrams().forEach(d -> { d.strokes().forEach(s -> counts[Math.floorMod(s.element(), 6)]++); d.symbols().forEach(s -> { if(s.element() == 0) symbols.add(s.id()); }); });
            boolean fire = counts[0] > 0;
            for (int i = 1; i < counts.length; i++) if (counts[i] >= counts[0]) fire = false;
            if (!player.getAbilities().instabuild && (PlayerMana.rank(player) < 5 || !fire || !symbols.containsAll(java.util.List.of(0, 2, 7)))) {
                player.displayClientMessage(Component.translatable("message.manatech.furnace_spell"), false);
                return ItemInteractionResult.CONSUME;
            }
            if (!PlayerMana.spend(player, AdamantiteFurnaceBlockEntity.HEAT_MANA)) {
                player.displayClientMessage(Component.translatable("message.manatech.no_mana", com.vital.manatech.util.CompactNumbers.format(AdamantiteFurnaceBlockEntity.HEAT_MANA)), true);
                return ItemInteractionResult.CONSUME;
            }
            for (int x = 1; x <= 2; x++) for (int y = 1; y <= 2; y++)
                if (level.getBlockEntity(structure.at(x, y, 0)) instanceof AdamantiteFurnaceBlockEntity f) { f.validate(); f.ignite(); }
            player.displayClientMessage(Component.translatable("message.manatech.furnace_heated"), true);
        } else if (state.is(ModBlocks.ADAMANTITE_FURNACE)) {
            var f = furnace(level, pos, state, structure);
            if (f != null) {
                ItemStack remainder = f.inventory.insertItem(0, stack.copy(), false);
                stack.setCount(remainder.getCount());
                player.displayClientMessage(Component.translatable("message.manatech.furnace_status", f.inventory.getStackInSlot(0).getCount(), f.heat() / 20, f.progress() * 100 / AdamantiteFurnaceBlockEntity.SMELT_TICKS), true);
            }
        }
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (state.is(ModBlocks.ADAMANTITE_FURNACE) && !player.isShiftKeyDown()) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new com.vital.manatech.menu.AdamantiteMenu(id, inv, pos), Component.translatable("screen.manatech.adamantite_furnace")), pos);
            return InteractionResult.CONSUME;
        }
        var structure = AdamantiteStructure.find(level, pos, state);
        if (!report(level, pos, state, player, structure)) return InteractionResult.CONSUME;
        var f = furnace(level, pos, state, structure);
        if (f == null) return InteractionResult.CONSUME;
        if (state.is(ModBlocks.ADAMANTITE_DRAIN) || player.isShiftKeyDown()) {
            ItemStack taken = f.inventory.extractItem(state.is(ModBlocks.ADAMANTITE_DRAIN) ? 1 : 0, 64, false);
            if (!taken.isEmpty() && !player.addItem(taken)) player.drop(taken, false);
        }
        player.displayClientMessage(Component.translatable("message.manatech.furnace_status", f.inventory.getStackInSlot(0).getCount(), f.heat() / 20, f.progress() * 100 / AdamantiteFurnaceBlockEntity.SMELT_TICKS), true);
        return InteractionResult.CONSUME;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof AdamantiteFurnaceBlockEntity f)
            for (int i = 0; i < 2; i++) popResource(level, pos, f.inventory.extractItem(i, 64, false));
        super.onRemove(state, level, pos, next, moving);
    }
}
