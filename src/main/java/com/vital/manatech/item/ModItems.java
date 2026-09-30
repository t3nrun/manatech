package com.vital.manatech.item;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ManatechMod.MOD_ID);

    public static final DeferredItem<BlockItem> MANA_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.MANA_PLATE);
    public static final DeferredItem<BlockItem> OVERLAY_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.OVERLAY_TABLE);
    public static final DeferredItem<BlockItem> RUNE_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE);
    public static final DeferredItem<BlockItem> RUNE_TABLE_2 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_2);
    public static final DeferredItem<BlockItem> RUNE_TABLE_3 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_3);
    public static final DeferredItem<BlockItem> RUNE_TABLE_4 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_4);
    public static final DeferredItem<BlockItem> RUNE_TABLE_5 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_5);
    public static final DeferredItem<BlockItem> RUNE_TABLE_6 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_6);
    public static final DeferredItem<BlockItem> RUNE_TABLE_7 = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_TABLE_7);

    public static final DeferredItem<Item> RUNE_STYLUS = ITEMS.register("rune_stylus",
            () -> new RuneStylusItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RUNE_STYLUS_2 = stylus("rune_stylus_2", 2);
    public static final DeferredItem<Item> RUNE_STYLUS_3 = stylus("rune_stylus_3", 3);
    public static final DeferredItem<Item> RUNE_STYLUS_4 = stylus("rune_stylus_4", 4);
    public static final DeferredItem<Item> RUNE_STYLUS_5 = stylus("rune_stylus_5", 5);
    public static final DeferredItem<Item> RUNE_STYLUS_6 = stylus("rune_stylus_6", 6);
    public static final DeferredItem<Item> RUNE_STYLUS_7 = stylus("rune_stylus_7", 7);
    public static final DeferredItem<Item> RUNE_LAYER_PAGE = ITEMS.register("rune_layer_page",
            () -> new RuneLayerPageItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RUNE_GRIMOIRE = ITEMS.register("rune_grimoire",
            () -> new RuneGrimoireItem(new Item.Properties().stacksTo(1)));


    private static DeferredItem<Item> stylus(String id, int level) {
        return ITEMS.register(id, () -> new RuneStylusItem(new Item.Properties().stacksTo(1), level));
    }

    public static final DeferredItem<Item> RAW_SILVER = ITEMS.registerSimpleItem("raw_silver");
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> RAW_ORICHALCUM = ITEMS.registerSimpleItem("raw_orichalcum");
    public static final DeferredItem<Item> ORICHALCUM_INGOT = ITEMS.registerSimpleItem("orichalcum_ingot");
    public static final DeferredItem<Item> RAW_ADAMANTITE = ITEMS.registerSimpleItem("raw_adamantite");
    public static final DeferredItem<Item> ADAMANTITE_INGOT = ITEMS.registerSimpleItem("adamantite_ingot");
    public static final DeferredItem<Item> RAW_MITHRIL = ITEMS.registerSimpleItem("raw_mithril");
    public static final DeferredItem<Item> MITHRIL_INGOT = ITEMS.registerSimpleItem("mithril_ingot");
    public static final DeferredItem<BlockItem> SILVER_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.SILVER_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_SILVER_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final DeferredItem<BlockItem> SILVER_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.SILVER_BLOCK);
    public static final DeferredItem<BlockItem> ORICHALCUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.ORICHALCUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ORICHALCUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ORICHALCUM_ORE);
    public static final DeferredItem<BlockItem> ORICHALCUM_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.ORICHALCUM_BLOCK);
    public static final DeferredItem<BlockItem> ADAMANTITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.ADAMANTITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ADAMANTITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ADAMANTITE_ORE);
    public static final DeferredItem<BlockItem> ADAMANTITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.ADAMANTITE_BLOCK);
    public static final DeferredItem<BlockItem> MITHRIL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.MITHRIL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MITHRIL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_MITHRIL_ORE);
    public static final DeferredItem<BlockItem> MITHRIL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.MITHRIL_BLOCK);
    public static final DeferredItem<BlockItem> DEEPSLATE_MANASTONE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_MANASTONE_ORE);
    public static final DeferredItem<BlockItem> MANASTONE_PROCESSING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.MANASTONE_PROCESSING_TABLE);
    public static final DeferredItem<BlockItem> ADAMANTITE_FURNACE = ITEMS.registerSimpleBlockItem(ModBlocks.ADAMANTITE_FURNACE);
    public static final DeferredItem<BlockItem> ADAMANTITE_DRAIN = ITEMS.registerSimpleBlockItem(ModBlocks.ADAMANTITE_DRAIN);
    public static final DeferredItem<Item> RAW_MANASTONE = ITEMS.register("raw_manastone", () -> new ManastoneItem(1, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MANASTONE = ITEMS.register("manastone", () -> new ManastoneItem(2, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> REPROCESSED_MANASTONE = ITEMS.register("reprocessed_manastone", () -> new ManastoneItem(3, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HIGH_GRADE_MANASTONE = ITEMS.register("high_grade_manastone", () -> new ManastoneItem(4, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DRAGON_MANASTONE = ITEMS.register("dragon_manastone", () -> new ManastoneItem(5, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> NEW_STAR = ITEMS.register("new_star", () -> new ManastoneItem(6, new Item.Properties().stacksTo(1)));

    private ModItems() {
    }


}
