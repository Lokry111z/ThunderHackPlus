package com.mrzak34.thunderhack.modules.combat;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.mrzak34.thunderhack.Thunderhack;
import com.mrzak34.thunderhack.command.Command;
import com.mrzak34.thunderhack.events.*;
import com.mrzak34.thunderhack.gui.fontstuff.FontRender;
import com.mrzak34.thunderhack.mixin.ducks.ISPacketSpawnObject;
import com.mrzak34.thunderhack.mixin.mixins.IEntityRenderer;
import com.mrzak34.thunderhack.mixin.mixins.ISPacketEntity;
import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.modules.movement.PacketFly;
import com.mrzak34.thunderhack.setting.ColorSetting;
import com.mrzak34.thunderhack.setting.Setting;
import com.mrzak34.thunderhack.setting.SubBind;
import com.mrzak34.thunderhack.util.EntityUtil;
import com.mrzak34.thunderhack.util.InventoryUtil;
import com.mrzak34.thunderhack.util.TessellatorUtil;
import com.mrzak34.thunderhack.util.Timer;
import com.mrzak34.thunderhack.util.math.MathUtil;
import com.mrzak34.thunderhack.util.phobos.*;
import com.mrzak34.thunderhack.util.render.BlockRenderUtil;
import com.mrzak34.thunderhack.util.render.RenderUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MouseFilter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mrzak34.thunderhack.modules.render.Trajectories.*;
import static com.mrzak34.thunderhack.util.EntityUtil.interpolateEntity;
import static com.mrzak34.thunderhack.util.RotationUtil.getRotationPlayer;
import static com.mrzak34.thunderhack.util.phobos.RotationUtil.getAngle;
import static com.mrzak34.thunderhack.util.phobos.RotationUtil.inFov;
import static com.mrzak34.thunderhack.util.phobos.ServerTimeHelper.*;
import static net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting;
import static net.minecraft.util.EnumFacing.HORIZONTALS;
import static org.lwjgl.opengl.GL11.*;


public class AutoCrystal extends Module {
    public static final PositionHistoryHelper POSITION_HISTORY = new PositionHistoryHelper();

    /*

     * This file is part of the 3arthh4ck distribution (https://github.com/3arthqu4ke/3arthh4ck).
     * Copyright (c) 3arthh4ck Development.
     * 3arthqu4ke Gerald Megyn Ohare

     */
    private static final ScheduledExecutorService EXECUTOR = ThreadUtil.newDaemonScheduledExecutor("AutoCrystal");
    private static final AtomicBoolean ATOMIC_STARTED = new AtomicBoolean();
    public static Timer timercheckerfg = new Timer();
    public static Timer timercheckerwfg = new Timer();
    public static boolean psdead = true;
    private static boolean started;

    static {
        MinecraftForge.EVENT_BUS.register(POSITION_HISTORY);
    }


