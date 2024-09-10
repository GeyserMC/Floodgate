package org.geysermc.floodgate.mod.listener;

import com.google.inject.Inject;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.core.util.LanguageManager;

import java.util.UUID;

public final class ModEventListener {
    @Inject private FloodgateApi api;
    @Inject private FloodgateLogger logger;
    @Inject private LanguageManager languageManager;

    public void onPlayerJoin(UUID uuid) {
        FloodgatePlayer player = api.getPlayer(uuid);
        if (player != null) {
            logger.translatedInfo(
                    "floodgate.ingame.login_name",
                    player.getCorrectUsername(), player.getCorrectUniqueId()
            );
            languageManager.loadLocale(player.getLanguageCode());
        }
    }
}
