package org.geysermc.floodgate.mixin_interface;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;

import java.util.UUID;

public interface ServerLoginNetworkHandlerSetter {
    void setGameProfile(GameProfile profile);

    void setLoginState();
}
