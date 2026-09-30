package com.vital.manatech.block.entity;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ManatechMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneTableBlockEntity>> RUNE_TABLE =
            BLOCK_ENTITIES.register("rune_table", () -> BlockEntityType.Builder
                    .of(RuneTableBlockEntity::new, ModBlocks.RUNE_TABLE.get(), ModBlocks.RUNE_TABLE_2.get(), ModBlocks.RUNE_TABLE_3.get(), ModBlocks.RUNE_TABLE_4.get(),
                            ModBlocks.RUNE_TABLE_5.get(), ModBlocks.RUNE_TABLE_6.get(), ModBlocks.RUNE_TABLE_7.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaPlateBlockEntity>> MANA_PLATE =
            BLOCK_ENTITIES.register("mana_plate", () -> BlockEntityType.Builder
                    .of(ManaPlateBlockEntity::new, ModBlocks.MANA_PLATE.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OverlayTableBlockEntity>> OVERLAY_TABLE =
            BLOCK_ENTITIES.register("overlay_table", () -> BlockEntityType.Builder
                    .of(OverlayTableBlockEntity::new, ModBlocks.OVERLAY_TABLE.get()).build(null));

    private ModBlockEntities() {
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdamantiteFurnaceBlockEntity>> ADAMANTITE_FURNACE =
            BLOCK_ENTITIES.register("adamantite_furnace", () -> BlockEntityType.Builder.of(AdamantiteFurnaceBlockEntity::new, ModBlocks.ADAMANTITE_FURNACE.get()).build(null));
}