    /* ---------------- Fields -------------- */
    public final Map<BlockPos, CrystalTimeStamp> placed = new ConcurrentHashMap<>();
    public final HelperSequential sequentialHelper = new HelperSequential(this);
    public final IDHelper idHelper = new IDHelper();
    public final HelperRotation rotationHelper = new HelperRotation(this);
    public final HelperRange rangeHelper = new HelperRange(this);
    private final MouseFilter pitchMouseFilter = new MouseFilter();
    private final MouseFilter yawMouseFilter = new MouseFilter();
    private final Map<BlockPos, Long> fadeList = new HashMap<>();
    public Setting<settingtypeEn> settingType = register(new Setting<>("Settings", settingtypeEn.Noob));
    public Setting<pages> page = register(new Setting<>("Page", pages.Place));
    /* ---------------- Extrapolation Settings -------------- */
    public final Setting<Integer> extrapol = register(new Setting<>("Extrapolation", 0, 0, 50, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Integer> bExtrapol = register(new Setting<>("Break-Extrapolation", 0, 0, 50, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Integer> blockExtrapol = register(new Setting<>("Block-Extrapolation", 0, 0, 50, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<BlockExtrapolationMode> blockExtraMode = register(new Setting<>("BlockExtraMode", BlockExtrapolationMode.Pessimistic, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Boolean> doubleExtraCheck = register(new Setting<Boolean>("DoubleExtraCheck", true, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Boolean> avgPlaceDamage = register(new Setting<Boolean>("AvgPlaceExtra", false, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> placeExtraWeight = register(new Setting<>("P-Extra-Weight", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Double> placeNormalWeight = register(new Setting<>("P-Norm-Weight", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Boolean> avgBreakExtra = register(new Setting<Boolean>("AvgBreakExtra", false, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> breakExtraWeight = register(new Setting<>("B-Extra-Weight", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Double> breakNormalWeight = register(new Setting<>("B-Norm-Weight", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Boolean> gravityExtrapolation = register(new Setting<Boolean>("Extra-Gravity", true, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> gravityFactor = register(new Setting<>("Gravity-Factor", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> yPlusFactor = register(new Setting<>("Y-Plus-Factor", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> yMinusFactor = register(new Setting<>("Y-Minus-Factor", 1.0, 0.0, 5.0, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Boolean> selfExtrapolation = register(new Setting<Boolean>("SelfExtrapolation", false, v -> page.getValue() == pages.Extrapolation && settingType.getValue() == settingtypeEn.Pro));
    public final Setting<Boolean> useSafetyFactor = register(new Setting<Boolean>("UseSafetyFactor", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> selfFactor = register(new Setting<>("SelfFactor", 1.0, 0.0, 10.0, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> safetyFactor = register(new Setting<>("SafetyFactor", 1.0, 0.0, 10.0, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Double> compareDiff = register(new Setting<>("CompareDiff", 1.0, 0.0, 10.0, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public final Setting<Boolean> facePlaceCompare = register(new Setting<Boolean>("FacePlaceCompare", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Place Settings -------------- */
    public Setting<Boolean> place = register(new Setting<Boolean>("Place", true, v -> page.getValue() == pages.Place));
    public Setting<Target> targetMode = register(new Setting<>("Target", Target.Closest, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> placeRange = register(new Setting<>("PlaceRange", 6.0f, 0.0f, 6.0f, v -> page.getValue() == pages.Place));
    public Setting<Float> placeTrace = register(new Setting<>("PlaceTrace", 6.0f, 0.0f, 6.0f, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> minDamage = register(new Setting<>("MinDamage", 6.0f, 0.1f, 20.0f, v -> page.getValue() == pages.Place));
    public Setting<Integer> placeDelay = register(new Setting<>("PlaceDelay", 25, 0, 500, v -> page.getValue() == pages.Place));
    /* ---------------- Timers -------------- */
    public DiscreteTimer placeTimer = new GuardTimer(1000, 5).reset(placeDelay.getValue());
    public Setting<Float> maxSelfPlace = register(new Setting<>("MaxSelfPlace", 9.0f, 0.0f, 20.0f, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> multiPlace = register(new Setting<>("MultiPlace", 1, 1, 5, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> slowPlaceDmg = register(new Setting<>("SlowPlace", 4.0f, 0.1f, 20.0f, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> slowPlaceDelay = register(new Setting<>("SlowPlaceDelay", 500, 0, 500, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> override = register(new Setting<Boolean>("OverridePlace", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> newVer = register(new Setting<Boolean>("1.13+", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> newVerEntities = register(new Setting<Boolean>("1.13-Entities", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<SwingTime> placeSwing = register(new Setting<>("PlaceSwing", SwingTime.Post, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> smartTrace = register(new Setting<Boolean>("Smart-Trace", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> placeRangeEyes = register(new Setting<Boolean>("PlaceRangeEyes", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> placeRangeCenter = register(new Setting<Boolean>("PlaceRangeCenter", true, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> traceWidth = register(new Setting<>("TraceWidth", -1.0, -1.0, 1.0, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> fallbackTrace = register(new Setting<Boolean>("Fallback-Trace", true, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> rayTraceBypass = register(new Setting<Boolean>("RayTraceBypass", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> forceBypass = register(new Setting<Boolean>("ForceBypass", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> rayBypassFacePlace = register(new Setting<Boolean>("RayBypassFacePlace", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> rayBypassFallback = register(new Setting<Boolean>("RayBypassFallback", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> bypassTicks = register(new Setting<>("BypassTicks", 10, 0, 20, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> rbYaw = register(new Setting<>("RB-Yaw", 180.0f, 0.0f, 180.0f, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> rbPitch = register(new Setting<>("RB-Pitch", 90.0f, 0.0f, 90.0f, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> bypassRotationTime = register(new Setting<>("RayBypassRotationTime", 500, 0, 1000, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> ignoreNonFull = register(new Setting<Boolean>("IgnoreNonFull", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> efficientPlacements = register(new Setting<Boolean>("EfficientPlacements", false, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> simulatePlace = register(new Setting<>("Simulate-Place", 0, 0, 10, v -> page.getValue() == pages.Place && settingType.getValue() == settingtypeEn.Pro));
    public FakeCrystalRender crystalRender = new FakeCrystalRender(simulatePlace);
    /* ---------------- Break Settings -------------- */
    public Setting<Attack2> attackMode = register(new Setting<>("Attack", Attack2.Crystal, v -> page.getValue() == pages.Break));
    public Setting<Boolean> attack = register(new Setting<Boolean>("Break", true, v -> page.getValue() == pages.Break));
    public Setting<Float> breakRange = register(new Setting<>("BreakRange", 6.0f, 0.0f, 6.0f, v -> page.getValue() == pages.Break));
    public Setting<Integer> breakDelay = register(new Setting<>("BreakDelay", 25, 0, 500, v -> page.getValue() == pages.Break));
    public DiscreteTimer breakTimer = new GuardTimer(1000, 5).reset(breakDelay.getValue());
    public Setting<Float> breakTrace = register(new Setting<>("BreakTrace", 3.0f, 0.0f, 6.0f, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> minBreakDamage = register(new Setting<>("MinBreakDmg", 0.5f, 0.0f, 20.0f, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> maxSelfBreak = register(new Setting<>("MaxSelfBreak", 10.0f, 0.0f, 20.0f, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> slowBreakDamage = register(new Setting<>("SlowBreak", 3.0f, 0.1f, 20.0f, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> slowBreakDelay = register(new Setting<>("SlowBreakDelay", 500, 0, 500, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> instant = register(new Setting<Boolean>("Instant", false, v -> page.getValue() == pages.Break));
    public Setting<Boolean> asyncCalc = register(new Setting<Boolean>("Async-Calc", false, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> alwaysCalc = register(new Setting<Boolean>("Always-Calc", false, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> ncpRange = register(new Setting<Boolean>("NCP-Range", false, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<SmartRange> placeBreakRange = register(new Setting<>("SmartRange", SmartRange.None, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> smartTicks = register(new Setting<>("SmartRange-Ticks", 0, 0, 20, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> negativeTicks = register(new Setting<>("Negative-Ticks", 0, 0, 20, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> smartBreakTrace = register(new Setting<Boolean>("SmartBreakTrace", true, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> negativeBreakTrace = register(new Setting<Boolean>("NegativeBreakTrace", true, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> packets = register(new Setting<>("Packets", 1, 1, 5, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> overrideBreak = register(new Setting<Boolean>("OverrideBreak", false, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<AntiWeakness> antiWeakness = register(new Setting<>("AntiWeakness", AntiWeakness.None, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> instantAntiWeak = register(new Setting<Boolean>("AW-Instant", true, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> efficient = register(new Setting<Boolean>("Efficient", true, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> manually = register(new Setting<Boolean>("Manually", true, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> manualDelay = register(new Setting<>("ManualDelay", 500, 0, 500, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<SwingTime> breakSwing = register(new Setting<>("BreakSwing", SwingTime.Post, v -> page.getValue() == pages.Break && settingType.getValue() == settingtypeEn.Hacker));
    /* --------------- Rotations -------------- */
    public Setting<ACRotate> rotate = register(new Setting<>("Rotate", ACRotate.None, v -> page.getValue() == pages.Rotations));
    public Setting<RotateMode> rotateMode = register(new Setting<>("Rotate-Mode", RotateMode.Normal, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> smoothSpeed = register(new Setting<>("Smooth-Speed", 0.5f, 0.1f, 2.0f, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> endRotations = register(new Setting<>("End-Rotations", 250, 0, 1000, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> angle = register(new Setting<>("Break-Angle", 180.0f, 0.1f, 180.0f, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> placeAngle = register(new Setting<>("Place-Angle", 180.0f, 0.1f, 180.0f, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> height = register(new Setting<>("Height", 0.05f, 0.0f, 1.0f, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> placeHeight = register(new Setting<>("Place-Height", 1.0, 0.0, 1.0, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> rotationTicks = register(new Setting<>("Rotations-Existed", 0, 0, 500, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> focusRotations = register(new Setting<Boolean>("Focus-Rotations", false, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> focusAngleCalc = register(new Setting<Boolean>("FocusRotationCompare", false, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> focusExponent = register(new Setting<>("FocusExponent", 0.0, 0.0, 10.0, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> focusDiff = register(new Setting<>("FocusDiff", 0.0, 0.0, 180.0, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> rotationExponent = register(new Setting<>("RotationExponent", 0.0, 0.0, 10.0, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> minRotDiff = register(new Setting<>("MinRotationDiff", 0.0, 0.0, 180.0, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> existed = register(new Setting<>("Existed", 0, 0, 500, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> pingExisted = register(new Setting<Boolean>("Ping-Existed", false, v -> page.getValue() == pages.Rotations && settingType.getValue() == settingtypeEn.Pro));
    /* ---------------- Misc Settings -------------- */
    public Setting<Float> targetRange = register(new Setting<>("TargetRange", 20.0f, 0.1f, 20.0f, v -> page.getValue() == pages.Misc));
    public Setting<Float> pbTrace = register(new Setting<>("CombinedTrace", 3.0f, 0.0f, 6.0f, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> range = register(new Setting<>("Range", 12.0f, 0.1f, 20.0f, v -> page.getValue() == pages.Misc));
    public Setting<Boolean> suicide = register(new Setting<Boolean>("Suicide", false, v -> page.getValue() == pages.Misc));
    public Setting<Boolean> shield = register(new Setting<Boolean>("Shield", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> shieldCount = register(new Setting<>("ShieldCount", 1, 1, 5, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> shieldMinDamage = register(new Setting<>("ShieldMinDamage", 6.0f, 0.0f, 20.0f, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> shieldSelfDamage = register(new Setting<>("ShieldSelfDamage", 2.0f, 0.0f, 20.0f, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> shieldDelay = register(new Setting<>("ShieldPlaceDelay", 50, 0, 5000, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> shieldRange = register(new Setting<>("ShieldRange", 10.0f, 0.0f, 20.0f, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> shieldPrioritizeHealth = register(new Setting<Boolean>("Shield-PrioritizeHealth", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> multiTask = register(new Setting<Boolean>("MultiTask", true, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> multiPlaceCalc = register(new Setting<Boolean>("MultiPlace-Calc", true, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> multiPlaceMinDmg = register(new Setting<Boolean>("MultiPlace-MinDmg", true, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> countDeadCrystals = register(new Setting<Boolean>("CountDeadCrystals", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> countDeathTime = register(new Setting<Boolean>("CountWithinDeathTime", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> yCalc = register(new Setting<Boolean>("Y-Calc", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> dangerSpeed = register(new Setting<Boolean>("Danger-Speed", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> dangerHealth = register(new Setting<>("Danger-Health", 0.0f, 0.0f, 36.0f, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> cooldown = register(new Setting<>("CoolDown", 500, 0, 10000, v -> page.getValue() == pages.Misc));
    public WeaknessHelper weaknessHelper = new WeaknessHelper(antiWeakness, cooldown);
    public Setting<Integer> placeCoolDown = register(new Setting<>("PlaceCooldown", 0, 0, 10000, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<AntiFriendPop> antiFriendPop = register(new Setting<>("AntiFriendPop", AntiFriendPop.None, v -> page.getValue() == pages.Misc));
    public Setting<Boolean> antiFeetPlace = register(new Setting<Boolean>("AntiFeetPlace", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> feetBuffer = register(new Setting<>("FeetBuffer", 5, 0, 50, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    public ServerTimeHelper serverTimeHelper = new ServerTimeHelper(this, rotate, placeSwing, antiFeetPlace, newVer, feetBuffer);
    public Setting<Boolean> stopWhenEating = register(new Setting<Boolean>("StopWhenEating", false, v -> page.getValue() == pages.Misc));
    public Setting<Boolean> stopWhenMining = register(new Setting<Boolean>("StopWhenMining", false, v -> page.getValue() == pages.Misc));
    public Setting<Boolean> dangerFacePlace = register(new Setting<Boolean>("Danger-FacePlace", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> motionCalc = register(new Setting<Boolean>("Motion-Calc", false, v -> page.getValue() == pages.Misc && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- FacePlace and ArmorPlace -------------- */
    public Setting<Boolean> holdFacePlace = register(new Setting<Boolean>("HoldFacePlace", false, v -> page.getValue() == pages.FacePlace));
    public Setting<Float> facePlace = register(new Setting<>("FacePlace", 10.0f, 0.0f, 36.0f, v -> page.getValue() == pages.FacePlace));
    public Setting<Float> minFaceDmg = register(new Setting<>("Min-FP", 2.0f, 0.0f, 5.0f, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> armorPlace = register(new Setting<>("ArmorPlace", 5.0f, 0.0f, 100.0f, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> pickAxeHold = register(new Setting<Boolean>("PickAxe-Hold", false, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> antiNaked = register(new Setting<Boolean>("AntiNaked", false, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> fallBack = register(new Setting<Boolean>("FallBack", true, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> fallBackDiff = register(new Setting<>("Fallback-Difference", 10.0f, 0.0f, 16.0f, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> fallBackDmg = register(new Setting<>("FallBackDmg", 3.0f, 0.0f, 6.0f, v -> page.getValue() == pages.FacePlace && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Switch, Swing -------------- */
    public Setting<AutoSwitch> autoSwitch = register(new Setting<>("AutoSwitch", AutoSwitch.Bind, v -> page.getValue() == pages.SwitchNSwing));
    public Setting<Boolean> mainHand = register(new Setting<Boolean>("MainHand", false, v -> page.getValue() == pages.SwitchNSwing));
    public Setting<SubBind> switchBind = this.register(new Setting<>("SwitchBind", new SubBind(Keyboard.KEY_NONE), v -> page.getValue() == pages.SwitchNSwing));
    public Setting<Boolean> switchBack = register(new Setting<Boolean>("SwitchBack", true, v -> page.getValue() == pages.SwitchNSwing));
    public Setting<Boolean> useAsOffhand = register(new Setting<Boolean>("UseAsOffHandBind", false, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> instantOffhand = register(new Setting<Boolean>("Instant-Offhand", true, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> switchMessage = register(new Setting<Boolean>("Switch-Message", false, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<SwingType> swing = register(new Setting<>("BreakHand", SwingType.MainHand, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<SwingType> placeHand = register(new Setting<>("PlaceHand", SwingType.MainHand, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<CooldownBypass2> cooldownBypass = register(new Setting<>("CooldownBypass", CooldownBypass2.None, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Pro));
    public Setting<CooldownBypass2> obsidianBypass = register(new Setting<>("ObsidianBypass", CooldownBypass2.None, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Pro));
    public Setting<CooldownBypass2> antiWeaknessBypass = register(new Setting<>("AntiWeaknessBypass", CooldownBypass2.None, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Pro));
    public Setting<CooldownBypass2> mineBypass = register(new Setting<>("MineBypass", CooldownBypass2.None, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Pro));
    public Setting<SwingType> obbyHand = register(new Setting<>("ObbyHand", SwingType.MainHand, v -> page.getValue() == pages.SwitchNSwing && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Render Settings -------------- */
    public Setting<Boolean> render = register(new Setting<Boolean>("Render", true, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> renderTime = register(new Setting<>("Render-Time", 600, 0, 5000, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> box = register(new Setting<Boolean>("Draw-Box", true, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> fade = register(new Setting<Boolean>("Fade", true, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> fadeComp = register(new Setting<Boolean>("Fade-Compatibility", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> fadeTime = register(new Setting<>("Fade-Time", 1000, 0, 5000, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> realtime = register(new Setting<Boolean>("Realtime", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> slide = register(new Setting<Boolean>("Slide", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> smoothSlide = register(new Setting<Boolean>("SmoothenSlide", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> slideTime = register(new Setting<>("Slide-Time", 250, 1, 1000, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> zoom = register(new Setting<Boolean>("Zoom", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Double> zoomTime = register(new Setting<>("Zoom-Time", 100.0, 1.0, 1000.0, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Double> zoomOffset = register(new Setting<>("Zoom-Offset", -0.5, -1.0, 1.0, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> multiZoom = register(new Setting<Boolean>("Multi-Zoom", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> renderExtrapolation = register(new Setting<Boolean>("RenderExtrapolation", false, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<RenderDamagePos> renderDamage = register(new Setting<>("DamageRender", RenderDamagePos.None, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    public Setting<RenderDamage> renderMode = register(new Setting<>("DamageMode", RenderDamage.Normal, v -> page.getValue() == pages.Render && settingType.getValue() == settingtypeEn.Pro));
    /* ---------------- SetDead Settings -------------- */
    public Setting<Boolean> setDead = register(new Setting<Boolean>("SetDead", false, v -> page.getValue() == pages.SetDead));
    public Setting<Boolean> instantSetDead = register(new Setting<Boolean>("Instant-Dead", false, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> pseudoSetDead = register(new Setting<Boolean>("Pseudo-Dead", true, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> simulateExplosion = register(new Setting<Boolean>("SimulateExplosion", false, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> soundRemove = register(new Setting<Boolean>("SoundRemove", true, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> useSafeDeathTime = register(new Setting<Boolean>("UseSafeDeathTime", false, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> safeDeathTime = register(new Setting<>("Safe-Death-Time", 0, 0, 500, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> deathTime = register(new Setting<>("Death-Time", 0, 0, 500, v -> page.getValue() == pages.SetDead && settingType.getValue() == settingtypeEn.Pro));
    /* ---------------- Obsidian Settings -------------- */
    public Setting<Boolean> obsidian = register(new Setting<Boolean>("Obsidian", false, v -> page.getValue() == pages.Obsidian));
    public Setting<Boolean> basePlaceOnly = register(new Setting<Boolean>("BasePlaceOnly", false, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> obbySwitch = register(new Setting<Boolean>("Obby-Switch", false, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> obbyDelay = register(new Setting<>("ObbyDelay", 500, 0, 5000, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> obbyCalc = register(new Setting<>("ObbyCalc", 500, 0, 5000, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> helpingBlocks = register(new Setting<>("HelpingBlocks", 1, 0, 5, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> obbyMinDmg = register(new Setting<>("Obby-MinDamage", 7.0f, 0.1f, 36.0f, v -> page.getValue() == pages.Obsidian));
    public Setting<Boolean> terrainCalc = register(new Setting<Boolean>("TerrainCalc", true, v -> page.getValue() == pages.Obsidian));
    public Setting<Boolean> obbySafety = register(new Setting<Boolean>("ObbySafety", false, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<RayTraceMode> obbyTrace = register(new Setting<>("Obby-Raytrace", RayTraceMode.Fast, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> obbyTerrain = register(new Setting<Boolean>("Obby-Terrain", true, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> obbyPreSelf = register(new Setting<Boolean>("Obby-PreSelf", true, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> fastObby = register(new Setting<>("Fast-Obby", 0, 0, 3, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> maxDiff = register(new Setting<>("Max-Difference", 1, 0, 5, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Double> maxDmgDiff = register(new Setting<>("Max-DamageDiff", 0.0, 0.0, 10.0, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> setState = register(new Setting<Boolean>("Client-Blocks", false, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<PlaceSwing> obbySwing = register(new Setting<>("Obby-Swing", PlaceSwing.Once, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> obbyFallback = register(new Setting<Boolean>("Obby-Fallback", false, v -> page.getValue() == pages.Obsidian && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Rotate> obbyRotate = register(new Setting<>("Obby-Rotate", Rotate.None, v -> page.getValue() == pages.Obsidian));
    /* ---------------- Liquids Settings -------------- */
    public Setting<Boolean> interact = register(new Setting<Boolean>("Interact", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> inside = register(new Setting<Boolean>("Inside", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> lava = register(new Setting<Boolean>("Lava", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> water = register(new Setting<Boolean>("Water", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> liquidObby = register(new Setting<Boolean>("LiquidObby", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> liquidRayTrace = register(new Setting<Boolean>("LiquidRayTrace", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> liqDelay = register(new Setting<>("LiquidDelay", 500, 0, 1000, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Rotate> liqRotate = register(new Setting<>("LiquidRotate", Rotate.None, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> pickaxeOnly = register(new Setting<Boolean>("PickaxeOnly", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> interruptSpeedmine = register(new Setting<Boolean>("InterruptSpeedmine", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> setAir = register(new Setting<Boolean>("SetAir", true, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> absorb = register(new Setting<Boolean>("Absorb", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> requireOnGround = register(new Setting<Boolean>("RequireOnGround", true, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> ignoreLavaItems = register(new Setting<Boolean>("IgnoreLavaItems", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> sponges = register(new Setting<Boolean>("Sponges", false, v -> page.getValue() == pages.Liquid && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- AntiTotem Settings -------------- */
    public Setting<Boolean> antiTotem = register(new Setting<Boolean>("AntiTotem", false, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Float> totemHealth = register(new Setting<>("Totem-Health", 1.5f, 0.0f, 10.0f, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public AntiTotemHelper antiTotemHelper = new AntiTotemHelper(totemHealth);
    public Setting<Float> minTotemOffset = register(new Setting<>("Min-Offset", 0.5f, 0.0f, 5.0f, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> maxTotemOffset = register(new Setting<>("Max-Offset", 2.0f, 0.0f, 5.0f, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> popDamage = register(new Setting<>("Pop-Damage", 12.0f, 10.0f, 20.0f, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> totemSync = register(new Setting<Boolean>("TotemSync", true, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> forceAntiTotem = register(new Setting<Boolean>("Force-AntiTotem", false, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> forceSlow = register(new Setting<Boolean>("Force-Slow", false, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> syncForce = register(new Setting<Boolean>("Sync-Force", true, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> dangerForce = register(new Setting<Boolean>("Danger-Force", false, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> forcePlaceConfirm = register(new Setting<>("Force-Place", 100, 0, 500, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> forceBreakConfirm = register(new Setting<>("Force-Break", 100, 0, 500, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> attempts = register(new Setting<>("Attempts", 500, 0, 10000, v -> page.getValue() == pages.AntiTotem && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Damage Sync -------------- */
    public Setting<Boolean> damageSync = register(new Setting<Boolean>("DamageSync", false, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> preSynCheck = register(new Setting<Boolean>("Pre-SyncCheck", false, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> discreteSync = register(new Setting<Boolean>("Discrete-Sync", false, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> dangerSync = register(new Setting<Boolean>("Danger-Sync", false, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> placeConfirm = register(new Setting<>("Place-Confirm", 250, 0, 500, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> breakConfirm = register(new Setting<>("Break-Confirm", 250, 0, 500, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> syncDelay = register(new Setting<>("SyncDelay", 500, 0, 500, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Pro));
    public DamageSyncHelper damageSyncHelper = new DamageSyncHelper(discreteSync, syncDelay, dangerSync);
    public ForceAntiTotemHelper forceHelper = new ForceAntiTotemHelper(discreteSync, syncDelay, forcePlaceConfirm, forceBreakConfirm, dangerForce);
    public Setting<Boolean> surroundSync = register(new Setting<Boolean>("SurroundSync", true, v -> page.getValue() == pages.DamageSync && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Predict Settings -------------- */
    public Setting<Boolean> idPredict = register(new Setting<Boolean>("ID-Predict", false, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> idOffset = register(new Setting<>("ID-Offset", 1, 1, 10, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> idDelay = register(new Setting<>("ID-Delay", 0, 0, 500, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> idPackets = register(new Setting<>("ID-Packets", 1, 1, 10, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> godAntiTotem = register(new Setting<Boolean>("God-AntiTotem", false, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> holdingCheck = register(new Setting<Boolean>("Holding-Check", true, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> toolCheck = register(new Setting<Boolean>("Tool-Check", true, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<PlaceSwing> godSwing = register(new Setting<>("God-Swing", PlaceSwing.Once, v -> page.getValue() == pages.Predict && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- Efficiency -------------- */
    public Setting<PreCalc> preCalc = register(new Setting<>("Pre-Calc", PreCalc.None, v -> page.getValue() == pages.Efficiency && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<ExtrapolationType> preCalcExtra = register(new Setting<>("PreCalcExtra", ExtrapolationType.Place, v -> page.getValue() == pages.Efficiency && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> preCalcDamage = register(new Setting<>("Pre-CalcDamage", 15.0f, 0.0f, 36.0f, v -> page.getValue() == pages.Efficiency && settingType.getValue() == settingtypeEn.Hacker));
    /* ---------------- MultiThreading -------------- */
    public Setting<Boolean> multiThread = register(new Setting<Boolean>("MultiThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> smartPost = register(new Setting<Boolean>("Smart-Post", true, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> mainThreadThreads = register(new Setting<Boolean>("MainThreadThreads", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<RotationThread> rotationThread = register(new Setting<>("RotationThread", RotationThread.Predict, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Float> partial = register(new Setting<>("Partial", 0.8f, 0.0f, 1.0f, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> maxCancel = register(new Setting<>("MaxCancel", 10, 1, 50, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public RotationCanceller rotationCanceller = new RotationCanceller(this, maxCancel);
    public Setting<Integer> timeOut = register(new Setting<>("Wait", 2, 1, 10, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> blockDestroyThread = register(new Setting<Boolean>("BlockDestroyThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Integer> threadDelay = register(new Setting<>("ThreadDelay", 25, 0, 100, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public ThreadHelper threadHelper = new ThreadHelper(this, multiThread, mainThreadThreads, threadDelay, rotationThread, rotate);
    public Setting<Integer> tickThreshold = register(new Setting<>("TickThreshold", 5, 1, 20, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> preSpawn = register(new Setting<>("PreSpawn", 3, 1, 20, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> maxEarlyThread = register(new Setting<>("MaxEarlyThread", 8, 1, 20, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> pullBasedDelay = register(new Setting<>("PullBasedDelay", 0, 0, 1000, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> explosionThread = register(new Setting<Boolean>("ExplosionThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> soundThread = register(new Setting<Boolean>("SoundThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> entityThread = register(new Setting<Boolean>("EntityThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> spawnThread = register(new Setting<Boolean>("SpawnThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> spawnThreadWhenAttacked = register(new Setting<Boolean>("SpawnThreadWhenAttacked", true, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> destroyThread = register(new Setting<Boolean>("DestroyThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> serverThread = register(new Setting<Boolean>("ServerThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> gameloop = register(new Setting<Boolean>("Gameloop", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> asyncServerThread = register(new Setting<Boolean>("AsyncServerThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> earlyFeetThread = register(new Setting<Boolean>("EarlyFeetThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> lateBreakThread = register(new Setting<Boolean>("LateBreakThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> motionThread = register(new Setting<Boolean>("MotionThread", true, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    public Setting<Boolean> blockChangeThread = register(new Setting<Boolean>("BlockChangeThread", false, v -> page.getValue() == pages.MultiThreading && settingType.getValue() == settingtypeEn.Pro));
    /* ---------------- Dev and Debugging -------------- */
    public Setting<Integer> priority = register(new Setting<>("Priority", 1500, Integer.MIN_VALUE, Integer.MAX_VALUE, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> spectator = register(new Setting<Boolean>("Spectator", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> noPacketFlyRotationChecks = register(new Setting<Boolean>("NoPacketFlyRotationChecks", true, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> clearPost = register(new Setting<Boolean>("ClearPost", true, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> sequential = register(new Setting<Boolean>("Sequential", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> seqTime = register(new Setting<>("Seq-Time", 250, 0, 1000, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> endSequenceOnSpawn = register(new Setting<Boolean>("EndSequenceOnSpawn", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> endSequenceOnBreak = register(new Setting<Boolean>("EndSequenceOnBreak", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> endSequenceOnExplosion = register(new Setting<Boolean>("EndSequenceOnExplosion", true, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> antiPlaceFail = register(new Setting<Boolean>("AntiPlaceFail", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Boolean> debugAntiPlaceFail = register(new Setting<Boolean>("DebugAntiPlaceFail", false, v -> page.getValue() == pages.Dev)); //DEV
    public Setting<Boolean> alwaysBomb = register(new Setting<Boolean>("Always-Bomb", false, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    public Setting<Integer> removeTime = register(new Setting<>("Remove-Time", 1000, 0, 2500, v -> page.getValue() == pages.Dev && settingType.getValue() == settingtypeEn.Hacker));
    /* ----------------  BLYA ---------------*/
    public final Setting<ColorSetting> boxColor = this.register(new Setting<>("Box", new ColorSetting(0x50bf40bf)));
    public final Setting<ColorSetting> outLine = this.register(new Setting<>("Outline", new ColorSetting(0x50bf40bf)));
    public final Setting<ColorSetting> indicatorColor = this.register(new Setting<>("IndicatorColor", new ColorSetting(0x50bf40bf)));

    public ListenerSound soundObserver = new ListenerSound(this);
    public AtomicInteger motionID = new AtomicInteger();
    public Timer renderTimer = new Timer();
    public Timer bypassTimer = new Timer();
    public Timer obbyTimer = new Timer();
    public Timer obbyCalcTimer = new Timer();
    public Timer targetTimer = new Timer();
    public Timer cTargetTimer = new Timer();
    public Timer forceTimer = new Timer();
    public Timer liquidTimer = new Timer();
    public Timer shieldTimer = new Timer();
    public Timer slideTimer = new Timer();
    public Timer zoomTimer = new Timer();
    public Timer pullTimer = new Timer();
    /* ---------------- States -------------- */
    public Queue<Runnable> post = new ConcurrentLinkedQueue<>();
    public volatile RotationFunction rotation;
    public BlockPos bombPos;
    public EntityPlayer target;
    public Entity crystal;
    public Entity focus;
    public BlockPos renderPos;
    public BlockPos slidePos;
    public boolean switching;
    public boolean isSpoofing;
    public boolean noGod;
    public String damage;
    /* ---------------- Helpers -------------- */
    public ExtrapolationHelper extrapolationHelper = new ExtrapolationHelper(this);
    public HelperLiquids liquidHelper = new HelperLiquids(this);
    public HelperPlace placeHelper = new HelperPlace(this);
    public HelperBreak breakHelper = new HelperBreak(this);
    public HelperObby obbyHelper = new HelperObby(this);
    public HelperBreakMotion breakHelperMotion = new HelperBreakMotion(this);
    public HelperEntityBlocksPlace bbBlockingHelper = new HelperEntityBlocksPlace(this);
    private BlockPos bypassPos;
    private float forward = 0.004f;
    public DamageHelper damageHelper = new DamageHelper(this, extrapolationHelper, terrainCalc, extrapol, bExtrapol, selfExtrapolation, obbyTerrain);


    public AutoCrystal() {
        super("AutoCrystal", "ставит и ломает кристалы", "do you really need-an explanation?)", Category.COMBAT);
    }

    public static boolean canBeFeetPlaced(EntityPlayer player, boolean ignoreCrystals, boolean noBoost2) {
        BlockPos origin = player.getPosition().down();
        World world = player.world;

        if (!world.isAreaLoaded(origin, 1)) {
            return false;
        }

        for (EnumFacing face : HORIZONTALS) {
            BlockPos off = origin.offset(face);

            if (!world.isBlockLoaded(off)) {
                continue;
            }

            IBlockState state = world.getBlockState(off);

            if (canPlaceCrystal(off, ignoreCrystals, noBoost2)) {
                return true;
            }

            BlockPos off2 = off.offset(face);

            if (!world.isBlockLoaded(off2)) {
                continue;
            }

            IBlockState stateOff2 = world.getBlockState(off2);

            if (canPlaceCrystal(off2, ignoreCrystals, noBoost2) && state.getBlock() == Blocks.AIR) {
                return true;
            }
        }

        return false;
    }


    public static EntityPlayer getByFov(List<EntityPlayer> players, double maxRange) {
        EntityPlayer closest = null;
        double closestAngle = 360.0;
        for (EntityPlayer player : players) {
            if (!isValid(player, maxRange)) {
                continue;
            }

            double angle = getAngle(player, 1.4);
            if (angle < closestAngle
                    && angle < mc.gameSettings.fovSetting / 2) {
                closest = player;
                closestAngle = angle;
            }
        }

        return closest;
    }

    public static EntityPlayer getByAngle(List<EntityPlayer> players, double maxRange) {
        EntityPlayer closest = null;
        double closestAngle = 360.0;
        double fovHalf = mc.gameSettings.fovSetting / 2.0;

        for (EntityPlayer player : players) {
            if (!isValid(player, maxRange)) {
                continue;
            }

            double angle = getAngle(player, 1.4);

            if (angle < closestAngle && angle < fovHalf) {
                closest = player;
                closestAngle = angle;
            }
        }

        return closest;
    }

    private static boolean isValid(EntityPlayer player, double maxRange) {
        if (player == null || player == mc.player || player.isDead || player.getHealth() <= 0.0F) {
            return false;
        }

        double distanceSq = mc.player.getDistanceSq(player);

        return distanceSq <= maxRange * maxRange;
    }

    private static double getAngle(EntityPlayer player, double height) {
        EntityPlayer localPlayer = mc.player;

        if (localPlayer == null || player == null) {
            return 0.0;
        }

        double xDiff = player.posX - localPlayer.posX;
        double yDiff = (player.posY + height) - (localPlayer.posY + localPlayer.getEyeHeight());
        double zDiff = player.posZ - localPlayer.posZ;

        if (xDiff == 0.0 && zDiff == 0.0) {
            return yDiff > 0 ? 90.0 : -90.0;
        }

        double angle = Math.toDegrees(Math.atan2(zDiff, xDiff)) - 90.0;

        if (angle < 0) {
            angle += 360.0;
        }

        return angle;
    }


    public static AxisAlignedBB interpolatePos(BlockPos pos, float height) {
        if (pos == null) {
            return null;
        }

        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = x1 + 1.0;
        double y2 = y1 + height;
        double z2 = z1 + 1.0;

        y2 = Math.min(y2, 255.0);

        if (x1 > x2) {
            double temp = x1;
            x1 = x2;
            x2 = temp;
        }

        if (y1 > y2) {
            double temp = y1;
            y1 = y2;
            y2 = temp;
        }

        if (z1 > z2) {
            double temp = z1;
            z1 = z2;
            z2 = temp;
        }

        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }


    public boolean isNotCheckingRotations() {
        return noPacketFlyRotationChecks.getValue() && (Thunderhack.moduleManager.getModuleByClass(PacketFly.class).isEnabled());
    }

    @Override
    public void onEnable() {
        resetModule();
        Thunderhack.setDeadManager.addObserver(this.soundObserver);
    }

    @Override
    public void onDisable() {
        Thunderhack.setDeadManager.removeObserver(this.soundObserver);
        resetModule();
    }

    @Override
    public String getDisplayInfo() {
        if (switching) {
            return ChatFormatting.GREEN + "Switching";
        }

        String cps = crys_speed * 2 + " c/s";

        EntityPlayer t = getTarget();
        return t == null ? null : t.getName() + " " + cps;

    }

    public void setRenderPos(BlockPos pos, float damage) {
        setRenderPos(pos, MathUtil.round(damage, 1) + "");
    }

    public void setRenderPos(BlockPos pos, String text) {
        renderTimer.reset();

        if (pos != null) {
            if (!pos.equals(slidePos) && (!smoothSlide.getValue() || slideTimer.passedMs(slideTime.getValue()))) {
                slidePos = renderPos;
                slideTimer.reset();
            }

            if (multiZoom.getValue() || !pos.equals(renderPos)) {
                zoomTimer.reset();
            }
        }

        this.renderPos = pos;
        this.damage = text;
        this.bypassPos = null;
    }


    public BlockPos getRenderPos() {
        if (renderTimer.passedMs(renderTime.getValue())) {
            renderPos = null;
            slidePos = null;
        }

        return renderPos;
    }

    public EntityPlayer getTarget() {
        if (targetTimer.passedMs(600)) {
            target = null;
        }

        return target;
    }

    public void setTarget(EntityPlayer target) {
        this.targetTimer.reset();
        this.target = target;
    }


    public Entity getCrystal() {
        if (cTargetTimer.passedMs(600)) {
            crystal = null;
        }

        return crystal;
    }

    public void setCrystal(Entity crystal) {
        if (focusRotations.getValue() && !noRotateNigga(ACRotate.Break)) {
            focus = crystal;
        }

        this.cTargetTimer.reset();
        this.crystal = crystal;
    }


    public float getMinDamage() {
        boolean isHoldingMouseLeft = Mouse.isButtonDown(0);
        boolean isScreenNotOpen = mc.currentScreen == null;
        boolean isNotUsingPickaxe = !(mc.player.getHeldItemMainhand().getItem() instanceof ItemPickaxe);
        boolean shouldUsePickaxeHold = pickAxeHold.getValue();
        boolean shouldHoldFacePlace = holdFacePlace.getValue();
        boolean shouldDangerFacePlace = dangerFacePlace.getValue();

        if (shouldHoldFacePlace && isScreenNotOpen && isHoldingMouseLeft &&
                (isNotUsingPickaxe || shouldUsePickaxeHold) || shouldDangerFacePlace) {
            return minFaceDmg.getValue();
        } else {
            return minDamage.getValue();
        }
    }



    public void runPost() {
        CollectionUtil.emptyQueue(post);
    }


    public void resetModule() {
        target = null;
        crystal = null;
        renderPos = null;
        slidePos = null;
        rotation = null;
        switching = false;
        bypassPos = null;
        post.clear();
        mc.addScheduledTask(crystalRender::clear);

        try {
            placed.clear();
            threadHelper.resetThreadHelper();
            rotationCanceller.reset();
            antiTotemHelper.setTarget(null);
            antiTotemHelper.setTargetPos(null);
            idHelper.setUpdated(false);
            idHelper.setHighestID(0);
        } catch (Throwable t) // Possible since MultiThread stuff...
        {
            Thunderhack.LOG.error(t);
        }
    }

    public boolean shouldDanger() {
        return dangerSpeed.getValue() && (/*Managers.SAFETY.isSafe()*/ EntityUtil.getHealth(mc.player) < dangerHealth.getValue());
    }


    public void checkExecutor() {
        // we use "started" here cause its faster than the atomic one
        if (!started
                && asyncServerThread.getValue()
                && serverThread.getValue()
                && multiThread.getValue()
                && rotate.getValue() == ACRotate.None) {
            synchronized (AutoCrystal.class) {
                if (!ATOMIC_STARTED.get()) // check again this time volatile
                {
                    startExecutor();
                    ATOMIC_STARTED.set(true);
                    started = true;
                }
            }
        }
    }

    private void startExecutor() {
        // Start Executor
        EXECUTOR.scheduleAtFixedRate((SafeRunnable) this::doExecutorTick, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void doExecutorTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        boolean asyncServer = asyncServerThread.getValue();
        ACRotate rotateMode = rotate.getValue();
        boolean serverThreadEnabled = serverThread.getValue();
        boolean multiThreadEnabled = multiThread.getValue();

        if (asyncServer && rotateMode == ACRotate.None && serverThreadEnabled && multiThreadEnabled) {
            long tickTimeAdjusted = Thunderhack.servtickManager.getTickTimeAdjusted();
            long spawnTime = Thunderhack.servtickManager.getSpawnTime();
            long tickThresholdValue = tickThreshold.getValue();
            long preSpawnValue = preSpawn.getValue();

            boolean isValidTickTime = Thunderhack.servtickManager.valid(
                    (int) tickTimeAdjusted,
                    Thunderhack.servtickManager.normalize((int) (spawnTime - tickThresholdValue)),
                    Thunderhack.servtickManager.normalize((int) (spawnTime - preSpawnValue)));

            if (isValidTickTime) {
                boolean earlyFeet = earlyFeetThread.getValue();
                boolean lateBreak = lateBreakThread.getValue();

                if (!earlyFeet) {
                    threadHelper.startThread();
                } else if (lateBreak) {
                    threadHelper.startThread(true, false);
                }
            } else {
                EntityPlayer closestEnemy = getClosestEnemy();

                if (closestEnemy != null) {
                    boolean newVersion = newVer.getValue();
                    boolean isSemiSafe = isSemiSafe(closestEnemy, true, newVersion);
                    boolean canBeFeetPlaced = canBeFeetPlaced(closestEnemy, true, newVersion);
                    boolean earlyFeet = earlyFeetThread.getValue();
                    long maxEarlyThreadValue = maxEarlyThread.getValue();

                    if (isSemiSafe && canBeFeetPlaced && earlyFeet &&
                            Thunderhack.servtickManager.valid((int) tickTimeAdjusted, 0, (int) maxEarlyThreadValue)) {
                        threadHelper.startThread(false, true);
                    }
                }
            }
        }
    }


    public boolean isSuicideModule() {
        return false;
    }

    public BlockPos getBypassPos() {
        boolean shouldResetBypassPos =
                bypassTimer.passedMs(bypassRotationTime.getValue()) ||
                        !forceBypass.getValue() ||
                        !rayTraceBypass.getValue();

        if (shouldResetBypassPos) {
            bypassPos = null;
        }

        return bypassPos;
    }


    public void setBypassPos(BlockPos pos) {
        bypassTimer.reset();
        this.bypassPos = pos;
    }

    public boolean isEating() {
        ItemStack stack = mc.player.getActiveItemStack();
        return mc.player.isHandActive()
                && !stack.isEmpty()
                && stack.getItem().getItemUseAction(stack) == EnumAction.EAT;
    }

    public boolean isMining() {
        return mc.playerController.getIsHittingBlock();
    }

    public boolean isOutsidePlaceRange(BlockPos pos) {
        EntityPlayer player = mc.player;
        double x = player.posX;
        double y = player.posY + (placeRangeEyes.getValue() ? player.getEyeHeight() : 0);
        double z = player.posZ;
        double distance = placeRangeCenter.getValue() ? pos.distanceSqToCenter(x, y, z) : pos.distanceSq(x, y, z);
        return distance >= MathUtil.square(placeRange.getValue());
    }

    public int getDeathTime() {
        if (useSafeDeathTime.getValue()) {
            return safeDeathTime.getValue();
        }
        if (!pseudoSetDead.getValue() && !setDead.getValue()) {
            return 0;
        }

        return deathTime.getValue();
    }

    public boolean noRotateNigga(ACRotate rotate2) {
        switch (rotate.getValue()) {
            case None:
                return true;
            case All:
                return false;
            case Place:
                return rotate2 == ACRotate.Break || rotate2 == ACRotate.None;
            case Break:
                return rotate2 == ACRotate.Place || rotate2 == ACRotate.None;
        }
        return false;
    }

    public boolean shouldCalcFuckinBitch(AntiFriendPop type) {
        switch (antiFriendPop.getValue()) {
            case None:
                return false;
            case All:
                return true;
            case Break:
                return type == AntiFriendPop.Break;
            case Place:
                return type == AntiFriendPop.Place;
        }
        return false;
    }

    public boolean shouldcalcN() {
        switch (attackMode.getValue()) {
            case Calc:
                return true;
            case Always:
                return true;
            case Crystal:
                return InventoryUtil.isHolding(Items.END_CRYSTAL);
        }
        return true;
    }

    public boolean shouldattackN() {
        switch (attackMode.getValue()) {
            case Calc:
                return InventoryUtil.isHolding(Items.END_CRYSTAL);
            case Always:
                return true;
            case Crystal:
                return InventoryUtil.isHolding(Items.END_CRYSTAL);
        }
        return true;
    }

    public boolean isOutsideBreakRange(double x, double y, double z, AutoCrystal module) {
        switch (placeBreakRange.getValue()) {
            case All:
                return !module.rangeHelper.isCrystalInRange(x, y, z, module.smartTicks.getValue()) && !module.rangeHelper.isCrystalInRange(x, y, z, 0);
            case None:
                return false;
            case Normal:
                return !module.rangeHelper.isCrystalInRange(x, y, z, 0);
            case Extrapolated:
                return !module.rangeHelper.isCrystalInRange(x, y, z, module.smartTicks.getValue());
        }
        return !module.rangeHelper.isCrystalInRange(x, y, z, module.smartTicks.getValue()) && !module.rangeHelper.isCrystalInRange(x, y, z, 0);
    }

    public boolean isOutsideBreakRange(BlockPos pos, AutoCrystal module) {
        return isOutsideBreakRange(pos.getX() + 0.5f, pos.getY() + 1, pos.getZ() + 0.5f, module);
    }

    @SubscribeEvent
    public void onUpdateEntities(UpdateEntitiesEvent e) {
        ExtrapolationHelper.onUpdateEntity(e);
    }

    @Override
    public void onLogin() {
        resetModule();
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        threadHelper.schedulePacket(e);

        if (e.getPacket() instanceof SPacketBlockChange) {
            if (multiThread.getValue() && blockChangeThread.getValue()) {
                SPacketBlockChange packet = e.getPacket();
                if (packet.getBlockState().getBlock() == Blocks.AIR && mc.player.getDistanceSq(packet.getBlockPosition()) < 40) {
                    e.addPostEvent(() -> {
                        if (mc.world != null && HelperUtil.validChange(packet.getBlockPosition(), mc.world.playerEntities)) {
                            threadHelper.startThread();
                        }
                    });
                }
            }
        }
        if (e.getPacket() instanceof SPacketMultiBlockChange) {
            if (multiThread.getValue() && blockChangeThread.getValue()) {
                SPacketMultiBlockChange packet = e.getPacket();
                e.addPostEvent(() ->
                {
                    for (SPacketMultiBlockChange.BlockUpdateData data : packet.getChangedBlocks()) {
                        if (data.getBlockState().getMaterial() == Material.AIR && HelperUtil.validChange(data.getPos(), mc.world.playerEntities)) {
                            threadHelper.startThread();
                            break;
                        }
                    }
                });
            }

        }

        if (e.getPacket() instanceof SPacketDestroyEntities) {
            if (destroyThread.getValue()) {
                threadHelper.schedulePacket(e);
            }
        }
        if (e.getPacket() instanceof SPacketEntity.S15PacketEntityRelMove) {
            onEvent22(e.getPacket());
        }

        if (e.getPacket() instanceof SPacketEntity.S17PacketEntityLookMove) {
            onEvent22(e.getPacket());
        }
        if (e.getPacket() instanceof SPacketExplosion) {
            if (explosionThread.getValue() && !((SPacketExplosion) e.getPacket()).getAffectedBlockPositions().isEmpty()) {
                threadHelper.schedulePacket(e);
            }
        }
        if (e.getPacket() instanceof SPacketPlayerPosLook) {
            rotationCanceller.drop();
        }
        if (e.getPacket() instanceof SPacketSpawnObject) {
            try {
                onEvent33(e);
            } catch (Throwable t)
            {
                Thunderhack.LOG.error(t);
            }
        }
        if (e.getPacket() instanceof SPacketSoundEffect) {
            if (this.soundThread.getValue()) {
                this.threadHelper.startThread();
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof CPacketPlayer) {
            updater228(event);
        }

        if (event.getPacket() instanceof CPacketPlayer.Position) {
            updater228(event);
        }

        if (event.getPacket() instanceof CPacketPlayer.Rotation) {
            updater228(event);
        }

        if (event.getPacket() instanceof CPacketPlayer.PositionRotation) {
            updater228(event);
        }
    }

    @SubscribeEvent
    public void onPacketSendPost(PacketEvent.SendPost event) {
        if (event.getPacket() instanceof CPacketPlayerTryUseItemOnBlock) {
            if (idPredict.getValue()
                    && !noGod
                    && breakTimer.passed(breakDelay.getValue())
                    && mc.player
                    .getHeldItem(((CPacketPlayerTryUseItemOnBlock) event.getPacket()).getHand())
                    .getItem() == Items.END_CRYSTAL
                    && idHelper.isSafe(mc.world.playerEntities,
                    holdingCheck.getValue(),
                    toolCheck.getValue())) {
                idHelper.attack(breakSwing.getValue(),
                        godSwing.getValue(),
                        idOffset.getValue(),
                        idPackets.getValue(),
                        idDelay.getValue());

                breakTimer.reset(breakDelay.getValue());
            }
        }
        if (event.getPacket() instanceof CPacketUseEntity) {
            Entity entity = null; //CPACKET
            if (entity == null) {
                entity = ((CPacketUseEntity) event.getPacket()).getEntityFromWorld(mc.world);
                if (entity == null) {
                    return;
                }
            }
            serverTimeHelper
                    .onUseEntity(event.getPacket(),
                            entity);
        }
    }

    private final Timer inv_timer = new Timer();
    private int prev_crystals_ammount;
    private int crys_speed;

    @Override
    public void onTick() {
        if(inv_timer.passedMs(500)){
            crys_speed = prev_crystals_ammount - InventoryUtil.getItemCount(Items.END_CRYSTAL);
            prev_crystals_ammount = InventoryUtil.getItemCount(Items.END_CRYSTAL);
            inv_timer.reset();
        }

        checkExecutor();
        placed.values().removeIf(stamp ->
                System.currentTimeMillis() - stamp.getTimeStamp()
                        > removeTime.getValue());

        crystalRender.tick();
        if (!idHelper.isUpdated()) {
            idHelper.update();
            idHelper.setUpdated(true);
        }
        weaknessHelper.updateWeakness();
    }



    private void updater228(PacketEvent.Send event) {
        if (multiThread.getValue() && !isSpoofing && rotate.getValue() != ACRotate.None && rotationThread.getValue() == RotationThread.Cancel) {
            rotationCanceller.onPacketNigger(event);
        } else {
            rotationCanceller.reset();
        }
    }

    @SubscribeEvent
    public void onDestroyBlock(DestroyBlockEvent event) {
        if (blockDestroyThread.getValue()
                && multiThread.getValue()
                && !event.isCanceled()
                && HelperUtil.validChange(event.getBlockPos(), mc.world.playerEntities)) {
            threadHelper.startThread(event.getBlockPos().down());
        }
    }

    @SubscribeEvent
    public void onGameZaloop(GameZaloopEvent event) {
        rotationCanceller.onGameLoop();
        if (!multiThread.getValue()) {
            return;
        }

        if (gameloop.getValue()) {
            threadHelper.startThread();
        } else if (rotate.getValue() != ACRotate.None && rotationThread.getValue() == RotationThread.Predict && mc.getRenderPartialTicks() >= partial.getValue()) {
            threadHelper.startThread();
        } else if (rotate.getValue() == ACRotate.None
                && serverThread.getValue()
                && mc.world != null
                && mc.player != null) {
            if (Thunderhack.servtickManager.valid(
                    Thunderhack.servtickManager.getTickTimeAdjusted(),
                    Thunderhack.servtickManager.normalize(Thunderhack.servtickManager.getSpawnTime()
                            - tickThreshold.getValue()),
                    Thunderhack.servtickManager.normalize(Thunderhack.servtickManager.getSpawnTime()
                            - preSpawn.getValue()))) {
                if (!earlyFeetThread.getValue()) {
                    threadHelper.startThread();
                } else if (lateBreakThread.getValue()) {
                    threadHelper.startThread(true, false);
                }
            } else if (getClosestEnemy() != null
                    && isSemiSafe(getClosestEnemy(), true, newVer.getValue())
                    && canBeFeetPlaced(getClosestEnemy(), true, newVer.getValue()) // temp and hacky
                    && earlyFeetThread.getValue()
                    && Thunderhack.servtickManager.valid(Thunderhack.servtickManager.getTickTimeAdjusted(), 0, maxEarlyThread.getValue())) {
                threadHelper.startThread(false, true);
            }
        }
    }

    @SubscribeEvent
    public void onKeyBoard(KeyboardEvent event) {
        if (event.getEventState()
                && event.getKey() == switchBind.getValue().getKey()) {
            if (useAsOffhand.getValue()) {
                /*
                OffhandMode m = OFFHAND.returnIfPresent(Offhand::getMode, null);
                if (m != null)
                {
                    if (m.equals(OffhandMode.CRYSTAL))
                    {
                        OFFHAND.computeIfPresent(o ->
                                o.setMode(OffhandMode.TOTEM));
                    }
                    else
                    {
                        OFFHAND.computeIfPresent(o ->
                                o.setMode(OffhandMode.CRYSTAL));
                    }
                }


                 */
                switching = false;
            } else if (autoSwitch.getValue() == AutoSwitch.Bind) {
                switching = !switching;
                if (switchMessage.getValue()) {
                    Command.sendMessage(switching ? TextFormatting.GREEN + "Switch on" : TextFormatting.RED + "Switch off");
                }
            }
        }
    }

    @SubscribeEvent
    public void onEventSync(EventSync event) {

        if (!multiThread.getValue()
                && motionCalc.getValue()
                && (Thunderhack.positionManager.getX() != mc.player.posX
                || Thunderhack.positionManager.getY() != mc.player.posY
                || Thunderhack.positionManager.getZ() != mc.player.posZ)) {
            CalculationMotion calc = new CalculationMotion(this,
                    mc.world.loadedEntityList,
                    mc.world.playerEntities);
            threadHelper.start(calc, false);
        } else {
            if (motionThread.getValue()) {
                threadHelper.startThread();
            }
        }

        AbstractCalculation<?> current =
                threadHelper.getCurrentCalc();
        if (current != null
                && !current.isFinished()
                && rotate.getValue() != ACRotate.None
                && rotationThread.getValue() == RotationThread.Wait) {
            synchronized (this) {
                try {
                    wait(timeOut.getValue());
                } catch (InterruptedException e) {
                    Command.sendMessage("Minecraft Main-Thread interrupted!");
                    Thread.currentThread().interrupt();
                }
            }
        }

        RotationFunction rotation = this.rotation;
        if (rotation != null) {
            isSpoofing = true;
            float[] rotations = rotation.apply(mc.player.posX,
                    mc.player.posY,
                    mc.player.posZ,
                    event.getYaw(),
                    event.getPitch());

            if (rotateMode.getValue() == RotateMode.Smooth) {
                final float yaw = (yawMouseFilter.smooth(rotations[0] + MathUtil.random(-1.0f, 5.0f), smoothSpeed.getValue()));
                final float pitch = (pitchMouseFilter.smooth(rotations[1] + MathUtil.random(-1.20f, 3.50f), smoothSpeed.getValue()));
                mc.player.rotationYaw = (yaw);
                mc.player.rotationPitch = (pitch);
            } else {
                mc.player.rotationYaw = (rotations[0]);
                mc.player.rotationPitch = (rotations[1]);
            }
        } else if (rayTraceBypass.getValue()
                && forceBypass.getValue()) {
            BlockPos bypassPos = getBypassPos();
            if (bypassPos != null) {
                float[] rotations =
                        RotationUtil.getRotationsToTopMiddleUp(bypassPos);
                float pitch =
                        rotations[1] == 0.0f && rbYaw.getValue() != 0.0f
                                ? 0.0f
                                : rotations[1] < 0.0f
                                ? rotations[1] + rbPitch.getValue()
                                : rotations[1] - rbPitch.getValue();

                mc.player.rotationYaw = ((rotations[0] + rbYaw.getValue()) % 360);
                mc.player.rotationPitch = (pitch);
            }
        }
    }

    @SubscribeEvent
    public void onPostMotion(EventPostSync e) {
        motionID.incrementAndGet();
        synchronized (post) {
            runPost();
        }

        isSpoofing = false;
    }

    @SubscribeEvent
    public void onMotion(NoMotionUpdateEvent event) {
        if (multiThread.getValue()
                && !isSpoofing
                && rotate.getValue() != ACRotate.None
                && rotationThread.getValue() == RotationThread.Cancel) {
            forward = -forward;
            float yaw = Thunderhack.rotationManager.getServerYaw() + forward;
            float pitch = Thunderhack.rotationManager.getServerPitch() + forward;

            this.rotationCanceller.onPacketNigger9(new CPacketPlayer.Rotation(yaw, pitch, Thunderhack.positionManager.isOnGround()));
        } else {
            this.rotationCanceller.reset();
        }
    }

    protected void onEvent22(SPacketEntity packet) {
        if (!shouldCalc22()) {
            return;
        }

        EntityPlayer p = null;
        if (mc.world.getEntityByID(((ISPacketEntity) packet).getEntityId()) instanceof EntityPlayer)
            p = (EntityPlayer) mc.world.getEntityByID(((ISPacketEntity) packet).getEntityId());
        if (p == null) {
            return;
        }

        double x = (p.serverPosX + packet.getX()) / 4096.0;
        double y = (p.serverPosY + packet.getY()) / 4096.0;
        double z = (p.serverPosZ + packet.getZ()) / 4096.0;

        onEvent22(p, x, y, z);
    }

    protected void onEvent22(EntityPlayer player, double x, double y, double z) {
        Entity entity = getRotationPlayer();
        if (entity != null
                && entity.getDistanceSq(x, y, z)
                < MathUtil.square(targetRange.getValue())
                && !Thunderhack.friendManager.isFriend(player)) {
            // Scheduling is required since this event might get cancelled.
            Scheduler.getInstance().scheduleAsynchronously(() ->
            {
                if (mc.world == null) {
                    return;
                }

                List<EntityPlayer> enemies;


                enemies = Collections.emptyList();


                EntityPlayer target = getTTRG(mc.world.playerEntities, enemies, targetRange.getValue());

                if (target == null || target.equals(player)) {
                    threadHelper.startThread();
                }
            });
        }
    }

    public EntityPlayer getTTRG(List<EntityPlayer> players, List<EntityPlayer> enemies, Float maxRange) {
        switch (targetMode.getValue()) {
            case Fov: {
                EntityPlayer prioritizedEnemy = getByFov(enemies, maxRange);
                return prioritizedEnemy != null ? prioritizedEnemy : getByFov(players, maxRange);
            }
            case Angle: {
                EntityPlayer prioritizedEnemy = getByAngle(enemies, maxRange);
                return prioritizedEnemy != null ? prioritizedEnemy : getByAngle(players, maxRange);
            }
            case Closest:
                return getClosestEnemy(mc.player.posX,
                        mc.player.posY,
                        mc.player.posZ,
                        maxRange,
                        enemies,
                        players);
            default:
                return null;
        }
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean shouldCalc22() {
        boolean useMultiThread = multiThread.getValue();
        boolean useEntityThread = entityThread.getValue();
        boolean shouldNotRotate = rotate.getValue() == ACRotate.None;
        boolean avoidPredictRotation = rotationThread.getValue() != RotationThread.Predict;

        if (!useMultiThread || !useEntityThread) {
            return false;
        }

        if (shouldNotRotate) {
            return true;
        }

        return avoidPredictRotation;
    }


    protected EntityPlayer getEntity22(int id) {
        List<Entity> entities = mc.world.loadedEntityList;
        if (entities == null) {
            return null;
        }

        Entity entity = null;
        for (Entity e : entities) {
            if (e != null && e.getEntityId() == id) {
                entity = e;
                break;
            }
        }

        if (entity instanceof EntityPlayer) {
            return (EntityPlayer) entity;
        }

        return null;
    }

    private void onEvent33(PacketEvent.Receive event) {
        World world = mc.world;
        if (mc.player == null
                || world == null
                || basePlaceOnly.getValue()
                || ((SPacketSpawnObject) event.getPacket()).getType() != 51
                || mc.world == null
                || !spectator.getValue() && mc.player.isSpectator()
                || stopWhenEating.getValue() && isEating()
                || stopWhenMining.getValue() && isMining()
                || ((ISPacketSpawnObject) event.getPacket()).isAttacked()) {
            return;
        }

        SPacketSpawnObject packet = event.getPacket();
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        EntityEnderCrystal entity = new EntityEnderCrystal(world, x, y, z);

        if (simulatePlace.getValue() != 0) {
            event.addPostEvent(() ->
            {
                if (mc.world == null) {
                    return;
                }

                Entity e = mc.world.getEntityByID(packet.getEntityID());
                if (e instanceof EntityEnderCrystal) {
                    crystalRender.onSpawn((EntityEnderCrystal) e);
                }
            });
        }

        if (!instant.getValue() || !breakTimer.passed(breakDelay.getValue())) {
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        CrystalTimeStamp stamp = placed.get(pos);
        entity.setShowBottom(false);
        entity.setEntityId(packet.getEntityID());
        entity.setUniqueId(packet.getUniqueId());

        boolean attacked = false;
        if ((!alwaysCalc.getValue() || pos.equals(bombPos) && alwaysBomb.getValue()) && stamp != null && stamp.isValid() && (stamp.getDamage() > slowBreakDamage.getValue() || stamp.isShield() || breakTimer.passed(slowBreakDelay.getValue()) || pos.down().equals(antiTotemHelper.getTargetPos()))) {
            if (pos.equals(bombPos)) {
                bombPos = null;
            }

            float damage = checkPos(entity);
            if (damage <= -1000.0f) {
                MutableWrapper<Boolean> a = new MutableWrapper<>(false);
                rotation = rotationHelper.forBreaking(entity, a);
                event.addPostEvent(() ->
                {
                    if (mc.world != null) {
                        Entity e = mc.world.getEntityByID(packet.getEntityID());
                        if (e != null) {
                            post.add(rotationHelper.post(e, a));
                            rotation = rotationHelper.forBreaking(e, a);
                            setCrystal(e);
                        }
                    }
                });

                return;
            }

            if (damage < 0.0f) {
                return;
            }

            if (damage > shieldSelfDamage.getValue() && stamp.isShield()) {
                return;
            }
            attack(packet, event, entity, stamp.getDamage() <= slowBreakDamage.getValue());
            attacked = true;
        } else if (asyncCalc.getValue() || alwaysCalc.getValue()) {
            List<EntityPlayer> players = mc.world.playerEntities;
            if (players == null) {
                return;
            }

            float self = checkPos(entity);
            if (self < 0.0f) {
                // TODO: ROTATIONS HERE?
                return;
            }

            boolean slow = true;
            boolean attack = false;
            for (EntityPlayer player : players) {
                if (player == null
                        || EntityUtil.isDead(player)
                        || player.getDistanceSq(x, y, z) > 144) {
                    continue;
                }

                if (Thunderhack.friendManager.isFriend(player) && (!isSuicideModule() || !player.equals(mc.player))) {
                    if (shouldCalcFuckinBitch(AntiFriendPop.Break)) {
                        if (damageHelper.getDamage(entity, player) > EntityUtil.getHealth(player) - 0.5f) {
                            attack = false;
                            break;
                        }
                    }

                    continue;
                }

                float dmg = damageHelper.getDamage(entity, player);
                if ((dmg > self
                        || suicide.getValue()
                        && dmg >= minDamage.getValue())
                        && dmg > minBreakDamage.getValue()
                        && (dmg > slowBreakDamage.getValue()
                        || shouldDanger()
                        || breakTimer.passed(slowBreakDelay
                        .getValue()))) {
                    slow = slow && dmg <= slowBreakDamage.getValue();
                    attack = true;
                }
            }

            if (attack) {

                attack(packet, event, entity, (stamp == null || !stamp.isShield()) && slow);
                attacked = true;
            } else if (stamp != null
                    && stamp.isShield()
                    && self >= 0.0f
                    && self <= shieldSelfDamage.getValue()) {
                attack(packet, event, entity, false);
                attacked = true;
            }
        }

        if (spawnThread.getValue() && (!spawnThreadWhenAttacked.getValue() || attacked)) {
            threadHelper.schedulePacket(event);
        }
    }

    private void attack(SPacketSpawnObject packet, PacketEvent.Receive event, EntityEnderCrystal entityIn, boolean slow) {
        HelperInstantAttack.attack(this, packet, event, entityIn, slow);
    }

    private float checkPos(Entity entity) {
        BreakValidity validity = HelperUtil.isValid(this, entity, true);
        switch (validity) {
            // TODO: wtf is this magic number shit
            case INVALID:
                return -1.0f;
            case ROTATIONS:
                float damage = getSelfDamage(entity);
                if (damage < 0) {
                    return damage;
                }

                return -1000.0f - damage;
            case VALID:
            default:
        }

        return getSelfDamage(entity);
    }

    private float getSelfDamage(Entity entity) {
        float damage = damageHelper.getDamage(entity);

        float maxAllowedSelfBreak = maxSelfBreak.getValue();
        float playerHealth = EntityUtil.getHealth(mc.player);
        float minimumSafeHealth = playerHealth - 1.0f;

        if (damage > maxAllowedSelfBreak || damage > minimumSafeHealth) {
            // uncomment maye
            // Managers.SAFETY.setSafe(false);

            if (!suicide.getValue()) {
                return -1.0f;
            }
        }

        return damage;
    }



    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (this.render.getValue() && this.box.getValue() && this.fade.getValue()) {
            for (Map.Entry<BlockPos, Long> set : fadeList.entrySet()) {
                if (this.getRenderPos() == set.getKey()) {
                    continue;
                }

                final Color boxColor = this.boxColor.getValue().getColorObject();
                final Color outlineColor = this.outLine.getValue().getColorObject();
                final float maxBoxAlpha = boxColor.getAlpha();
                final float maxOutlineAlpha = outlineColor.getAlpha();
                final float alphaBoxAmount = maxBoxAlpha / this.fadeTime.getValue();
                final float alphaOutlineAmount = maxOutlineAlpha / this.fadeTime.getValue();
                final int fadeBoxAlpha = MathHelper.clamp((int) (alphaBoxAmount * (set.getValue() + this.fadeTime.getValue() - System.currentTimeMillis())), 0, (int) maxBoxAlpha);
                final int fadeOutlineAlpha = MathHelper.clamp((int) (alphaOutlineAmount * (set.getValue() + this.fadeTime.getValue() - System.currentTimeMillis())), 0, (int) maxOutlineAlpha);

                if (this.box.getValue()) {

                    BlockRenderUtil.prepareGL();
                    TessellatorUtil.drawBox(interpolatePos(set.getKey(), 1.0f), new Color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), fadeBoxAlpha));
                    BlockRenderUtil.releaseGL();

                    BlockRenderUtil.prepareGL();
                    TessellatorUtil.drawBoundingBox(interpolatePos(set.getKey(), 1.0f), 1.5f, new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), fadeOutlineAlpha));
                    BlockRenderUtil.releaseGL();

                }
            }
        }

        BlockPos pos;
        if (this.render.getValue() && (pos = this.getRenderPos()) != null) {
            if (!this.fade.getValue()) {
                if (this.box.getValue()) {
                    BlockRenderUtil.prepareGL();
                    TessellatorUtil.drawBox(interpolatePos(pos, 1.0f), this.boxColor.getValue().getColorObject());
                    BlockRenderUtil.releaseGL();

                    BlockRenderUtil.prepareGL();
                    TessellatorUtil.drawBoundingBox(interpolatePos(pos, 1.0f), 1.5f, this.outLine.getValue().getColorObject());
                    BlockRenderUtil.releaseGL();
                }
            }
            if (renderDamage.getValue() != RenderDamagePos.None)
                renderDamage(pos);

            if (this.fade.getValue())
                fadeList.put(pos, System.currentTimeMillis());
        }

        fadeList.entrySet().removeIf(e -> e.getValue() + this.fadeTime.getValue() < System.currentTimeMillis());


        if (this.renderExtrapolation.getValue()) {
            for (EntityPlayer player : mc.world.playerEntities) {
                MotionTracker tracker;
                if (player == null
                        || EntityUtil.isDead(player)
                        || RenderUtil.getEntity().getDistanceSq(player) > 200
                        || !inFov(player)
                        || player.equals(mc.player)
                        || (tracker = this.extrapolationHelper
                        .getTrackerFromEntity(player)) == null
                        || !tracker.active) {
                    continue;
                }

                Vec3d interpolation = interpolateEntity(player, mc.getRenderPartialTicks());
                double x = interpolation.x - getRenderPosX();
                double y = interpolation.y - getRenderPosY();
                double z = interpolation.z - getRenderPosZ();

                double tX = tracker.posX - getRenderPosX();
                double tY = tracker.posY - getRenderPosY();
                double tZ = tracker.posZ - getRenderPosZ();

                //    RenderUtil.startRender();
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();

                if (Thunderhack.friendManager.isFriend(player)) {
                    GL11.glColor4f(0.33333334f, 0.78431374f, 0.78431374f, 0.55f);
                } else {
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                }

                boolean viewBobbing = mc.gameSettings.viewBobbing;
                mc.gameSettings.viewBobbing = false;
                ((IEntityRenderer) mc.entityRenderer).orientCam(event.getPartialTicks());
                mc.gameSettings.viewBobbing = viewBobbing;

                GL11.glLineWidth(1.5f);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3d(tX, tY, tZ);
                GL11.glVertex3d(x, y, z);
                GL11.glEnd();

                GlStateManager.popMatrix();
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                //   RenderUtil.endRender();
            }
        }
    }

    private void renderDamage(BlockPos pos) {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        String text = this.damage;
        GlStateManager.pushMatrix();
        enableStandardItemLighting();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(1.0f, -1500000.0f);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        double x = pos.getX() + 0.5;
        double y = pos.getY() + (this.renderDamage.getValue() == RenderDamagePos.OnTop ? 1.35 : 0.5);
        double z = pos.getZ() + 0.5;

        float scale = 0.016666668f * (this.renderMode.getValue() == RenderDamage.Indicator ? 0.95f : 1.3f);

        GlStateManager.translate(x - getRenderPosX(), y - getRenderPosY(), z - getRenderPosZ());

        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.player.rotationYaw, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(mc.player.rotationPitch, mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-scale, -scale, scale);

        int distance = (int) mc.player.getDistance(x, y, z);
        float scaleD = (distance / 2.0f) / (2.0f + (2.0f - 1));
        if (scaleD < 1.0f) {
            scaleD = 1;
        }

        GlStateManager.scale(scaleD, scaleD, scaleD);
        GlStateManager.translate(-(FontRender.getStringWidth6(text) / 2.0), 0, 0);

        FontRender.drawString6(text, 0, 0, -1, false);

        GlStateManager.disablePolygonOffset();
        GlStateManager.doPolygonOffset(1.0f, 1500000.0f);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    }

    @SubscribeEvent
    public void onRenderEntity(PostRenderEntitiesEvent event) {
        if (event.getPass() == 0) {
            this.crystalRender.render(event.getPartialTicks());
        }
    }


    public enum settingtypeEn {
        Noob,
        Pro,
        Hacker,
    }


    public enum pages {
        Place,
        Break,
        Rotations,
        Misc,
        FacePlace,
        SwitchNSwing,
        Render,
        Predict,
        Dev,
        SetDead,
        Obsidian,
        Liquid,
        AntiTotem,
        DamageSync,
        Extrapolation,
        Efficiency,
        MultiThreading
    }
    /// БЛЯЯЯЯЯЯЯЯЯЯЯТЬ

    public enum ACRotate {
        None,
        All,
        Break,
        Place,
    }
    public enum AntiFriendPop {
        All,
        Break,
        Place,
        None
    }

    public enum AntiWeakness {
        None,
        Switch
    }

    public enum Attack2 {
        Always,
        Crystal,
        Calc
    }

    public enum BlockExtrapolationMode {
        Extrapolated,
        Pessimistic,
        Optimistic
    }

    public enum BreakValidity {
        INVALID,
        ROTATIONS,
        VALID
    }

    public enum ExtrapolationType {
        None,
        Place,
        Break,
        Block
    }

    public enum PreCalc {
        None,
        Target,
        Damage
    }

    public enum RenderDamage {
        Normal,
        Indicator
    }


    public enum RenderDamagePos {
        None,
        Inside,
        OnTop
    }

    public enum RotateMode {
        Normal,
        Smooth
    }


    public enum RotationThread {
        Predict,
        Cancel,
        Wait
    }

    public enum SwingTime {
        None,
        Pre,
        Post
    }

    public enum SwingType {
        None,
        MainHand,
        OffHand,
    }

    public enum Target {
        Closest,
        Damage,
        Angle,
        Fov
    }

    public enum SmartRange {
        None,
        Normal,
        All,
        Extrapolated
    }


    public enum AutoSwitch {
        None,
        Bind,
        Always
    }


    public enum CooldownBypass2 {
        None,
        Swap,
        Pick,
        Slot
    }


    public enum RayTraceMode {
        Fast,
        Resign,
        Force,
        Smart
    }

    public enum PlaceSwing {
        Always,
        Never,
        Once
    }


    public enum Rotate {
        None,
        Normal,
        Packet
    }


}
