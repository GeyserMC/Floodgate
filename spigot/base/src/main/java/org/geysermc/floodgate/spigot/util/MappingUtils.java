package org.geysermc.floodgate.spigot.util;

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
        throw new IllegalStateException(
                "Could not find class " + mojangPackage + "." + mojangName + ". What server software are you using?"
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> craftbukkitClass(String className) {
        return (Class<T>) ReflectionUtils.getClassOrThrow(CRAFTBUKKIT_MAPPING_PREFTIX + "." + className);
    }

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        LEGACY_MAPPING_PREFIX = "net.minecraft.server." + version;
        CRAFTBUKKIT_MAPPING_PREFTIX = "org.bukkit.craftbukkit." + version;
    }
}
