package com.mrzak34.thunderhack.modules.player;

import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.setting.Setting;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;

public class NoEntityTrace extends Module {
    private static NoEntityTrace INSTANCE;

    static {
        NoEntityTrace.INSTANCE = new NoEntityTrace();
    }

    public Setting<Boolean> pick = this.register(new Setting<>("Pick", true));
    public Setting<Boolean> gap =  this.register(new Setting<>("Gap", false));
    public Setting<Boolean> obby = this.register(new Setting<>("Obby", false));
    public boolean noTrace;

    public NoEntityTrace() {
        super("NoEntityTrace", "копать сквозь игроков", Module.Category.PLAYER);
        this.setInstance();
    }

    public static NoEntityTrace getINSTANCE() {
        if (NoEntityTrace.INSTANCE == null) {
            NoEntityTrace.INSTANCE = new NoEntityTrace();
        }
        return NoEntityTrace.INSTANCE;
    }

    private void setInstance() {
        NoEntityTrace.INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        final Item item = NoEntityTrace.mc.player.getHeldItemMainhand().getItem();
        if (item instanceof ItemPickaxe && this.pick.getValue()) {
            this.noTrace = true;
            return;
        }
        if (item == Items.GOLDEN_APPLE && this.gap.getValue()) {
            this.noTrace = true;
            return;
        }
        if (item instanceof ItemBlock) {
            this.noTrace = (((ItemBlock) item).getBlock() == Blocks.OBSIDIAN && this.obby.getValue());
            return;
        }
        this.noTrace = false;
    }
}