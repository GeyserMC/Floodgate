package org.geysermc.floodgate.mixin_interface;

import com.mojang.authlib.GameProfile;

public interface ServerLoginNetworkHandlerSetter {
    void setGameProfile(GameProfile profile);

    void setLoginState();
}
