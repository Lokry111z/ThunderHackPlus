package com.mrzak34.thunderhack.modules.movement;


import com.mrzak34.thunderhack.events.EventMove;
import com.mrzak34.thunderhack.events.EventPostSync;
import com.mrzak34.thunderhack.events.EventSync;
import com.mrzak34.thunderhack.events.Render3DEvent;
import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.setting.ColorSetting;
import com.mrzak34.thunderhack.setting.Setting;
import com.mrzak34.thunderhack.util.Timer;
import com.mrzak34.thunderhack.util.render.RenderUtil;
import com.mrzak34.thunderhack.util.surround.BlockPosWithFacing;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RusherScaffold extends Module {

    public final Setting<ColorSetting> Color2 = this.register(new Setting<>("Color", new ColorSetting(0x8800FF00)));
    private final Setting<Float> lineWidth = this.register(new Setting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    public Setting<Boolean> rotate = this.register(new Setting<>("Rotate", true));
    public Setting<Boolean> allowShift = this.register(new Setting<>("AllowShift", false));
    public Setting<Boolean> autoswap = this.register(new Setting<>("AutoSwap", true));
    public Setting<Boolean> tower = this.register(new Setting<>("Tower", true));
    public Setting<Boolean> safewalk = this.register(new Setting<>("SafeWalk", true));
    public Setting<Boolean> echestholding = this.register(new Setting<>("EchestHolding", false));
    public Setting<Boolean> render = this.register(new Setting<>("Render", true));

    private final Timer timer = new Timer();
    private BlockPosWithFacing currentblock;

    public RusherScaffold() {
        super("Scaffold", "лучший скафф", Module.Category.PLAYER);
    }

    private boolean isBlockValid(Block block) {
        return block.getDefaultState().getMaterial().isSolid();
    }

    private BlockPosWithFacing checkNearBlocks(BlockPos blockPos) {
        if (isBlockValid(mc.world.getBlockState(blockPos.add(0, -1, 0)).getBlock()))
            return new BlockPosWithFacing(blockPos.add(0, -1, 0), EnumFacing.UP);
        else if (isBlockValid(mc.world.getBlockState(blockPos.add(-1, 0, 0)).getBlock()))
            return new BlockPosWithFacing(blockPos.add(-1, 0, 0), EnumFacing.EAST);
        else if (isBlockValid(mc.world.getBlockState(blockPos.add(1, 0, 0)).getBlock()))
            return new BlockPosWithFacing(blockPos.add(1, 0, 0), EnumFacing.WEST);
        else if (isBlockValid(mc.world.getBlockState(blockPos.add(0, 0, 1)).getBlock()))
            return new BlockPosWithFacing(blockPos.add(0, 0, 1), EnumFacing.NORTH);
        else if (isBlockValid(mc.world.getBlockState(blockPos.add(0, 0, -1)).getBlock()))
            return new BlockPosWithFacing(blockPos.add(0, 0, -1), EnumFacing.SOUTH);
        return null;
    }

    private int findBlockToPlace() {
        if (mc.player.getHeldItemMainhand().getItem() instanceof ItemBlock) {
            if (isBlockValid(((ItemBlock) mc.player.getHeldItemMainhand().getItem()).getBlock()))
                return mc.player.inventory.currentItem;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getCount() != 0) {
                if (mc.player.inventory.getStackInSlot(i).getItem() instanceof ItemBlock) {
                    if (!echestholding.getValue() || (echestholding.getValue() && !mc.player.inventory.getStackInSlot(i).getItem().equals(Item.getItemFromBlock(Blocks.ENDER_CHEST)))) {
                        if (isBlockValid(((ItemBlock) mc.player.inventory.getStackInSlot(i).getItem()).getBlock()))
                            return i;
                    }
                }
            }
        }
        return -1;
    }

    private BlockPosWithFacing checkNearBlocksExtended(BlockPos blockPos) {
        BlockPosWithFacing ret = null;

        ret = checkNearBlocks(blockPos);
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(-1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, 1));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, -1));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(-2, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(2, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, 2));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, -2));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, -1, 0));
        BlockPos blockPos2 = blockPos.add(0, -1, 0);

        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(-1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(0, 0, 1));
        if (ret != null) return ret;

        return checkNearBlocks(blockPos2.add(0, 0, -1));
    }

    private int countValidBlocks() {
        int n = 36;
        int n2 = 0;

        while (n < 45) {

            if (mc.player.inventoryContainer.getSlot(n).getHasStack()) {
                ItemStack itemStack = mc.player.inventoryContainer.getSlot(n).getStack();
                if (itemStack.getItem() instanceof ItemBlock) {
                    if (isBlockValid(((ItemBlock) itemStack.getItem()).getBlock()))
                        n2 += itemStack.getCount();
                }
            }

            n++;
        }

        return n2;
    }

    private Vec3d getEyePosition() {
        return new Vec3d(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ);
    }

    private float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        Vec3d vec3d = new Vec3d((double) blockPos.getX() + 0.5, mc.world.getBlockState(blockPos).getSelectedBoundingBox(mc.world, blockPos).maxY - 0.01, (double) blockPos.getZ() + 0.5);
        vec3d = vec3d.add(new Vec3d(enumFacing.getDirectionVec()).scale(0.5));

        Vec3d vec3d2 = getEyePosition();

        double d = vec3d.x - vec3d2.x;
        double d2 = vec3d.y - vec3d2.y;
        double d3 = vec3d.z - vec3d2.z;
        double d6 = Math.sqrt(d * d + d3 * d3);

        float f = (float) (Math.toDegrees(Math.atan2(d3, d)) - 90.0f);
        float f2 = (float) (-Math.toDegrees(Math.atan2(d2, d6)));

        float[] ret = new float[2];
        ret[0] = mc.player.rotationYaw + MathHelper.wrapDegrees(f - mc.player.rotationYaw);
        ret[1] = mc.player.rotationPitch + MathHelper.wrapDegrees(f2 - mc.player.rotationPitch);

        return ret;
    }

    private void doSafeWalk(EventMove event) {
        double x = event.get_x();
        double y = event.get_y();
        double z = event.get_z();

        if (mc.player.onGround && !mc.player.noClip) {
            double increment;
            for (increment = 0.05D; x != 0.0D && isOffsetBBEmpty(x, 0.0D); ) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
            }
            while (z != 0.0D && isOffsetBBEmpty(0.0D, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
            while (x != 0.0D && z != 0.0D && isOffsetBBEmpty(x, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
        }
        event.set_x(x);
        event.set_y(y);
        event.set_z(z);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMove(EventMove event) {
        if (fullNullCheck()) return;

        if (safewalk.getValue())
            doSafeWalk(event);
    }

    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (render.getValue() && currentblock != null) {
            GlStateManager.pushMatrix();
            RenderUtil.drawBlockOutline(currentblock.getPosition(), Color2.getValue().getColorObject(), lineWidth.getValue(), false, 0);
            GlStateManager.popMatrix();
        }
    }

    private boolean isOffsetBBEmpty(double x, double z) {
        return mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset(x, -2, z)).isEmpty();
    }

    @SubscribeEvent
    public void onPre(EventSync event) {
        if (countValidBlocks() <= 0) {
            currentblock = null;
            return;
        }
        if (mc.player.posY < 257d) {
            currentblock = null;

            if (mc.player.isSneaking() && !allowShift.getValue()) return;

            int n2 = findBlockToPlace();
            if (n2 == -1) return;

            Item item = mc.player.inventory.getStackInSlot(n2).getItem();
            if (!(item instanceof ItemBlock)) return;
            Block block = ((ItemBlock) item).getBlock();

            boolean fullBlock = block.getDefaultState().isFullBlock();

            BlockPos blockPos2 = new BlockPos(mc.player.posX, mc.player.posY - (fullBlock ? 1.0 : 0.01), mc.player.posZ);
            if (!mc.world.getBlockState(blockPos2).getMaterial().isReplaceable()) return;

            currentblock = checkNearBlocksExtended(blockPos2);
            if (currentblock != null) {
                if (rotate.getValue()) {
                    float[] rotations = getRotations(currentblock.getPosition(), currentblock.getFacing());
                    mc.player.rotationYaw = rotations[0];
                    mc.player.renderYawOffset = rotations[0];
                    mc.player.rotationPitch = rotations[1];
                }
            }
        }
    }

    @SubscribeEvent
    public void onPost(EventPostSync e) {
        if (currentblock == null) return;
        int prev_item = mc.player.inventory.currentItem;
        if (!(mc.player.getHeldItemMainhand().getItem() instanceof ItemBlock)) {
            if (autoswap.getValue()) {
                int blockSlot = findBlockToPlace();
                if (blockSlot != -1) {
                    mc.player.inventory.currentItem = blockSlot;
                    mc.player.connection.sendPacket(new CPacketHeldItemChange(blockSlot));
                }
            }
        }
        if ((mc.player.getHeldItemMainhand().getItem() instanceof ItemBlock) && isBlockValid(((ItemBlock) mc.player.getHeldItemMainhand().getItem()).getBlock())) {
            if (!mc.player.movementInput.jump || mc.player.moveForward != 0.0f || mc.player.moveStrafing != 0.0f || !tower.getValue()) {
                timer.reset();
            } else {
                mc.player.setVelocity(0.0, 0.42, 0.0);
                if (timer.passedMs(1500)) {
                    mc.player.motionY = -0.28;
                    timer.reset();
                }
            }

            boolean sneak = mc.world.getBlockState(currentblock.getPosition()).getBlock().onBlockActivated(mc.world, currentblock.getPosition(), mc.world.getBlockState(currentblock.getPosition()), mc.player, EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.0f, 0.0f, 0.0f);
            if (sneak)
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
            mc.playerController.processRightClickBlock(
                    mc.player,
                    mc.world,
                    currentblock.getPosition(),
                    this.currentblock.getFacing(),
                    new Vec3d(
                            (double) currentblock.getPosition().getX() + Math.random(),
                            mc.world.getBlockState(currentblock.getPosition()).getSelectedBoundingBox(mc.world, currentblock.getPosition()).maxY - 0.01,
                            (double) currentblock.getPosition().getZ() + Math.random()
                    ),
                    EnumHand.MAIN_HAND);

            mc.player.swingArm(EnumHand.MAIN_HAND);
            if (sneak)
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
            mc.player.inventory.currentItem = prev_item;
            mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem));
        }
    }
}
