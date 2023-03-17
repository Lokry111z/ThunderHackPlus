package com.mrzak34.thunderhack.manager;

import com.mrzak34.thunderhack.events.PacketEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.mrzak34.thunderhack.manager.EventManager.fullNullCheck;
import static com.mrzak34.thunderhack.util.Util.mc;

/**
 * Manages the last position that has been
 * reported to or, via SPacketPlayerPosLook,
 * by the server.
 */
public class PositionManager{

    private boolean blocking;
    private volatile int teleportID;
    private volatile double last_x;
    private volatile double last_y;
    private volatile double last_z;
    private volatile boolean onGround;
    private volatile boolean sprinting;

    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void unload() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.SendPost e) {

        if (e.getPacket() instanceof CPacketPlayer.Position) {
            readCPacket(e.getPacket());
        }
        if (e.getPacket() instanceof CPacketPlayer.PositionRotation) {
            readCPacket(e.getPacket());
        }
        if (e.getPacket() instanceof CPacketEntityAction) {
            CPacketEntityAction action = e.getPacket();
            if (action.getAction() == CPacketEntityAction.Action.START_SPRINTING) {
                sprinting = true;
            }
            if (action.getAction() == CPacketEntityAction.Action.STOP_SPRINTING) {
                sprinting = false;
            }
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;

        if (e.getPacket() instanceof SPacketPlayerPosLook) {

            EntityPlayerSP player = mc.player;
            if (player == null) {
                if (!mc.isCallingFromMinecraftThread()) {
                    mc.addScheduledTask(() -> this.onPacketReceive(e));
                }

                return;
            }

            SPacketPlayerPosLook packet = e.getPacket();
            double x = packet.getX();
            double y = packet.getY();
            double z = packet.getZ();

            if (packet.getFlags()
                    .contains(SPacketPlayerPosLook.EnumFlags.X)) {
                x += player.posX;
            }

            if (packet.getFlags()
                    .contains(SPacketPlayerPosLook.EnumFlags.Y)) {
                y += player.posY;
            }

            if (packet.getFlags()
                    .contains(SPacketPlayerPosLook.EnumFlags.Z)) {
                z += player.posZ;
            }

            last_x = MathHelper.clamp(x, -3.0E7, 3.0E7);
            last_y = y;
            last_z = MathHelper.clamp(z, -3.0E7, 3.0E7);

            player.serverPosX = EntityTracker.getPositionLong(last_x);
            player.serverPosY = EntityTracker.getPositionLong(last_y);
            player.serverPosZ = EntityTracker.getPositionLong(last_z);


            onGround = false;
            teleportID = packet.getTeleportId();


        }


    }

    public int getTeleportID() {
        return teleportID;
    }

    public double getX() {
        return last_x;
    }

    public double getY() {
        return last_y;
    }

    public double getZ() {
        return last_z;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public AxisAlignedBB getBB() {
        double x = this.last_x;
        double y = this.last_y;
        double z = this.last_z;
        float w = mc.player.width / 2.0f;
        float h = mc.player.height;
        return new AxisAlignedBB(x - w, y, z - w, x + w, y + h, z + w);
    }

    public Vec3d getVec() {
        return new Vec3d(last_x, last_y, last_z);
    }

    public void readCPacket(CPacketPlayer packetIn) {
        last_x = packetIn.getX(mc.player.posX);
        last_y = packetIn.getY(mc.player.posY);
        last_z = packetIn.getZ(mc.player.posZ);
        EntityPlayer player;
        if ((player = mc.player) != null) {
            player.serverPosX = EntityTracker.getPositionLong(last_x);
            player.serverPosY = EntityTracker.getPositionLong(last_y);
            player.serverPosZ = EntityTracker.getPositionLong(last_z);
        }

        setOnGround(packetIn.isOnGround());
    }

    public double getDistanceSq(Entity entity) {
        return getDistanceSq(entity.posX, entity.posY, entity.posZ);
    }

    public double getDistanceSq(double x, double y, double z) {
        double xDiff = last_x - x;
        double yDiff = last_y - y;
        double zDiff = last_z - z;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canEntityBeSeen(Entity entity) {
        return mc.world.rayTraceBlocks(
                new Vec3d(last_x, last_y + mc.player.getEyeHeight(), last_z),
                new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ),
                false,
                true,
                false) == null;
    }

    public void set(double x, double y, double z) {
        this.last_x = x;
        this.last_y = y;
        this.last_z = z;
    }

    /**
     * Indicates that a module is currently
     * spoofing the position and it shouldn't
     * be spoofed by others.
     *
     * @return <tt>true</tt> if blocking.
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Makes {@link PositionManager#isBlocking()} return the given
     * argument, that won't prevent other modules from
     * spoofing positions but they can check it. For more info
     * see {@link RotationManager#setBlocking(boolean)}.
     * <p>
     * Remember to set this to false after
     * the Rotations have been sent.
     *
     * @param blocking blocks position spoofing
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isSprintingSS() {
        return sprinting;
    }

}