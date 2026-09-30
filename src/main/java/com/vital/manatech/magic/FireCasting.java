package com.vital.manatech.magic;

import com.vital.manatech.magic.FireSpells.Form;
import com.vital.manatech.network.CastBeamPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Bounded server effects for the first six Fire circles. All damage and item effects happen server side. */
@EventBusSubscriber(modid="manatech")
public final class FireCasting {
    private static final Map<ServerLevel,List<Active>> ACTIVE=new WeakHashMap<>();
    private record Active(UUID owner,Form form,Vec3 target,int duration,int age,float power) {
        Active next() { return new Active(owner,form,target,duration,age+1,power); }
    }
    private FireCasting() {}
    public static void cast(ServerPlayer player,Form form,int circle) {
        ServerLevel level=player.serverLevel();
        Vec3 eye=player.getEyePosition(),look=player.getLookAngle();
        HitResult hit=player.pick(Math.min(56,12+8*circle),0,false);
        Vec3 target=hit.getLocation();
        float power=Math.min(32,2+circle*2+Integer.bitCount(form.mask()));
        int red=0xFFFF1824;
        switch(form) {
            case FLAME -> schedule(level,player,form,target,80,power);
            case SPARKS -> { for(int n=0;n<12;n++) particles(level,eye.add(look.scale(1+n*.5)),2); damageLine(level,player,eye,target,power*.4f,2); }
            case HEAT -> heat(level,player,hit);
            case FIREBALL -> projectile(level,player,look,false,0);
            case BARRAGE -> volley(level,player,look,3,false,0);
            case ZONE -> schedule(level,player,form,target,100,power);
            case METEOR -> projectile(level,player,look,true,1);
            case FLAMETHROWER -> schedule(level,player,form,target,45,power);
            case GREATER_BARRAGE -> volley(level,player,look,6,false,0);
            case METEORITE -> projectile(level,player,look,true,2);
            case TORNADO -> schedule(level,player,form,target,100,power);
            case LONG_BARRAGE -> schedule(level,player,form,target,90,power);
            case HELD_FLAME -> schedule(level,player,form,target,90,power);
            case METEOR_RAIN -> schedule(level,player,form,target,100,power);
            case STORM -> schedule(level,player,form,target,120,power);
            case PILLAR -> schedule(level,player,form,target,100,power);
            case FLAME_BEAM -> schedule(level,player,form,target,60,power);
            case DISINTEGRATION -> {
                CastBeamPayload.send(level,eye,target,red);
                damageLine(level,player,eye,target,power*2,5);
                if(hit instanceof BlockHitResult blockHit) {
                    BlockPos pos=blockHit.getBlockPos();
                    var state=level.getBlockState(pos);
                    if(player.mayInteract(level,pos) && !state.hasBlockEntity() && state.getDestroySpeed(level,pos)>=0 && state.getDestroySpeed(level,pos)<=3)
                        level.destroyBlock(pos,true,player);
                }
            }
            case SUN_SHARD -> { CastBeamPayload.send(level,eye,target,0xFFFFC97F); impact(level,player,target,5,power*2); }
            case BIG_EXPLOSION -> impact(level,player,target,7,power*2);
            case HOLY_BEAM -> {
                CastBeamPayload.send(level,eye,target,0xFFFFEEB7);
                damageLine(level,player,eye,target,power*1.5f,4);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,120,1));
            }
        }
    }
    private static void schedule(ServerLevel level,ServerPlayer owner,Form form,Vec3 target,int duration,float power) {
        var list=ACTIVE.computeIfAbsent(level,l->new ArrayList<>());
        if(list.size()<128) list.add(new Active(owner.getUUID(),form,target,duration,0,power));
    }
    private static void projectile(ServerLevel level,ServerPlayer player,Vec3 dir,boolean large,int blast) {
        Vec3 origin=player.getEyePosition().add(dir.scale(1.1));
        if(large) { var ball=new LargeFireball(level,player,dir.normalize(),blast); ball.setPos(origin); level.addFreshEntity(ball); }
        else { var ball=new SmallFireball(level,player,dir.normalize()); ball.setPos(origin); level.addFreshEntity(ball); }
    }
    private static void volley(ServerLevel level,ServerPlayer player,Vec3 look,int count,boolean large,int blast) {
        Vec3 side=look.cross(new Vec3(0,1,0)); if(side.lengthSqr()<.001) side=new Vec3(1,0,0); side=side.normalize();
        for(int i=0;i<count;i++) projectile(level,player,look.add(side.scale((i-(count-1)/2.0)*.12)).normalize(),large,blast);
    }
    private static void heat(ServerLevel level,ServerPlayer player,HitResult hit) {
        if(hit instanceof BlockHitResult blockHit) {
            BlockPos pos=blockHit.getBlockPos(); var state=level.getBlockState(pos);
            if(player.mayInteract(level,pos) && state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT))
                level.setBlock(pos,state.setValue(CampfireBlock.LIT,true),3);
        }
        particles(level,hit.getLocation(),24);
    }
    private static void particles(ServerLevel level,Vec3 point,int count) {
        level.sendParticles(ParticleTypes.FLAME,point.x,point.y,point.z,count,.25,.25,.25,.01);
    }
    private static void damageLine(ServerLevel level,ServerPlayer owner,Vec3 start,Vec3 end,float damage,int seconds) {
        Vec3 delta=end.subtract(start); double length=Math.min(56,delta.length()); if(length<.01)return;
        Vec3 axis=delta.normalize();
        for(var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1.3),e->e!=owner && e.isAlive())) {
            Vec3 relative=living.getBoundingBox().getCenter().subtract(start);
            double distance=Math.clamp(relative.dot(axis),0,length);
            if(living.getBoundingBox().getCenter().distanceToSqr(start.add(axis.scale(distance)))<2.25) {
                living.hurt(owner.damageSources().magic(),damage);
                living.igniteForSeconds(seconds);
            }
        }
    }
    private static void area(ServerLevel level,ServerPlayer owner,Vec3 target,double radius,float damage,boolean lift) {
        AABB box=new AABB(target,target).inflate(radius);
        for(var living:level.getEntitiesOfClass(LivingEntity.class,box,e->e!=owner && e.isAlive())) {
            if(living.distanceToSqr(target)>radius*radius) continue;
            living.hurt(owner.damageSources().magic(),damage);
            living.igniteForSeconds(3);
            if(lift) living.push(0,.28,0);
        }
    }
    private static void impact(ServerLevel level,ServerPlayer owner,Vec3 target,double radius,float damage) {
        area(level,owner,target,radius,damage,false);
        level.sendParticles(ParticleTypes.EXPLOSION,target.x,target.y,target.z,8,radius*.3,radius*.3,radius*.3,0);
        particles(level,target,40);
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var list=ACTIVE.get(level); if(list==null)return;
        var iterator=list.listIterator();
        while(iterator.hasNext()) {
            Active active=iterator.next();
            ServerPlayer owner=level.getServer().getPlayerList().getPlayer(active.owner());
            if(owner==null || owner.level()!=level || active.age()>=active.duration() || !level.hasChunkAt(BlockPos.containing(active.target()))) { iterator.remove(); continue; }
            int age=active.age(); Form form=active.form(); Vec3 target=active.target();
            if(age%4==0) {
                if(form==Form.FLAME) particles(level,owner.getEyePosition().add(owner.getLookAngle().scale(2)),3);
                else if(form==Form.FLAMETHROWER || form==Form.HELD_FLAME || form==Form.FLAME_BEAM) {
                    Vec3 start=owner.getEyePosition().add(owner.getLookAngle().scale(.8));
                    Vec3 end=owner.pick(form==Form.FLAME_BEAM?32:10,0,false).getLocation();
                    CastBeamPayload.send(level,start,end,0xFFFF1824);
                    damageLine(level,owner,start,end,active.power()*.5f,2);
                } else particles(level,target,form==Form.STORM?12:5);
            }
            if(age%10==0) {
                switch(form) {
                    case ZONE -> area(level,owner,target,3,active.power()*.55f,false);
                    case TORNADO -> area(level,owner,target,4,active.power()*.6f,true);
                    case STORM -> area(level,owner,target,6,active.power()*.8f,false);
                    case PILLAR -> area(level,owner,target,2.5,active.power(),false);
                    case METEOR_RAIN -> {
                        double angle=age*2.39996;
                        Vec3 origin=target.add(Math.cos(angle)*4,16,Math.sin(angle)*4);
                        var ball=new LargeFireball(level,owner,target.subtract(origin).normalize(),1);
                        ball.setPos(origin); level.addFreshEntity(ball);
                    }
                    case LONG_BARRAGE -> projectile(level,owner,owner.getLookAngle(),false,0);
                    default -> {}
                }
            }
            iterator.set(active.next());
        }
        if(list.isEmpty())ACTIVE.remove(level);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); }
}
