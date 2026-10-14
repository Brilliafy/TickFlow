package com.cleanroommc.tickflow.mixin;

import net.minecraft.tileentity.MobSpawnerBaseLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobSpawnerBaseLogic.class)
public interface MobSpawnerBaseLogicAccessor {

    @Accessor("activatingRangeFromPlayer")
    int getActivatingRangeFromPlayer();
}
