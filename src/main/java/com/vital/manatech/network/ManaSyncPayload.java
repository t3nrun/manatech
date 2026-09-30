package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.magic.PlayerMana;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ManaSyncPayload(double mana, double maximum, int level, long absorbed) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "mana_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> { buf.writeDouble(packet.mana); buf.writeDouble(packet.maximum); buf.writeVarInt(packet.level); buf.writeVarLong(packet.absorbed); },
            buf -> new ManaSyncPayload(buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToClient(TYPE, STREAM_CODEC, (packet, context) ->
                context.enqueueWork(() -> com.vital.manatech.client.ManaHud.sync(packet)));
    }
    public static void send(ServerPlayer player, double mana) {
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(mana, PlayerMana.maximum(player), PlayerMana.level(player), PlayerMana.absorbed(player)));
    }
}
