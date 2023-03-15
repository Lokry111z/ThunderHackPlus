package com.mrzak34.thunderhack.modules.client;


import com.mrzak34.thunderhack.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class MultiConnect extends Module {

    private static MultiConnect INSTANCE = new MultiConnect();
    public List<Integer> serverData = new ArrayList<>();

    public MultiConnect() {
        super("MultiConnect", "MultiConnect", Category.CLIENT);
        this.setInstance();
    }

    public static MultiConnect getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MultiConnect();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }
}
