package com.cleanroommc.tickflow.path;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BossChecker {

    private static final Map<Class<?>, Boolean> BOSS_CLASS_CACHE = new ConcurrentHashMap<>();

    public static boolean isBoss(EntityLiving entity) {
        if (entity == null) {
            return false;
        }

        if (!entity.isNonBoss()) {
            return true;
        }

        Class<?> clazz = entity.getClass();
        Boolean cachedClass = BOSS_CLASS_CACHE.get(clazz);
        if (cachedClass != null) {
            return cachedClass;
        }

        boolean result = computeBoss(entity);
        BOSS_CLASS_CACHE.put(clazz, result);
        return result;
    }

    private static boolean computeBoss(EntityLiving entity) {
        EntityEntry entry = EntityRegistry.getEntry(entity.getClass());
        if (entry != null && entry.getRegistryName() != null) {
            ResourceLocation loc = entry.getRegistryName();
            String name = loc.toString().toLowerCase();
            if (name.contains("mosontweaker:paladin") ||
                name.contains("mosontweaker:zeus") ||
                name.contains("mosontweaker:dr_stalker") ||
                name.contains("wroughtnaut") ||
                name.contains("barako") ||
                name.contains("frostmaw") ||
                name.contains("dragon") ||
                name.contains("gorgon") ||
                name.contains("siren") ||
                name.contains("cyclops") ||
                name.contains("rahovart") ||
                name.contains("asmodeus") ||
                name.contains("amalgalich")) {
                return true;
            }
        }

        String className = entity.getClass().getName().toLowerCase();
        if (className.contains("boss") || className.contains("wroughtnaut") ||
            className.contains("zeus") || className.contains("stalker") ||
            className.contains("dragon") || className.contains("wither")) {
            return true;
        }

        // Deep inspection of class and superclasses for BossInfo or BossBar fields
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String fieldTypeName = f.getType().getName();
                    if (fieldTypeName.contains("BossInfo") || fieldTypeName.contains("BossBar")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }
}
