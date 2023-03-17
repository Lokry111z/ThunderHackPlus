package com.mrzak34.thunderhack.mixin.mixins;

import com.mrzak34.thunderhack.Thunderhack;
import com.mrzak34.thunderhack.modules.client.MainSettings;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPlayer.class)
public class MixinPlayerModel extends ModelBiped {
    @Shadow
    public ModelRenderer bipedLeftArmwear;
    @Shadow
    public ModelRenderer bipedRightArmwear;

    public MixinPlayerModel(float p_i1148_1_) {
        super(p_i1148_1_);
    }

    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/model/ModelPlayer;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V")
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (entityIn instanceof EntityPlayerSP && Thunderhack.moduleManager.getModuleByClass(MainSettings.class).eatAnim.getValue()) {
            eatingAnimationRightHand(EnumHand.MAIN_HAND, (EntityPlayerSP) entityIn, ageInTicks);
            eatingAnimationLeftHand(EnumHand.OFF_HAND, (EntityPlayerSP) entityIn, ageInTicks);
        }
    }

    public void eatingAnimationRightHand(EnumHand hand, EntityPlayerSP entity, float ageInTicks) {
        ItemStack itemstack = entity.getHeldItem(hand);
        boolean drinkingoreating = itemstack.getItemUseAction() == EnumAction.EAT || itemstack.getItemUseAction() == EnumAction.DRINK;
        if (entity.getItemInUseCount() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            bipedRightArm.rotateAngleY = -0.5F;
            bipedRightArm.rotateAngleX = -1.3F;
            bipedRightArm.rotateAngleZ = MathHelper.cos(ageInTicks) * 0.1F;
            copyModelAngles2(bipedRightArm, bipedRightArmwear);

            bipedHead.rotateAngleX = MathHelper.cos(ageInTicks) * 0.2F;
            bipedHead.rotateAngleY = bipedHeadwear.rotateAngleY;
            copyModelAngles2(bipedHead, bipedHeadwear);
        }
    }

    public void eatingAnimationLeftHand(EnumHand hand, EntityPlayerSP entity, float ageInTicks) {
        ItemStack itemstack = entity.getHeldItem(hand);
        boolean drinkingoreating = itemstack.getItemUseAction() == EnumAction.EAT || itemstack.getItemUseAction() == EnumAction.DRINK;
        if (entity.getItemInUseCount() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            bipedLeftArm.rotateAngleY = 0.5F;
            bipedLeftArm.rotateAngleX = -1.3F;
            bipedLeftArm.rotateAngleZ = MathHelper.cos(ageInTicks) * 0.1F;
            copyModelAngles2(bipedLeftArm, bipedLeftArmwear);
            bipedHead.rotateAngleX = MathHelper.cos(ageInTicks) * 0.2F;
            bipedHead.rotateAngleY = bipedHeadwear.rotateAngleY;
            copyModelAngles2(bipedHead, bipedHeadwear);
        }
    }

    void copyModelAngles2(ModelRenderer source, ModelRenderer dest) {
        dest.rotateAngleX = source.rotateAngleX;
        dest.rotateAngleY = source.rotateAngleY;
        dest.rotateAngleZ = source.rotateAngleZ;
        dest.rotationPointX = source.rotationPointX;
        dest.rotationPointY = source.rotationPointY;
        dest.rotationPointZ = source.rotationPointZ;
    }


}