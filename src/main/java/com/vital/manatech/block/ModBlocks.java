package com.vital.manatech.block;

import com.vital.manatech.ManatechMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ManatechMod.MOD_ID);

    public static final DeferredBlock<Block> MANA_PLATE = BLOCKS.register("mana_plate",
            () -> new ManaPlateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ManaPlateBlock.FORMED) ? 10 : 0)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> RUNE_TABLE = BLOCKS.register("rune_table",
            () -> new RuneTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> RUNE_TABLE_2 = table("rune_table_2", 2);
    public static final DeferredBlock<Block> RUNE_TABLE_3 = table("rune_table_3", 3);
    public static final DeferredBlock<Block> RUNE_TABLE_4 = table("rune_table_4", 4);
    public static final DeferredBlock<Block> RUNE_TABLE_5 = table("rune_table_5", 5);
    public static final DeferredBlock<Block> RUNE_TABLE_6 = table("rune_table_6", 6);
    public static final DeferredBlock<Block> RUNE_TABLE_7 = table("rune_table_7", 7);
    public static final DeferredBlock<Block> OVERLAY_TABLE = BLOCKS.register("overlay_table",
            () -> new OverlayTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F).sound(SoundType.WOOD)));

    private static DeferredBlock<Block> table(String id, int level) {
        return BLOCKS.register(id, () -> new RuneTableBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD), level));
    }

    public static final DeferredBlock<Block> SILVER_ORE = material("silver_ore", 3.0F, false);
    public static final DeferredBlock<Block> DEEPSLATE_SILVER_ORE = material("deepslate_silver_ore", 4.5F, false);
    public static final DeferredBlock<Block> SILVER_BLOCK = material("silver_block", 5.0F, true);
    public static final DeferredBlock<Block> ORICHALCUM_ORE = material("orichalcum_ore", 3.0F, false);
    public static final DeferredBlock<Block> DEEPSLATE_ORICHALCUM_ORE = material("deepslate_orichalcum_ore", 4.5F, false);
    public static final DeferredBlock<Block> ORICHALCUM_BLOCK = material("orichalcum_block", 5.0F, true);
    public static final DeferredBlock<Block> ADAMANTITE_ORE = material("adamantite_ore", 3.0F, false);
    public static final DeferredBlock<Block> DEEPSLATE_ADAMANTITE_ORE = material("deepslate_adamantite_ore", 4.5F, false);
    public static final DeferredBlock<Block> ADAMANTITE_BLOCK = material("adamantite_block", 5.0F, true);
    public static final DeferredBlock<Block> MITHRIL_ORE = material("mithril_ore", 3.0F, false);
    public static final DeferredBlock<Block> DEEPSLATE_MITHRIL_ORE = material("deepslate_mithril_ore", 4.5F, false);
    public static final DeferredBlock<Block> MITHRIL_BLOCK = material("mithril_block", 5.0F, true);
    public static final DeferredBlock<Block> DEEPSLATE_MANASTONE_ORE = material("deepslate_manastone_ore", 4.5F, false);
    public static final DeferredBlock<Block> MANASTONE_PROCESSING_TABLE = BLOCKS.register("manastone_processing_table", () -> new ProcessingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Block> ADAMANTITE_FURNACE = BLOCKS.register("adamantite_furnace", () -> new HorizontalMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(8F, 30F).sound(SoundType.METAL).lightLevel(s -> s.getValue(HorizontalMachineBlock.LIT) ? 12 : 0).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> ADAMANTITE_DRAIN = BLOCKS.register("adamantite_drain", () -> new HorizontalMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(8F, 30F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    private static DeferredBlock<Block> material(String id, float hardness, boolean metal) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness, 6F).sound(metal ? SoundType.METAL : SoundType.STONE).requiresCorrectToolForDrops()));
    }

    private ModBlocks() {
    }
}
