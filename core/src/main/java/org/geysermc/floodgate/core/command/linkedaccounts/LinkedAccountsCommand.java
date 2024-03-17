package org.geysermc.floodgate.core.command.linkedaccounts;

import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.SubCommands;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;

@Singleton
final class LinkedAccountsCommand extends SubCommands {
    LinkedAccountsCommand() {
        super("linkedaccounts", "Manage locally linked accounts", Permission.COMMAND_LINKED);
    }

    public static final class LinkedAccountsCommonMessage {
        public static final TranslatableMessage NOT_FOUND = new TranslatableMessage("floodgate.command.linkedaccounts.common.not_found", MessageType.ERROR);
        public static final TranslatableMessage LINK_INFO = new TranslatableMessage("floodgate.command.linkedaccounts.common.link_info");
    }
}
