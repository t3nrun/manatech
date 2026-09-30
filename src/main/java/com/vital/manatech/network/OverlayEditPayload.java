package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.entity.OverlayTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-authoritative edits to an imported-page composition. */
public record OverlayEditPayload(BlockPos table, int action, int index, int first, int second)
        implements CustomPacketPayload {
    public static final int ADD = 0;
    public static final int MOVE = 1;
    public static final int SCALE = 2;
    public static final int ROTATE = 3;
    public static final int REORDER = 4;
    public static final int REMOVE = 5;
    public static final int ALIGN = 6;
    public static final Type<OverlayEditPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "overlay_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OverlayEditPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.table);
                buffer.writeVarInt(payload.action);
                buffer.writeVarInt(payload.index);
                buffer.writeVarInt(payload.first);
                buffer.writeVarInt(payload.second);
            }, buffer -> new OverlayEditPayload(buffer.readBlockPos(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            var player = context.player();
            if (player.distanceToSqr(payload.table.getX() + .5, payload.table.getY() + .5,
                    payload.table.getZ() + .5) > 64) return;
            if (!(player.level().getBlockEntity(payload.table) instanceof OverlayTableBlockEntity table)) return;
            if (payload.action == ADD) table.addPlacement(payload.index,
                    player.getAbilities().instabuild ? 16 : 12);
            else table.editPlacement(payload.index, payload.action, payload.first, payload.second);
        }));
    }
}
