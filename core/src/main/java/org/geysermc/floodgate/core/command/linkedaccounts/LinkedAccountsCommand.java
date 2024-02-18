package org.geysermc.floodgate.core.command.linkedaccounts;

import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;
import org.geysermc.floodgate.core.platform.command.SubCommands;

@Singleton
final class LinkedAccountsCommand extends SubCommands {
    LinkedAccountsCommand() {
        super("linkedaccounts", "Manage locally linked accounts", Permission.COMMAND_LINKED);
    }

    @Singleton
    static String linkInfoMessage(LinkedPlayer player) {
        return "Java UUID: %s\nJava username: %s\nBedrock UUID: %s"
                .formatted(player.javaUniqueId(), player.javaUsername(), player.bedrockId());
    }
}
