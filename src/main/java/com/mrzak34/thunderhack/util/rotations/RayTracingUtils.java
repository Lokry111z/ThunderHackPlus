package com.mrzak34.thunderhack.util.rotations;


import com.mrzak34.thunderhack.Thunderhack;
import com.mrzak34.thunderhack.modules.combat.Aura;
import com.mrzak34.thunderhack.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;

import javax.vecmath.Vector2f;
import java.util.ArrayList;
import java.util.Arrays;

import static com.mrzak34.thunderhack.util.Util.mc;

public class RayTracingUtils {


    public static ArrayList<Vec3d> getHitBoxPointsJitter(Vec3d position, float fakeBoxScale) {

        float head_height = 1.6f + interpolateRandom(-0.4f, 0.2f);
        float chest_height = 0.8f + interpolateRandom(-0.2f, 0.2f);
        float leggs_height = 0.225f + interpolateRandom(-0.1f, 0.1f);

        Vec3d head1 = position.add(-fakeBoxScale, head_height, fakeBoxScale);
        Vec3d head2 = position.add(0, head_height, fakeBoxScale);
        Vec3d head3 = position.add(fakeBoxScale, head_height, fakeBoxScale);
        Vec3d head4 = position.add(-fakeBoxScale, head_height, 0);
        Vec3d head5 = position.add(fakeBoxScale, head_height, 0);
        Vec3d head6 = position.add(-fakeBoxScale, head_height, -fakeBoxScale);
        Vec3d head7 = position.add(0, head_height, -fakeBoxScale);
        Vec3d head8 = position.add(fakeBoxScale, head_height, -fakeBoxScale);

        Vec3d chest1 = position.add(-fakeBoxScale, chest_height, fakeBoxScale);
        Vec3d chest2 = position.add(0, chest_height, fakeBoxScale);
        Vec3d chest3 = position.add(fakeBoxScale, chest_height, fakeBoxScale);
        Vec3d chest4 = position.add(-fakeBoxScale, chest_height, 0);
        Vec3d chest5 = position.add(fakeBoxScale, chest_height, 0);
        Vec3d chest6 = position.add(-fakeBoxScale, chest_height, -fakeBoxScale);
        Vec3d chest7 = position.add(0, chest_height, -fakeBoxScale);
        Vec3d chest8 = position.add(fakeBoxScale, chest_height, -fakeBoxScale);

        Vec3d legs1 = position.add(-fakeBoxScale, leggs_height, fakeBoxScale);
        Vec3d legs2 = position.add(0, leggs_height, fakeBoxScale);
        Vec3d legs3 = position.add(fakeBoxScale, leggs_height, fakeBoxScale);
        Vec3d legs4 = position.add(-fakeBoxScale, leggs_height, 0);
        Vec3d legs5 = position.add(fakeBoxScale, leggs_height, 0);
        Vec3d legs6 = position.add(-fakeBoxScale, leggs_height, -fakeBoxScale);
        Vec3d legs7 = position.add(0, leggs_height, -fakeBoxScale);
        Vec3d legs8 = position.add(fakeBoxScale, leggs_height, -fakeBoxScale);

        return new ArrayList<>(Arrays.asList(
                head1, head2, head3, head4, head5, head6, head7, head8,
                chest1, chest2, chest3, chest4, chest5, chest6, chest7, chest8,
                legs1, legs2, legs3, legs4, legs5, legs6, legs7, legs8
        ));
    }

    public static ArrayList<Vec3d> getHitBoxPointsNonJitter(Vec3d position, float fakeBoxScale) {

        float head_height = 1.6f;
        float chest_height = 0.8f;
        float leggs_height = 0.225f;

        Vec3d head1 = position.add(-fakeBoxScale, head_height, fakeBoxScale);
        Vec3d head2 = position.add(0, head_height, fakeBoxScale);
        Vec3d head3 = position.add(fakeBoxScale, head_height, fakeBoxScale);
        Vec3d head4 = position.add(-fakeBoxScale, head_height, 0);
        Vec3d head5 = position.add(fakeBoxScale, head_height, 0);
        Vec3d head6 = position.add(-fakeBoxScale, head_height, -fakeBoxScale);
        Vec3d head7 = position.add(0, head_height, -fakeBoxScale);
        Vec3d head8 = position.add(fakeBoxScale, head_height, -fakeBoxScale);

        Vec3d chest1 = position.add(-fakeBoxScale, chest_height, fakeBoxScale);
        Vec3d chest2 = position.add(0, chest_height, fakeBoxScale);
        Vec3d chest3 = position.add(fakeBoxScale, chest_height, fakeBoxScale);
        Vec3d chest4 = position.add(-fakeBoxScale, chest_height, 0);
        Vec3d chest5 = position.add(fakeBoxScale, chest_height, 0);
        Vec3d chest6 = position.add(-fakeBoxScale, chest_height, -fakeBoxScale);
        Vec3d chest7 = position.add(0, chest_height, -fakeBoxScale);
        Vec3d chest8 = position.add(fakeBoxScale, chest_height, -fakeBoxScale);

        Vec3d legs1 = position.add(-fakeBoxScale, leggs_height, fakeBoxScale);
        Vec3d legs2 = position.add(0, leggs_height, fakeBoxScale);
        Vec3d legs3 = position.add(fakeBoxScale, leggs_height, fakeBoxScale);
        Vec3d legs4 = position.add(-fakeBoxScale, leggs_height, 0);
        Vec3d legs5 = position.add(fakeBoxScale, leggs_height, 0);
        Vec3d legs6 = position.add(-fakeBoxScale, leggs_height, -fakeBoxScale);
        Vec3d legs7 = position.add(0, leggs_height, -fakeBoxScale);
        Vec3d legs8 = position.add(fakeBoxScale, leggs_height, -fakeBoxScale);

        return new ArrayList<>(Arrays.asList(
                head1, head2, head3, head4, head5, head6, head7, head8,
                chest1, chest2, chest3, chest4, chest5, chest6, chest7, chest8,
                legs1, legs2, legs3, legs4, legs5, legs6, legs7, legs8
        ));
    }

