package org.geysermc.floodgate.core.platform.command;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum MessageType {
    NONE(null, null),
    NORMAL(null, NamedTextColor.AQUA),
    INFO(NamedTextColor.YELLOW, NamedTextColor.AQUA),
    SUCCESS(NamedTextColor.GREEN, NamedTextColor.GOLD),
    ERROR(NamedTextColor.RED, NamedTextColor.GOLD);

    private final TextColor primaryColor;
    private final TextColor secondaryColor;

    MessageType(@Nullable TextColor primaryColor, @Nullable TextColor secondaryColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public @Nullable TextColor primaryColor() {
        return primaryColor;
    }

    public @Nullable TextColor secondaryColor() {
        return secondaryColor;
    }
}
