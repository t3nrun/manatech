package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.RuneTableBlock;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import com.vital.manatech.item.ModItems;
import com.vital.manatech.item.RuneLayerPageItem;
import com.vital.manatech.item.RuneStylusItem;
import com.vital.manatech.item.RuneGrimoireItem;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One export request per clicked layer. A page is created by the server only. */
public record ExportLayerPayload(BlockPos table, String encoded) implements CustomPacketPayload {
    public static final Type<ExportLayerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "export_layer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportLayerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> { buf.writeBlockPos(payload.table); buf.writeUtf(payload.encoded, 32767); },
            buf -> new ExportLayerPayload(buf.readBlockPos(), buf.readUtf(32767)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(TYPE, STREAM_CODEC, ExportLayerPayload::handle);
    }

    private static void handle(ExportLayerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.distanceToSqr(payload.table.getX() + .5, payload.table.getY() + .5, payload.table.getZ() + .5) > 64) return;
            var state = player.level().getBlockState(payload.table);
            if (!(state.getBlock() instanceof RuneTableBlock block)
                    || !(player.level().getBlockEntity(RuneTableBlock.firstPos(payload.table, state)) instanceof RuneTableBlockEntity)) return;
            var stylus = player.getMainHandItem().getItem() instanceof RuneStylusItem main ? main
                    : player.getOffhandItem().getItem() instanceof RuneStylusItem off ? off : null;
            if (stylus == null) return;
            DiagramLayer layer = DiagramLayer.decode(payload.encoded);
            int tier = com.vital.manatech.rune.RuneTier.effectiveTier(stylus.upgradeLevel(), block.upgradeLevel());
            if (layer.schemeLayer() >= 0 && !com.vital.manatech.rune.RuneTier.allowsScheme(tier, layer.schemeLayer())) return;
            if (!com.vital.manatech.rune.RuneTier.allowsSymbols(stylus.upgradeLevel(), layer)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manatech.symbol_limit", com.vital.manatech.rune.RuneTier.slots(stylus.upgradeLevel())), true); return;
            }
            if (layer.isEmpty() || layer.strokes().size() > 128 || layer.symbols().size() > DiagramLayer.MAX_SYMBOLS) return;
            if (!layer.hasCentralCreation()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manatech.creation_required"), true);
                return;
            }
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            ItemStack book = main.getItem() instanceof RuneGrimoireItem ? main
                    : off.getItem() instanceof RuneGrimoireItem ? off : ItemStack.EMPTY;
            if (book.isEmpty()) {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() instanceof RuneGrimoireItem) { book = stack; break; }
                }
            }
            if (!book.isEmpty() && !RuneGrimoireItem.insertLayer(book, layer, player))
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manatech.grimoire_full"), true);
            ItemStack page = new ItemStack(ModItems.RUNE_LAYER_PAGE.get());
            RuneLayerPageItem.write(page, layer);
            if (!player.getInventory().add(page)) player.drop(page, false);
            if (book.isEmpty()) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manatech.layer_exported"), true);
        });
    }
}