    public static ArrayList<Vec3d> getHitBoxPointsOldJitter(Vec3d position, float fakeBoxScale) {

        float head_height = 1.6f + interpolateRandom(-0.8f, 0.2f);
        float chest_height = 0.8f + interpolateRandom(-0.6f, 0.2f);
        float leggs_height = 0.15f + interpolateRandom(-0.1f, 0.1f);

        Vec3d head1 = position.add(0, head_height, 0);
        Vec3d chest1 = position.add(0, chest_height, 0);
        Vec3d legs1 = position.add(0, leggs_height, 0);

        return new ArrayList<>(Arrays.asList(
                head1,
                chest1,
                legs1
        ));
    }

    public static ArrayList<Vec3d> getHitBoxPointsOld(Vec3d position, float fakeBoxScale) {

        float head_height = 1.6f;
        float chest_height = 0.8f;
        float leggs_height = 0.15f;

        Vec3d head1 = position.add(0, head_height, 0);
        Vec3d chest1 = position.add(0, chest_height, 0);
        Vec3d legs1 = position.add(0, leggs_height, 0);


        return new ArrayList<>(Arrays.asList(
                head1,
                chest1,
                legs1
        ));
    }


    public static float interpolateRandom(float var0, float var1) {
        return (float) (var0 + (var1 - var0) * Math.random());
    }


