package com.mrzak34.thunderhack.modules.misc;

import com.mrzak34.thunderhack.events.EventSync;
import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.setting.Setting;
import com.mrzak34.thunderhack.util.InventoryUtil;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoSoup extends Module {

    public Setting<Float> thealth = this.register(new Setting<>("TriggerHealth", 7f, 1f, 20f));

    public AutoSoup() {
        super("AutoSoup", "Автосуп для-Мигосмси", Category.MISC);
    }

    @SubscribeEvent
    public void onUpdateWalkingPlayer(EventSync event) {
        if (mc.player.getHealth() <= thealth.getValue()) {
            int soupslot = InventoryUtil.findSoupAtHotbar();
            int currentslot = mc.player.inventory.currentItem;
            if (soupslot != -1) {
                mc.player.connection.sendPacket(new CPacketHeldItemChange(soupslot));
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
                InventoryUtil.switchToHotbarSlot(currentslot, true);
            }
        }
    }
}
