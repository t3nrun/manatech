package com.vital.manatech.magic;

import com.vital.manatech.rune.AssembledSpell;
import com.vital.manatech.rune.DiagramLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = com.vital.manatech.ManatechMod.MOD_ID)
public final class PlayerMana {
    private static final String KEY = "manatech.mana";
    private static final String LEVEL = "manatech.level";
    private static final String ABSORBED = "manatech.rank_absorbed";
    private static final String CAST_TICK = "manatech.last_cast";
    private PlayerMana() {}
    public static void initialize(Player player) {
        CompoundTag tag = player.getPersistentData();
        if (tag.contains(LEVEL)) return;
        int level = 1 + player.getRandom().nextInt(11);
        tag.putInt(LEVEL, level);
        tag.putLong(ABSORBED, ManaProgression.absorptionAtLevel(level));
        tag.putDouble(KEY, Math.min(tag.contains(KEY) ? tag.getDouble(KEY) : maximum(player), maximum(player)));
    }
    public static int level(Player player) { return Math.clamp(player.getPersistentData().getInt(LEVEL), 1, 100); }
    public static int rank(Player player) { return ManaProgression.rank(level(player)); }
    public static double maximum(Player player) { return ManaProgression.maximum(level(player)); }
    public static long absorbed(Player player) { return Math.max(0, player.getPersistentData().getLong(ABSORBED)); }
    public static boolean spend(Player player, double cost) {
        if (player.level().isClientSide || !Double.isFinite(cost) || cost <= 0) return false;
        initialize(player);
        if (!player.getAbilities().instabuild && get(player) < cost) return false;
        if (!player.getAbilities().instabuild) player.getPersistentData().putDouble(KEY, get(player) - cost);
        if (player instanceof ServerPlayer server) com.vital.manatech.network.ManaSyncPayload.send(server, get(player));
        return true;
    }
    public static double get(Player player) {
        CompoundTag tag = player.getPersistentData();
        double value = tag.contains(KEY) ? tag.getDouble(KEY) : maximum(player);
        return Double.isFinite(value) ? Math.clamp(value, 0, maximum(player)) : 0;
    }
    public static long absorb(Player player, long available) {
        if (player.level().isClientSide || available <= 0) return 0;
        initialize(player);
        long taken = Math.min(available, (long)Math.floor(maximum(player) - get(player)));
        if (taken <= 0) return 0;
        player.getPersistentData().putDouble(KEY, get(player) + taken);
        int oldLevel = level(player), rank = rank(player);
        long progress = Math.min(ManaProgression.rankAbsorption(rank), absorbed(player)) + taken;
        while (rank < 7 && progress >= ManaProgression.rankAbsorption(rank)) {
            progress -= ManaProgression.rankAbsorption(rank);
            rank++;
        }
        progress = Math.min(progress, ManaProgression.rankAbsorption(rank));
        int newLevel = ManaProgression.levelAt(rank, progress);
        player.getPersistentData().putInt(LEVEL, newLevel);
        player.getPersistentData().putLong(ABSORBED, progress);
        if (newLevel > oldLevel) player.displayClientMessage(Component.translatable("message.manatech.level_up", newLevel, Component.translatable("rank.manatech." + rank)), true);
        if (player instanceof ServerPlayer server) com.vital.manatech.network.ManaSyncPayload.send(server, get(player));
        return taken;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        initialize(player);
        double current = get(player), max = maximum(player);
        if (current < max) {
            // 0.5% of maximum per second, at 20 server ticks per second.
            player.getPersistentData().putDouble(KEY, Math.min(max, current + max * .005 / 20));
        }
        if (player instanceof ServerPlayer server && player.tickCount % 10 == 0) {
            com.vital.manatech.network.ManaSyncPayload.send(server, get(player));
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var from = event.getOriginal();
        initialize(from);
        var tag = event.getEntity().getPersistentData();
        tag.putInt(LEVEL, level(from));
        tag.putLong(ABSORBED, absorbed(from));
        tag.putDouble(KEY, get(from));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initialize(player);
            com.vital.manatech.network.ManaSyncPayload.send(player, get(player));
        }
    }

    public static void cast(Player player, AssembledSpell spell) {
        if (!(player instanceof ServerPlayer server) || spell.isEmpty()) return;
        if (!spell.hasCentralCreation()) {
            player.displayClientMessage(Component.translatable("message.manatech.creation_required"), true); return;
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(CAST_TICK) + 12 > now) return;
        initialize(player);
        if(FireSpells.dominant(spell)==0 && FireSpells.circle(spell)>6) {
            player.displayClientMessage(Component.translatable("message.manatech.fire_seventh_planned"),true);return;
        }
        var assembly=com.vital.manatech.rune.SpellAssembly.inspect(spell);
        if(!assembly.valid()) {
            player.displayClientMessage(Component.translatable("message.manatech.assembly_"+assembly.issue(),assembly.layer(),assembly.required()),true);return;
        }
        if(rank(player)<assembly.circle()&&!player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.manatech.circle_rank",assembly.circle()),true);return;
        }
        FireSpells.Form fire=FireSpells.resolve(spell);
        int fireCircle=fire==null?0:FireSpells.circle(spell);
        if(fire!=null && rank(player)<fireCircle && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.manatech.fire_rank",fireCircle),true);
            return;
        }
        double cost = fire==null?spell.manaCost():FireSpells.manaCost(spell,fire);
        double current = get(player);
        if (!player.getAbilities().instabuild && current < cost) {
            player.displayClientMessage(Component.translatable("message.manatech.no_mana", com.vital.manatech.util.CompactNumbers.format(cost)), true);
            return;
        }
        if (!player.getAbilities().instabuild) player.getPersistentData().putDouble(KEY, current - cost);
        player.getPersistentData().putLong(CAST_TICK, now);
        if(fire!=null) {
            FireCasting.cast(server,fire,fireCircle);
            player.displayClientMessage(Component.translatable("spell.manatech.fire."+fire.id()),true);
            com.vital.manatech.network.ManaSyncPayload.send(server,get(player));
            return;
        }
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(.8));
        Vec3 direction = player.getLookAngle();
        int[] counts = new int[com.vital.manatech.rune.hex.SpellElement.values().length];
        for (DiagramLayer layer : spell.diagrams()) {
            layer.strokes().forEach(s -> counts[Math.floorMod(s.element(), counts.length)]++);
            layer.symbols().forEach(s -> counts[Math.floorMod(s.element(), counts.length)] += 2);
        }
        int dominant = 0;
        for (int i = 1; i < counts.length; i++) if (counts[i] > counts[dominant]) dominant = i;
        float power = Math.min(12, 2 + spell.layers().size() + counts[dominant] / 3f);
        var hit = player.pick(24, 0, false);
        Vec3 target = hit.getLocation();
        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(), player, start,
                target, player.getBoundingBox().expandTowards(target.subtract(start)).inflate(1),
                entity -> entity.isPickable() && entity != player);
        Vec3 beamEnd = entityHit != null && start.distanceToSqr(entityHit.getLocation()) < start.distanceToSqr(target)
                ? entityHit.getLocation() : target;
        com.vital.manatech.network.CastBeamPayload.send((ServerLevel) player.level(), start, beamEnd,
                com.vital.manatech.rune.hex.SpellElement.values()[dominant].color());
        if (entityHit != null && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            if (dominant == 1) living.heal(power);
            else living.hurt(player.damageSources().magic(), power);
        }
        com.vital.manatech.network.ManaSyncPayload.send(server, get(player));
    }
}
