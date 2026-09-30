package com.vital.manatech.item;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.rune.schematic.RuneSchematic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RuneSchematicItem extends Item {
    public RuneSchematicItem(Properties properties) {
        super(properties);
    }

    public static boolean isSchematic(ItemStack stack) {
        return stack.getItem() instanceof RuneSchematicItem && stack.has(ModComponents.SCHEMATIC);
    }

    public static void write(ItemStack stack, RuneSchematic schematic) {
        stack.set(ModComponents.SCHEMATIC, ModComponents.from(schematic));
    }

    public static RuneSchematic read(ItemStack stack) {
        return ModComponents.toSchematic(stack.get(ModComponents.SCHEMATIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<GlyphCell> cells = stack.get(ModComponents.SCHEMATIC);
        tooltip.add(Component.literal((cells == null ? 0 : cells.size()) + " glyphs"));
    }
}
