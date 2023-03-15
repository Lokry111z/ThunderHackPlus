package com.mrzak34.thunderhack.modules.player;


import com.mrzak34.thunderhack.events.FreecamEvent;
import com.mrzak34.thunderhack.events.Render2DEvent;
import com.mrzak34.thunderhack.events.RenderItemOverlayEvent;
import com.mrzak34.thunderhack.gui.fontstuff.FontRender;
import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.setting.Setting;
import com.mrzak34.thunderhack.setting.SubBind;
import com.mrzak34.thunderhack.util.FreecamCamera;
import com.mrzak34.thunderhack.util.PlayerUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class FreeCam extends Module {

    private static FreeCam INSTANCE = new FreeCam();
    public Setting<SubBind> movePlayer = this.register(new Setting<>("Control", new SubBind(Keyboard.KEY_LMENU)));
    private final Setting<Float> hSpeed = this.register(new Setting<>("HSpeed", 1.0f, 0.2f, 2.0f));
    private final Setting<Float> vSpeed = this.register(new Setting<>("VSpeed", 1.0f, 0.2f, 2.0f));
    private final Setting<Boolean> follow = register(new Setting<>("Follow", false));
    private final Setting<Boolean> copyInventory = register(new Setting<>("CopyInv", false));
    private Entity cachedActiveEntity = null;
    private int lastActiveTick = -1;
    private Entity oldRenderEntity = null;
    private FreecamCamera camera = null;
    private final MovementInput cameraMovement = new MovementInputFromOptions(mc.gameSettings) {
        @Override
        public void updatePlayerMoveState() {
            if (!PlayerUtils.isKeyDown(movePlayer.getValue().getKey())) {
                super.updatePlayerMoveState();
            } else {
                this.moveStrafe = 0f;
                this.moveForward = 0f;
                this.forwardKeyDown = false;
                this.backKeyDown = false;
                this.leftKeyDown = false;
                this.rightKeyDown = false;
                this.jump = false;
                this.sneak = false;
            }
        }
    };
    private final MovementInput playerMovement = new MovementInputFromOptions(mc.gameSettings) {
        @Override
        public void updatePlayerMoveState() {
            if (PlayerUtils.isKeyDown(movePlayer.getValue().getKey())) {
                super.updatePlayerMoveState();
            } else {
                this.moveStrafe = 0f;
                this.moveForward = 0f;
                this.forwardKeyDown = false;
                this.backKeyDown = false;
                this.leftKeyDown = false;
                this.rightKeyDown = false;
                this.jump = false;
                this.sneak = false;
            }
        }
    };
    public FreeCam() {
        super("FreeCam", "свобоная камера", Category.PLAYER);
        this.setInstance();

    }

    public static FreeCam getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FreeCam();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }

    public Entity getActiveEntity() {
        if (cachedActiveEntity == null) {
            cachedActiveEntity = mc.player;
        }

        int currentTick = mc.player.ticksExisted;
        if (lastActiveTick != currentTick) {
            lastActiveTick = currentTick;

            if (this.isEnabled()) {
                if (PlayerUtils.isKeyDown(movePlayer.getValue().getKey())) {
                    cachedActiveEntity = mc.player;
                } else {
                    cachedActiveEntity = mc.getRenderViewEntity() == null ? mc.player : mc.getRenderViewEntity();
                }
            } else {
                cachedActiveEntity = mc.player;
            }
        }
        return cachedActiveEntity;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Unload event) {
        mc.setRenderViewEntity(mc.player);
        toggle();
    }

    @SubscribeEvent
    public void onFreecam(FreecamEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRender2D(Render2DEvent e) {
        ScaledResolution sr = new ScaledResolution(mc);
        String yCoord = "" + (-Math.round(mc.player.posY - getActiveEntity().posY));

        String str = ".vclip " + yCoord;
        FontRender.drawString6(str, (float) ((sr.getScaledWidth() - FontRender.getStringWidth6(str)) / 1.98), (float) (sr.getScaledHeight() / 1.8 - 20), -1, false);

    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        camera.setCopyInventory(copyInventory.getValue());
        camera.setFollow(follow.getValue());
        camera.sethSpeed(hSpeed.getValue());
        camera.setvSpeed(vSpeed.getValue());
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;

        camera = new FreecamCamera(copyInventory.getValue(), follow.getValue(), hSpeed.getValue(), vSpeed.getValue());
        camera.movementInput = cameraMovement;
        mc.player.movementInput = playerMovement;
        mc.world.addEntityToWorld(-921, camera);
        oldRenderEntity = mc.getRenderViewEntity();
        mc.setRenderViewEntity(camera);
        mc.renderChunksMany = false;
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        if (camera != null) mc.world.removeEntity(camera);
        camera = null;
        mc.player.movementInput = new MovementInputFromOptions(mc.gameSettings);
        mc.setRenderViewEntity(oldRenderEntity);
        mc.renderChunksMany = true;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderItemOverlayEvent event) {
        event.setCanceled(true);
    }
}
