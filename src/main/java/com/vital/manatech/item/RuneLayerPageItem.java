package com.vital.manatech.item;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A detachable single layer. It can be filed in a grimoire or overlaid on another page. */
public final class RuneLayerPageItem extends Item {
    public RuneLayerPageItem(Properties properties) { super(properties); }

    public static void write(ItemStack stack, DiagramLayer layer) {
        stack.set(ModComponents.LAYER_PAGE, layer.encode());
    }

    public static DiagramLayer read(ItemStack stack) {
        return DiagramLayer.decode(stack.get(ModComponents.LAYER_PAGE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DiagramLayer layer = read(stack);
        tooltip.add(Component.translatable("item.manatech.rune_layer_page.details", layer.strokes().size(), layer.symbols().size()));
    }
}
