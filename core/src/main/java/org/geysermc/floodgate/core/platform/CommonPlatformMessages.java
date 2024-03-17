package org.geysermc.floodgate.core.platform;

import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;

public class CommonPlatformMessages {
    public static final TranslatableMessage CORE_FINISH = new TranslatableMessage("floodgate.core.finish");
    public static final TranslatableMessage CONNECTION_LOGIN = new TranslatableMessage("floodgate.ingame.login_name");
    public static final TranslatableMessage CONNECTION_DISCONNECT = new TranslatableMessage("floodgate.ingame.disconnect_name");

    public static final TranslatableMessage NOT_LINKED = new TranslatableMessage("floodgate.core.not_linked", MessageType.ERROR);
}
