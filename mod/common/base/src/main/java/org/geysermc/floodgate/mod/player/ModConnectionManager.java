package org.geysermc.floodgate.mod.player;

import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.connection.ConnectionManager;

public class ModConnectionManager extends ConnectionManager {
    @Override
    protected @Nullable Object platformIdentifierOrConnectionFor(Object input) {
        if (input instanceof Player player) {
            return connectionByUuid(player.getUUID());
        }
        return null;
    }
}
