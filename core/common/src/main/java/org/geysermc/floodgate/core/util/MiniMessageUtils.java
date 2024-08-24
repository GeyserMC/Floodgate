package org.geysermc.floodgate.core.util;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.Placeholder;

public final class MiniMessageUtils {
    private MiniMessageUtils() {}

    public static Component formatMessage(String message, MessageType type, Placeholder... placeholders) {
        var styledResolvers = Arrays.stream(placeholders)
                .map(placeholder -> placeholder.resolver(type))
                .toArray(TagResolver[]::new);

        var component = MiniMessage.miniMessage().deserialize(message, styledResolvers);

        if (type.primaryColor() != null) {
            component = Component.empty().color(type.primaryColor()).append(component);
        }
        return component;
    }
}
