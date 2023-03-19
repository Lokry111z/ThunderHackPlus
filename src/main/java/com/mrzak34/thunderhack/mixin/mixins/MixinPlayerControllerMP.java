package com.mrzak34.thunderhack.mixin.mixins;

import com.mrzak34.thunderhack.Thunderhack;
import com.mrzak34.thunderhack.events.*;
import com.mrzak34.thunderhack.mixin.ducks.IPlayerControllerMP;
import com.mrzak34.thunderhack.modules.misc.NoGlitchBlock;
import com.mrzak34.thunderhack.modules.player.Reach;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {PlayerControllerMP.class})
public abstract class MixinPlayerControllerMP implements IPlayerControllerMP {

    @Inject(method = {"getBlockReachDistance"}, at = {@At("RETURN")}, cancellable = true)
    private void getReachDistanceHook(final CallbackInfoReturnable<Float> distance) {
        if (Reach.getInstance().isOn()) {
            final float range = distance.getReturnValue();
            distance.setReturnValue((range + Reach.getInstance().add.getValue()));
        }
    }

    @Inject(method = {"clickBlock"}, at = {@At(value = "HEAD")}, cancellable = true)
    private void clickBlockHook(BlockPos pos, EnumFacing face, CallbackInfoReturnable<Boolean> info) {
        ClickBlockEvent event2 = new ClickBlockEvent(pos, face);
        MinecraftForge.EVENT_BUS.post(event2);
        if (event2.isCanceled()) {
            info.cancel();
        }
    }

    @Override
    @Invoker(value = "syncCurrentPlayItem")
    public abstract void syncItem();

    @Inject(method = "attackEntity", at = @At(value = "HEAD"), cancellable = true)
    public void attackEntityPre(EntityPlayer playerIn, Entity targetEntity, CallbackInfo info) {
        AttackEvent event = new AttackEvent(targetEntity, (short) 0);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled())
            info.cancel();
    }

    @Inject(method = "attackEntity", at = @At(value = "RETURN"), cancellable = true)
    public void attackEntityPost(EntityPlayer playerIn, Entity targetEntity, CallbackInfo info) {
        AttackEvent event = new AttackEvent(targetEntity, (short) 1);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            info.cancel();
    }


    @Inject(method = "processRightClickBlock", at = @At(value = "HEAD"), cancellable = true)
    private void clickBlockHook(EntityPlayerSP player, WorldClient worldIn, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand, CallbackInfoReturnable<EnumActionResult> info) {
        ClickBlockEvent.Right event = new ClickBlockEvent
                .Right(pos, direction, vec, hand);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(
            method = "resetBlockRemoving",
            at = @At("HEAD"),
            cancellable = true)
    public void resetBlockRemovingHook(CallbackInfo info) {
        ResetBlockEvent event = new ResetBlockEvent();
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            info.cancel();
        }
    }


    @Inject(method = {"onStoppedUsingItem"}, at = {@At(value = "HEAD")}, cancellable = true)
    public void stopHook(CallbackInfo info) {
        StopUsingItemEvent event = new StopUsingItemEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"), cancellable = true)
    private void onPlayerDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        NoGlitchBlock noGlitchBlock = Thunderhack.moduleManager.getModuleByClass(NoGlitchBlock.class);
        if ( noGlitchBlock.isEnabled()) {
            callbackInfoReturnable.cancel();
            callbackInfoReturnable.setReturnValue(false);
        }
        MinecraftForge.EVENT_BUS.post(new DestroyBlockEvent(pos));
    }


    @Inject(method = "onPlayerDamageBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z", at = @At("HEAD"), cancellable = true)
    private void onPlayerDamageBlock(BlockPos posBlock, EnumFacing directionFacing, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        DamageBlockEvent event = new DamageBlockEvent(posBlock, directionFacing);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

}