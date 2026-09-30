package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** One short lived cast beam, rendered by Photon on nearby clients. */
public record CastBeamPayload(Vec3 start, Vec3 end, int color) implements CustomPacketPayload {
    public static final Type<CastBeamPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "cast_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastBeamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVec3(packet.start);
                buf.writeVec3(packet.end);
                buf.writeInt(packet.color);
            }, buf -> new CastBeamPayload(buf.readVec3(), buf.readVec3(), buf.readInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, STREAM_CODEC, (packet, context) ->
                context.enqueueWork(() -> com.vital.manatech.client.CastBeamEffect.spawn(
                        packet.start, packet.end, packet.color)));
    }

    public static void send(ServerLevel level, Vec3 start, Vec3 end, int color) {
        PacketDistributor.sendToPlayersNear(level, null, start.x, start.y, start.z, 64,
                new CastBeamPayload(start, end, color));
    }
}
