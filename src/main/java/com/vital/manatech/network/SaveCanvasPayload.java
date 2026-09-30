package com.vital.manatech.network;

import com.vital.manatech.ManatechMod;
import com.vital.manatech.block.RuneTableBlock;
import com.vital.manatech.block.entity.RuneTableBlockEntity;
import com.vital.manatech.item.RuneStylusItem;
import com.vital.manatech.rune.DiagramLayer;
import com.vital.manatech.rune.RuneTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/** Saves the editable canvas on the table, including layers not yet exported. */
public record SaveCanvasPayload(BlockPos table, List<String> layers) implements CustomPacketPayload {
    public static final Type<SaveCanvasPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ManatechMod.MOD_ID, "save_canvas"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCanvasPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.table);
                buf.writeVarInt(payload.layers.size());
                for (String layer : payload.layers) buf.writeUtf(layer, 32767);
            }, buf -> {
                BlockPos table = buf.readBlockPos();
                int count = buf.readVarInt();
                if (count < 0 || count > 16) throw new IllegalArgumentException("Invalid layer count");
                List<String> layers = new ArrayList<>();
                for (int i = 0; i < count; i++) layers.add(buf.readUtf(32767));
                return new SaveCanvasPayload(table, layers);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            var player = context.player();
            if (player.distanceToSqr(payload.table.getX() + .5, payload.table.getY() + .5, payload.table.getZ() + .5) > 64) return;
            var state = player.level().getBlockState(payload.table);
            if (!(state.getBlock() instanceof RuneTableBlock block)
                    || !(player.level().getBlockEntity(RuneTableBlock.firstPos(payload.table, state)) instanceof RuneTableBlockEntity table)) return;
            RuneStylusItem stylus = player.getMainHandItem().getItem() instanceof RuneStylusItem main ? main
                    : player.getOffhandItem().getItem() instanceof RuneStylusItem off ? off : null;
            if (stylus == null) return;
            int tier = RuneTier.effectiveTier(stylus.upgradeLevel(), block.upgradeLevel());
            if (payload.layers.size() > RuneTier.layers(tier)) return;
            int index=0;
            for (String encoded : payload.layers) {
                DiagramLayer layer = DiagramLayer.decode(encoded);
                if (layer.schemeLayer() >= 0 && !RuneTier.allowsScheme(tier, layer.schemeLayer())) return;
                if (!RuneTier.allowsSymbols(stylus.upgradeLevel(), layer)) {
                    if(index >= table.canvasLayers().size()) return;
                    var old=DiagramLayer.decode(table.canvasLayers().get(index)).symbols();
                    if(old.size()!=layer.symbols().size()) return;
                    for(int n=0;n<old.size();n++) if(old.get(n).id()!=layer.symbols().get(n).id()
                            || old.get(n).slot()!=layer.symbols().get(n).slot()
                            || old.get(n).element()!=layer.symbols().get(n).element()) return;
                }
                if (!layer.encode().equals(encoded) || layer.symbols().size() > DiagramLayer.MAX_SYMBOLS) return;
                index++;
            }
            table.saveCanvasLayers(payload.layers);
        }));
    }
}
