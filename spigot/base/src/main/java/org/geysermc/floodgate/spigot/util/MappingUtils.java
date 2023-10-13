package org.geysermc.floodgate.spigot.util;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.geysermc.floodgate.core.util.ReflectionUtils;

public class MappingUtils {
    private static final String LEGACY_MAPPING_PREFIX;
    private static final String CRAFTBUKKIT_MAPPING_PREFTIX;

    private MappingUtils() {}

    public static Class<?> classFor(String mojangPackage, String className) {
        return classFor(mojangPackage, className, className);
    }

    public static Class<?> classFor(String mojangPackage, String mojangName, String spigotName) {
        Class<?> mojmap = ReflectionUtils.getClassSilently(mojangPackage + "." + mojangName);
        if (mojmap != null) {
            return mojmap;
        }
        Class<?> spigot = ReflectionUtils.getClassSilently(mojangPackage + "." + spigotName);
        if (spigot != null) {
            return spigot;
        }
        Class<?> legacy = ReflectionUtils.getClassSilently(LEGACY_MAPPING_PREFIX + "." + spigotName);
        if (legacy != null) {
            return legacy;
        }
        throw new IllegalStateException(genericMessage("class " + mojangPackage + "." + mojangName));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> craftbukkitClass(String className) {
        return (Class<T>) ReflectionUtils.getClassOrThrow(CRAFTBUKKIT_MAPPING_PREFTIX + "." + className);
    }

    public static Field fieldFor(Class<?> clazz, String mojangName, String spigotName) {
        var mojmap = ReflectionUtils.getField(clazz, mojangName);
        if (mojmap != null) {
            return mojmap;
        }
        var spigot = ReflectionUtils.getField(clazz, spigotName);
        if (spigot != null) {
            return spigot;
        }
        throw new IllegalStateException(genericMessage("field " + mojangName + " for class " + clazz));
    }

    private static String genericMessage(String specific) {
        return "Could not find " + specific + ". What server software are you using?";
    }

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        LEGACY_MAPPING_PREFIX = "net.minecraft.server." + version;
        CRAFTBUKKIT_MAPPING_PREFTIX = "org.bukkit.craftbukkit." + version;
    }
}