    public static Entity getPointedEntity(Vec2f rot, double dst, boolean walls, Entity target) {
        double d0 = dst;
        RayTraceResult objectMouseOver = rayTrace(d0, rot.x, rot.y, walls);
        Vec3d vec3d = mc.player.getPositionEyes(1);
        boolean flag = false;
        double d1 = d0;
        if (objectMouseOver != null) {
            d1 = objectMouseOver.hitVec.distanceTo(vec3d);
        }
        Vec3d vec3d1 = getLook(rot.x, rot.y);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
        Entity pointedEntity = null;
        Vec3d vec3d3 = null;
        double d2 = d1;
        Entity entity1 = target;
        AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(entity1.getCollisionBorderSize(), entity1.getCollisionBorderSize(), entity1.getCollisionBorderSize());
        RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
        if (axisalignedbb.contains(vec3d)) {
            if (d2 >= 0.0D) {
                pointedEntity = entity1;
                vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                d2 = 0.0D;
            }
        } else if (raytraceresult != null) {
            double d3 = vec3d.distanceTo(raytraceresult.hitVec);

            if (d3 < d2 || d2 == 0.0D) {
                boolean flag1 = false;
                if (!flag1 && entity1.getLowestRidingEntity() == mc.player.getLowestRidingEntity()) {
                    if (d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec3d3 = raytraceresult.hitVec;
                    }
                } else {
                    pointedEntity = entity1;
                    vec3d3 = raytraceresult.hitVec;
                    d2 = d3;
                }
            }
        }
        if (pointedEntity != null && flag && vec3d.distanceTo(vec3d3) > dst) {
            pointedEntity = null;
            objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, null, new BlockPos(vec3d3));
        }
        if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
            objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);
        }
        return objectMouseOver != null ? (objectMouseOver.entityHit instanceof Entity ? objectMouseOver.entityHit : null) : null;
    }

    public static RayTraceResult rayTrace(double blockReachDistance, float yaw, float pitch, boolean walls) {
        if (!walls) {
            return null;
        }
        Vec3d vec3d = mc.player.getPositionEyes(1);
        Vec3d vec3d1 = getLook(yaw, pitch);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return mc.world.rayTraceBlocks(vec3d, vec3d2, true, true, true);
    }

    static Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public static Entity getMouseOver(Entity target, float yaw, float pitch, double distance, boolean ignoreWalls) {
        Entity pointedEntity;
        RayTraceResult objectMouseOver;
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.world != null) {
            objectMouseOver = ignoreWalls ? null : rayTrace(distance, yaw, pitch);
            Vec3d vec3d = entity.getPositionEyes(1);
            boolean flag = false;
            double d1 = distance;
            if (distance > 3) {
                flag = true;
            }
            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3d);
            }
            Vec3d vec3d1 = getVectorForRotation(pitch, yaw);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
            pointedEntity = null;
            Vec3d vec3d3 = null;
            double d2 = d1;
            AxisAlignedBB axisalignedbb = target.getEntityBoundingBox().expand(target.getCollisionBorderSize(), target.getCollisionBorderSize(), target.getCollisionBorderSize());
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
            if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0D) {
                    pointedEntity = target;
                    vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                    d2 = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                if (d3 < d2 || d2 == 0.0D) {
                    boolean flag1 = false;

                    if (!flag1 && target.getLowestRidingEntity() == entity.getLowestRidingEntity()) {
                        if (d2 == 0.0D) {
                            pointedEntity = target;
                            vec3d3 = raytraceresult.hitVec;
                        }
                    } else {
                        pointedEntity = target;
                        vec3d3 = raytraceresult.hitVec;
                        d2 = d3;
                    }
                }
            }
            if (pointedEntity != null && flag && vec3d.distanceTo(vec3d3) > distance) {
                pointedEntity = null;
                objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, null,
                        new BlockPos(vec3d3));
            }
            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);
            }
            if (objectMouseOver == null)
                return null;
            return objectMouseOver.entityHit;
        }
        return null;
    }


    public static RayTraceResult rayTrace(double blockReachDistance, float yaw, float pitch) {
        Vec3d vec3d = Minecraft.getMinecraft().player.getPositionEyes(1);
        Vec3d vec3d1 = getVectorForRotation(pitch, yaw);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return Minecraft.getMinecraft().world.rayTraceBlocks(vec3d, vec3d2, true, true, true);
    }

    static Vec3d getLook(float yaw, float pitch) {
        return getVectorForRotation(pitch, yaw);
    }

    public static ArrayList<Vec3d> getHitBoxPoints(Vec3d position, float fakeBoxScale) {
        Setting<Aura.RayTracingMode> mode = Thunderhack.moduleManager.getModuleByClass(Aura.class).rayTracing;
        switch (mode.getValue()) {
            case New:
                return getHitBoxPointsNonJitter(position, fakeBoxScale);
            case Old:
                return getHitBoxPointsOld(position, fakeBoxScale);
            case OldJitter:
                return getHitBoxPointsOldJitter(position, fakeBoxScale);
            case NewJitter:
                return getHitBoxPointsJitter(position, fakeBoxScale);
            default:
                return getHitBoxPointsNonJitter(position, fakeBoxScale);
        }
    }

    public static Vec3d getVecTarget(Entity target, double distance) {
        Vec3d vec = target.getPositionVector().add(new Vec3d(0, MathHelper.clamp(target.getEyeHeight() * (mc.player.getDistance(target) / (distance + target.width)), 0.2, mc.player.getEyeHeight()), 0));
        if (!isHitBoxVisible(vec)) {
            for (double i = target.width * 0.05; i <= target.width * 0.95; i += target.width * 0.9 / 8f) {
                for (double j = target.width * 0.05; j <= target.width * 0.95; j += target.width * 0.9 / 8f) {
                    for (double k = 0; k <= target.height; k += target.height / 8f) {
                        if (isHitBoxVisible(new Vec3d(i, k, j).add(target.getPositionVector().add(new Vec3d(-target.width / 2, 0, -target.width / 2))))) {
                            vec = new Vec3d(i, k, j).add(target.getPositionVector().add(new Vec3d(-target.width / 2, 0, -target.width / 2)));
                            break;
                        }
                    }
                }
            }
        }
        if (getDistanceFromHead(vec) > distance * distance) {
            return null;
        }
        return vec;
    }

    public static boolean isHitBoxVisible(Vec3d vec3d) {
        final Vec3d eyesPos = new Vec3d(mc.player.posX, mc.player.getEntityBoundingBox().minY + mc.player.getEyeHeight(), mc.player.posZ);
        return mc.world.rayTraceBlocks(eyesPos, vec3d, false, true, false) == null;
    }

    public static float getDistanceFromHead(Vec3d d1) {
        double x = d1.x - mc.player.posX;
        double y = d1.y - mc.player.getPositionEyes(1).y;
        double z = d1.z - mc.player.posZ;
        return (float) (Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }
}
