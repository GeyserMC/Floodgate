package org.geysermc.floodgate.core.platform.command;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.geysermc.floodgate.core.connection.audience.UserAudience;

public record Placeholder(@TagPattern String key, Tag tag) {
    public static Placeholder dynamic(@TagPattern String key, String value) {
        return new Placeholder(key, Tag.preProcessParsed(value));
    }

    public static Placeholder dynamic(@TagPattern String key, TranslatableMessage message, UserAudience audience) {
        return dynamic(key, audience.commandUtil().languageManager().rawTranslation(message.toString(), audience.locale()));
    }

    public static Placeholder literal(@TagPattern String key, String value) {
        return literal(key, Component.text(value));
    }

    public static Placeholder literal(@TagPattern String key, ComponentLike component) {
        return new Placeholder(key, Tag.selfClosingInserting(component));
    }

    public static Placeholder literal(@TagPattern String key, Object value) {
        return literal(key, Objects.toString(value));
    }

    public TagResolver resolver(MessageType type) {
        return TagResolver.resolver(key, ($, context) -> {
            var root = Component.empty();

            // We've only looked at PreProcess and Inserting.
            // We've excluded PreProcess because we expect the placeholders of the PreProcess to have the secondary
            // color, not the PreProcess itself.
            if (type.secondaryColor() != null && tag instanceof Inserting) {
                root = root.color(type.secondaryColor());
            }

            if (tag instanceof PreProcess preProcess) {
                root = root.append(context.deserialize(preProcess.value()));
            } else if (tag instanceof Inserting inserting) {
                root = root.append(inserting.value());
            } else {
                throw new UnsupportedOperationException("Only PreProcess and Inserting have been implemented!");
            }

            return Tag.selfClosingInserting(root);
        });
    }
}
