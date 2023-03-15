package com.mrzak34.thunderhack.modules.render;

import com.mrzak34.thunderhack.modules.Module;
import com.mrzak34.thunderhack.setting.Setting;

public class NoInterp extends Module {
    private static NoInterp INSTANCE = new NoInterp();
    public Setting<Boolean> lowIQ = this.register(new Setting<>("LowIQ", true));

    public NoInterp() {
        super("NoInterp", "убирает интерполяцию-с игроков", Module.Category.RENDER);
        this.setInstance();
    }

    public static NoInterp getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoInterp();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }

}
