package org.geysermc.floodgate.util;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("org.geysermc.floodgate.mixin.ClientIntentionPacketMixin")) {
            //returns true if fabricproxy-lite is present, therefore loading the mixin. If not present, the mixin will not be loaded.
            return FabricLoader.getInstance().isModLoaded("fabricproxy-lite");
        }
        if (mixinClassName.equals("org.geysermc.floodgate.mixin.GeyserModInjectorMixin")) {
            return FabricLoader.getInstance().isModLoaded("geyser-fabric");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}