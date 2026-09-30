package com.vital.manatech.item;

import com.vital.manatech.component.ModComponents;
import com.vital.manatech.rune.DiagramLayer;
import com.vital.manatech.rune.AssembledSpell;
import com.vital.manatech.magic.PlayerMana;
import com.vital.manatech.client.ClientSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/** Use with a page in the other hand to insert it; sneak-use to overlay the selected layer. */
public final class RuneGrimoireItem extends Item implements GeoItem {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public RuneGrimoireItem(Properties properties) {
        super(properties);
        GeckoLibUtil.registerSyncedAnimatable(this);
    }

    /** Stores a freshly exported layer directly in this grimoire. Runs on the server. */
    public static boolean insertLayer(ItemStack book, DiagramLayer incoming, Player player) {
        if (incoming == null || incoming.isEmpty()) return false;
        List<String> pages = new ArrayList<>(book.getOrDefault(ModComponents.GRIMOIRE, List.of()));
        int selected = Math.max(0, Math.min(book.getOrDefault(ModComponents.GRIMOIRE_SELECTION, 0),
                Math.max(0, pages.size() - 1)));
        int limit = player.getAbilities().instabuild ? 16 : 12;
        if (pages.size() >= limit) return false;
        int insertAt = pages.isEmpty() ? 0 : selected + 1;
        pages.add(insertAt, incoming.encode());
        book.set(ModComponents.GRIMOIRE, pages);
        book.set(ModComponents.GRIMOIRE_SELECTION, insertAt);
        player.displayClientMessage(Component.translatable("message.manatech.layer_inserted", insertAt + 1), true);
        return true;
    }

    public static void insertSpell(ItemStack book, List<String> layers, Player player) {
        AssembledSpell spell = new AssembledSpell(layers);
        if (spell.isEmpty() || spell.layers().size() > (player.getAbilities().instabuild ? 16 : 12)
                || spell.encode().length() > 32767) {
            player.displayClientMessage(Component.translatable("message.manatech.overlay_empty"), true);
            return;
        }
        if (!spell.hasCentralCreation()) {
            player.displayClientMessage(Component.translatable("message.manatech.creation_required"), true); return;
        }
        List<String> spells = new ArrayList<>(book.getOrDefault(ModComponents.SPELLS, List.of()));
        if (spells.size() >= 32) {
            player.displayClientMessage(Component.translatable("message.manatech.grimoire_full"), true);
            return;
        }
        spells.add(spell.encode());
        book.set(ModComponents.SPELLS, spells);
        book.set(ModComponents.SPELL_SELECTION, spells.size() - 1);
        player.displayClientMessage(Component.translatable("message.manatech.spell_saved", spells.size()), true);
    }

    public static AssembledSpell selectedSpell(ItemStack book) {
        List<String> spells = book.getOrDefault(ModComponents.SPELLS, List.of());
        if (spells.isEmpty()) return new AssembledSpell(List.of());
        int index = Math.clamp(book.getOrDefault(ModComponents.SPELL_SELECTION, 0), 0, spells.size() - 1);
        return AssembledSpell.read(spells.get(index));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "grimoire", 0,
                state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.grimoire.idle"))));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    @Override
    public void createGeoRenderer(java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
            private final com.vital.manatech.client.GrimoireItemRenderer renderer = new com.vital.manatech.client.GrimoireItemRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() { return renderer; }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        ItemStack page = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide && !player.isShiftKeyDown() && !(page.getItem() instanceof RuneLayerPageItem)) {
            ClientSetup.openGrimoire(book);
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && !(page.getItem() instanceof RuneLayerPageItem)) {
                AssembledSpell spell = selectedSpell(book);
                if (spell.isEmpty()) player.displayClientMessage(Component.translatable("message.manatech.no_spell"), true);
                else PlayerMana.cast(player, spell);
                return InteractionResultHolder.sidedSuccess(book, false);
            }
            List<String> pages = new ArrayList<>(book.getOrDefault(ModComponents.GRIMOIRE, List.of()));
            int selected = Math.max(0, Math.min(book.getOrDefault(ModComponents.GRIMOIRE_SELECTION, 0), Math.max(0, pages.size() - 1)));
            if (page.getItem() instanceof RuneLayerPageItem) {
                DiagramLayer incoming = RuneLayerPageItem.read(page);
                if (incoming.isEmpty()) return InteractionResultHolder.fail(book);
                if (player.isShiftKeyDown() && !pages.isEmpty()) {
                    String merged = DiagramLayer.decode(pages.get(selected)).overlay(incoming).encode();
                    if (merged.length() > 32767) return InteractionResultHolder.fail(book);
                    pages.set(selected, merged);
                    player.displayClientMessage(Component.translatable("message.manatech.layer_overlaid", selected + 1), true);
                } else {
                    if (pages.size() >= (player.getAbilities().instabuild ? 16 : 12)) {
                        player.displayClientMessage(Component.translatable("message.manatech.grimoire_full"), true);
                        return InteractionResultHolder.fail(book);
                    }
                    int insertAt = pages.isEmpty() ? 0 : selected + 1;
                    pages.add(insertAt, incoming.encode());
                    selected = insertAt;
                    player.displayClientMessage(Component.translatable("message.manatech.layer_inserted", selected + 1), true);
                }
                book.set(ModComponents.GRIMOIRE, pages);
                book.set(ModComponents.GRIMOIRE_SELECTION, selected);
                // Keep pages reusable for assembling or editing future spells.
            } else if (pages.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.manatech.grimoire_empty"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(book, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<String> layers = stack.getOrDefault(ModComponents.GRIMOIRE, List.of());
        tooltip.add(Component.translatable("item.manatech.rune_grimoire.details", layers.size(),
                layers.isEmpty() ? 0 : Math.min(layers.size(), stack.getOrDefault(ModComponents.GRIMOIRE_SELECTION, 0) + 1)));
        tooltip.add(Component.translatable("item.manatech.rune_grimoire.usage"));
        tooltip.add(Component.translatable("message.manatech.spell_count", stack.getOrDefault(ModComponents.SPELLS, List.of()).size()));
    }
}
