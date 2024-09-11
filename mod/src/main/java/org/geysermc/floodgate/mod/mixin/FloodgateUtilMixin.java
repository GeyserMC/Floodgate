package org.geysermc.floodgate.mod.mixin;

import org.geysermc.floodgate.core.util.Utils;
import org.geysermc.floodgate.mod.FloodgateMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mixins into Floodgate's {@link Utils} class to modify how resources are loaded from the jar.
 * This must be done due to mod platforms sharing a classloader across mods - this leads to Floodgate
 * loading Geyser's language files, as they're not prefixed to avoid conflicts.
 * To resolve this, this mixin replaces those calls with the platform-appropriate methods to load files.
 */
@Mixin(value = Utils.class, remap = false)
public class FloodgateUtilMixin {

    @Redirect(method = "readProperties",
            at = @At(value = "INVOKE", target = "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"))
    private static InputStream floodgate$redirectInputStream(ClassLoader instance, String string) {
        Path path = FloodgateMod.INSTANCE.resourcePath(string);
        try {
            return path == null ? null : Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Redirect(method = "getGeneratedClassesForAnnotation(Ljava/lang/String;)Ljava/util/Set;",
            at = @At(value = "INVOKE", target = "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"))
    private static InputStream floodgate$redirectInputStreamAnnotation(ClassLoader instance, String string) {
        Path path = FloodgateMod.INSTANCE.resourcePath(string);

        if (path == null) {
            throw new IllegalStateException("Unable to find classes marked by annotation class! " + string);
        }

        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
