package com.cleanroommc.tickflow;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;

public class TickFlowLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<>();
        configs.add("mixins.tickflow.json");
        if (Loader.isModLoaded("dynamictrees")) {
            configs.add("mixins.tickflow.dtd.json");
        }
        return configs;
    }
}
