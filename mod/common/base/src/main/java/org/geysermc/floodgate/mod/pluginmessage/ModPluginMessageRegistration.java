package org.geysermc.floodgate.mod.pluginmessage;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;

public abstract class ModPluginMessageRegistration implements PluginMessageRegistration {
    @Override
    public abstract void register(PluginMessageChannel channel);
}
