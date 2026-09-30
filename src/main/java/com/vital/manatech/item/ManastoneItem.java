package com.vital.manatech.item;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.magic.ManaProgression;
import com.vital.manatech.magic.PlayerMana;
import com.vital.manatech.util.CompactNumbers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ManastoneItem extends Item {
    private final int stage;
    public ManastoneItem(int stage, Properties properties) { super(properties); this.stage = stage; }
    public int stage() { return stage; }
    public long capacity() { return ManaProgression.power(stage); }
    public long charge(ItemStack stack) { return Math.clamp(stack.getOrDefault(ModComponents.MANASTONE_CHARGE, capacity()), 0, capacity()); }
    public void drain(ItemStack stack, long amount) {
        if (amount <= 0) return;
        long remaining = Math.max(0, charge(stack) - amount);
        if (remaining == 0) stack.shrink(1);
        else stack.set(ModComponents.MANASTONE_CHARGE, remaining);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            long absorbed = PlayerMana.absorb(player, charge(stack));
            if (absorbed == 0) player.displayClientMessage(Component.translatable("message.manatech.mana_full"), true);
            else {
                drain(stack, absorbed);
                player.displayClientMessage(Component.translatable("message.manatech.mana_absorbed", CompactNumbers.format(absorbed)), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.manatech.manastone.charge", CompactNumbers.format(charge(stack)), CompactNumbers.format(capacity())));
        tooltip.add(Component.translatable("item.manatech.manastone.absorb"));
    }
}
