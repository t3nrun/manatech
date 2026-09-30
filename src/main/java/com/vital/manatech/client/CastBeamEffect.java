package com.vital.manatech.client;

import com.lowdragmc.photon.client.fx.IEffectExecutor;
import com.lowdragmc.photon.client.gameobject.emitter.beam.BeamConfig;
import com.lowdragmc.photon.client.gameobject.emitter.beam.BeamEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;

/** Photon beam emitters provide continuous laser geometry and small electric strands. */
public final class CastBeamEffect {
    private CastBeamEffect() {}

    public static void spawn(Vec3 start, Vec3 end, int color) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !finite(start) || !finite(end) || start.distanceToSqr(end) > 64 * 64) return;
        IEffectExecutor executor = () -> level;
        beam(executor, start, end, color, .18f, 70);
        beam(executor, start, end, color, .065f, 225);

        Vec3 axis = end.subtract(start);
        if (axis.lengthSqr() < .01) return;
        Vec3 tangent = axis.normalize();
        Vec3 side = tangent.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .01) side = tangent.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        Vec3 up = tangent.cross(side).normalize();
        Random random = new Random(Double.doubleToLongBits(start.x + end.z) ^ System.nanoTime());
        for (int strand = 0; strand < 2; strand++) {
            Vec3 previous = start;
            for (int i = 1; i <= 12; i++) {
                double progress = i / 12.0;
                Vec3 point = start.lerp(end, progress);
                if (i < 12) {
                    double envelope = Math.sin(Math.PI * progress) * (strand == 0 ? .12 : .08);
                    point = point.add(side.scale((random.nextDouble() * 2 - 1) * envelope))
                            .add(up.scale((random.nextDouble() * 2 - 1) * envelope));
                }
                beam(executor, previous, point, color, .018f, 155);
                previous = point;
            }
        }
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static void beam(IEffectExecutor executor, Vec3 start, Vec3 end,
                             int color, float width, int alpha) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < .0001) return;
        BeamConfig config = new BeamConfig();
        config.setDuration(8);
        config.setLooping(false);
        config.getEnd().set((float) delta.x, (float) delta.y, (float) delta.z);
        config.setWidth(new Constant(width));
        config.setColor(new Color((alpha << 24) | (color & 0xFFFFFF)));
        new BeamEmitter(config).emit(executor, new Vector3f((float) start.x, (float) start.y,
                (float) start.z), null, null);
    }
}
