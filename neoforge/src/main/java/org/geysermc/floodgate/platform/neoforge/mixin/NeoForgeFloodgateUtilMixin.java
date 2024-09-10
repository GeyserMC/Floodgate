package org.geysermc.floodgate.platform.neoforge.mixin;

import org.geysermc.floodgate.core.util.Utils;
import org.geysermc.floodgate.mod.FloodgateMod;
import org.geysermc.floodgate.platform.neoforge.NeoForgeFloodgateMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Mixin into Floodgate's {@link Utils} class as NeoForge is really picky about how it allows scanning
 * mod-owned classes.
 */
@Mixin(value = Utils.class, remap = false)
public class NeoForgeFloodgateUtilMixin {

    /**
     * @author geysermc
     * @reason NeoForge is really picky about how it allows scanning mod-owned classes.
     */
    @Overwrite(remap = false)
    public static Set<Class<?>> getGeneratedClassesForAnnotation(Class<? extends Annotation> annotationClass) {
        return ((NeoForgeFloodgateMod) FloodgateMod.INSTANCE).getAnnotatedClasses(annotationClass);
    }
}
