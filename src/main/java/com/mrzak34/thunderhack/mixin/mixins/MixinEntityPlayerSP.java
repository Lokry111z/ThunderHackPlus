package com.mrzak34.thunderhack.mixin.mixins;

import com.mrzak34.thunderhack.events.*;
import com.mrzak34.thunderhack.manager.EventManager;
import com.mrzak34.thunderhack.mixin.ducks.IEntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.MoverType;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mrzak34.thunderhack.util.Util.mc;


@Mixin(value = {EntityPlayerSP.class}, priority = 9998)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer implements IEntityPlayerSP {


    @Shadow
    public final NetHandlerPlayClient connection;
    public Runnable auraCallBack;
    boolean pre_sprint_state = false;
    private boolean updateLock = false;

    public MixinEntityPlayerSP(Minecraft p_i47378_1_, World p_i47378_2_, NetHandlerPlayClient p_i47378_3_, StatisticsManager p_i47378_4_, RecipeBook p_i47378_5_, NetHandlerPlayClient connection) {
        super(p_i47378_2_, p_i47378_3_.getGameProfile());
        this.connection = p_i47378_3_;
    }

    @Shadow
    public abstract boolean isSneaking();


    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isCurrentViewEntity()Z"))
    private boolean redirectIsCurrentViewEntity(EntityPlayerSP entityPlayerSP) {
        FreecamEvent event = new FreecamEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return entityPlayerSP == mc.player;
        }
        return mc.getRenderViewEntity() == entityPlayerSP;
    }

    @Inject(method = {"onUpdate"}, at = {@At(value = "HEAD")})
    private void updateHook(CallbackInfo info) {
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
        MinecraftForge.EVENT_BUS.post(playerUpdateEvent);
    }

    @Shadow
    protected abstract void onUpdateWalkingPlayer();

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "net/minecraft/client/entity/EntityPlayerSP.onUpdateWalkingPlayer()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void PostUpdateHook(CallbackInfo info) {
        if (updateLock) {
            return;
        }
        PostPlayerUpdateEvent playerUpdateEvent = new PostPlayerUpdateEvent();
        MinecraftForge.EVENT_BUS.post(playerUpdateEvent);
        if (playerUpdateEvent.isCanceled()) {
            info.cancel();
            if (playerUpdateEvent.getIterations() > 0) {
                for (int i = 0; i < playerUpdateEvent.getIterations(); i++) {
                    updateLock = true;
                    onUpdate();
                    updateLock = false;
                    onUpdateWalkingPlayer();
                }
            }
        }
    }

    @Redirect(method = "updateEntityActionState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isCurrentViewEntity()Z"))
    private boolean redirectIsCurrentViewEntity2(EntityPlayerSP entityPlayerSP) {

        Minecraft mc = Minecraft.getMinecraft();
        FreecamEvent event = new FreecamEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return entityPlayerSP == mc.player;
        }
        return mc.getRenderViewEntity() == entityPlayerSP;
    }

    @Inject(method = {"pushOutOfBlocks"}, at = {@At("HEAD")}, cancellable = true)
    private void pushOutOfBlocksHook(final double x, final double y, final double z, final CallbackInfoReturnable<Boolean> info) {
        final PushEvent event = new PushEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = {"onUpdateWalkingPlayer"}, at = {@At(value = "HEAD")}, cancellable = true)
    private void preMotion(CallbackInfo info) {
        EventSync event = new EventSync(rotationYaw, rotationPitch, onGround);
        MinecraftForge.EVENT_BUS.post(event);
        EventSprint e = new EventSprint(isSprinting());
        MinecraftForge.EVENT_BUS.post(e);

        if (e.getSprintState() != ((com.mrzak34.thunderhack.mixin.mixins.IEntityPlayerSP) mc.player).getServerSprintState()) {
            if (e.getSprintState()) {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SPRINTING));
            }
            ((com.mrzak34.thunderhack.mixin.mixins.IEntityPlayerSP) mc.player).setServerSprintState(e.getSprintState());
        }
        pre_sprint_state = ((com.mrzak34.thunderhack.mixin.mixins.IEntityPlayerSP) mc.player).getServerSprintState();
        EventManager.lock_sprint = true;
        if (event.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void movePre(MoverType type, double x, double y, double z, CallbackInfo info) {
        EventMove event = new EventMove(type, x, y, z);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            super.move(type, event.get_x(), event.get_y(), event.get_z());
            info.cancel();
        }
    }


    @Inject(method = {"onUpdateWalkingPlayer"}, at = {@At(value = "RETURN")})
    private void postMotion(CallbackInfo info) {
        ((com.mrzak34.thunderhack.mixin.mixins.IEntityPlayerSP) mc.player).setServerSprintState(pre_sprint_state);
        EventManager.lock_sprint = false;

        EventPostSync event = new EventPostSync();
        MinecraftForge.EVENT_BUS.post(event);

        if (!event.getPostEvents().isEmpty()) {
            for (Runnable runnable : event.getPostEvents()) {
                Minecraft.getMinecraft().addScheduledTask(runnable);
            }
        }
    }


    public void setAuraCallback(Runnable auraCallBack) {
        this.auraCallBack = auraCallBack;
    }
}