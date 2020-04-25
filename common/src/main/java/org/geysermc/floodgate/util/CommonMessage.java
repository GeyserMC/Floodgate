package org.geysermc.floodgate.util;

import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;

public enum CommonMessage implements CommandMessage {
    NOT_A_PLAYER("Please head over to your Minecraft Account and link from there.");

    @Getter private final String message;

    CommonMessage(String message) {
        this.message = message.replace('&', COLOR_CHAR);
    }
}
