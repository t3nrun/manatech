package com.vital.manatech;

import com.vital.manatech.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ManatechMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.manatech"))
            .icon(() -> new ItemStack(ModItems.RUNE_STYLUS.get()))
            .displayItems((params, output) -> {
                // Patchouli adds the guide through book.json's creative_tab field.
                output.accept(ModItems.SILVER_ORE.get());
                output.accept(ModItems.DEEPSLATE_SILVER_ORE.get());
                output.accept(ModItems.SILVER_BLOCK.get());
                output.accept(ModItems.ORICHALCUM_ORE.get());
                output.accept(ModItems.DEEPSLATE_ORICHALCUM_ORE.get());
                output.accept(ModItems.ORICHALCUM_BLOCK.get());
                output.accept(ModItems.ADAMANTITE_ORE.get());
                output.accept(ModItems.DEEPSLATE_ADAMANTITE_ORE.get());
                output.accept(ModItems.ADAMANTITE_BLOCK.get());
                output.accept(ModItems.MITHRIL_ORE.get());
                output.accept(ModItems.DEEPSLATE_MITHRIL_ORE.get());
                output.accept(ModItems.MITHRIL_BLOCK.get());
                output.accept(ModItems.DEEPSLATE_MANASTONE_ORE.get());
                output.accept(ModItems.MANASTONE_PROCESSING_TABLE.get());
                output.accept(ModItems.ADAMANTITE_FURNACE.get());
                output.accept(ModItems.ADAMANTITE_DRAIN.get());
                output.accept(ModItems.RAW_SILVER.get());
                output.accept(ModItems.SILVER_INGOT.get());
                output.accept(ModItems.RAW_ORICHALCUM.get());
                output.accept(ModItems.ORICHALCUM_INGOT.get());
                output.accept(ModItems.RAW_ADAMANTITE.get());
                output.accept(ModItems.ADAMANTITE_INGOT.get());
                output.accept(ModItems.RAW_MITHRIL.get());
                output.accept(ModItems.MITHRIL_INGOT.get());
                output.accept(ModItems.RAW_MANASTONE.get());
                output.accept(ModItems.MANASTONE.get());
                output.accept(ModItems.REPROCESSED_MANASTONE.get());
                output.accept(ModItems.HIGH_GRADE_MANASTONE.get());
                output.accept(ModItems.DRAGON_MANASTONE.get());
                output.accept(ModItems.NEW_STAR.get());
                output.accept(ModItems.MANA_PLATE.get());
                output.accept(ModItems.OVERLAY_TABLE.get());
                output.accept(ModItems.RUNE_TABLE.get());
                output.accept(ModItems.RUNE_TABLE_2.get());
                output.accept(ModItems.RUNE_TABLE_3.get());
                output.accept(ModItems.RUNE_TABLE_4.get());
                output.accept(ModItems.RUNE_TABLE_5.get());
                output.accept(ModItems.RUNE_TABLE_6.get());
                output.accept(ModItems.RUNE_TABLE_7.get());
                output.accept(ModItems.RUNE_STYLUS.get());
                output.accept(ModItems.RUNE_STYLUS_2.get());
                output.accept(ModItems.RUNE_STYLUS_3.get());
                output.accept(ModItems.RUNE_STYLUS_4.get());
                output.accept(ModItems.RUNE_STYLUS_5.get());
                output.accept(ModItems.RUNE_STYLUS_6.get());
                output.accept(ModItems.RUNE_STYLUS_7.get());
                output.accept(ModItems.RUNE_LAYER_PAGE.get());
                output.accept(ModItems.RUNE_GRIMOIRE.get());
            })
            .build());

    private ModTabs() {
    }
}
