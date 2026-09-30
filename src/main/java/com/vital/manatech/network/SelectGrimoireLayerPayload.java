package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.component.ModComponents;
import com.vital.manatech.item.RuneGrimoireItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Persists the page selected in the client viewer back to the server-owned stack. */
public record SelectGrimoireLayerPayload(int selected, boolean spell) implements CustomPacketPayload {
    public static final Type<SelectGrimoireLayerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "select_grimoire_layer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectGrimoireLayerPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> { buf.writeVarInt(payload.selected); buf.writeBoolean(payload.spell); },
                    buf -> new SelectGrimoireLayerPayload(buf.readVarInt(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(TYPE, STREAM_CODEC, SelectGrimoireLayerPayload::handle);
    }

    private static void handle(SelectGrimoireLayerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            ItemStack book = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof RuneGrimoireItem
                    ? player.getItemInHand(InteractionHand.MAIN_HAND) : player.getItemInHand(InteractionHand.OFF_HAND);
            if (!(book.getItem() instanceof RuneGrimoireItem)) return;
            int count = book.getOrDefault(payload.spell ? ModComponents.SPELLS : ModComponents.GRIMOIRE, java.util.List.of()).size();
            if (count > 0) book.set(payload.spell ? ModComponents.SPELL_SELECTION : ModComponents.GRIMOIRE_SELECTION,
                    Math.max(0, Math.min(payload.selected, count - 1)));
        });
    }
}
