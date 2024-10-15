/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.config;

import java.security.Key;
import java.util.UUID;
import org.geysermc.floodgate.core.util.Constants;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.interfaces.meta.Field;
import org.spongepowered.configurate.interfaces.meta.Hidden;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultNumeric;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;

/**
 * The global Floodgate configuration file used in every platform. Some platforms have their own
 * addition to the global configuration like {@link ProxyFloodgateConfig} for the proxies.
 */
@ConfigSerializable
public interface FloodgateConfig {
    @Exclude
    default boolean proxy() {
        return this instanceof ProxyFloodgateConfig;
    }

    @PostProcess
    default void postInitialise() {
        // todo add postInitialize with argument
        //    @PostProcess
        //    default void postInitialize(Path dataDirectory) throws SerializationException {
        //        Path keyPath = dataDirectory.resolve(keyFileName());
        //
        //        // don't assume that the key always exists with the existence of a config
        //        if (!Files.exists(keyPath)) {
        //            // TODO improve message and also link to article about corrupted keys/WinSCP/FTP
        //            // Like, where is key.pem?
        //            // TODO don't be so noisy with error. It makes it hard to understand what the error is.
        //            throw new SerializationException("Floodgate requires a key file! " +
        //                    "Copy your key file from Geyser (key.pem) and paste it into " + keyPath);
        //        }

        // todo remove key file name config option

        rawUsernamePrefix(usernamePrefix());

        // Java usernames can't be longer than 16 chars
        if (usernamePrefix().length() >= 16) {
            usernamePrefix(".");
        }
    }

    @Comment(
            """
            In Floodgate bedrock player data is send encrypted.
            The following value should point to the key Floodgate generated.
            The public key should be used for the Geyser(s) and the private key for the Floodgate(s)""")
    @DefaultString("key.pem")
    String keyFileName();

    @Comment(
            """
            Floodgate prepends a prefix to bedrock usernames to avoid conflicts.
            However, certain conflicts can cause issues with some plugins so this prefix is configurable using the property below
            It is recommended to use a prefix that does not contain alphanumerical to avoid the possibility of duplicate usernames.""")
    @DefaultString(".")
    String usernamePrefix();

    void usernamePrefix(String usernamePrefix);

    @Comment("Should spaces be replaced with '_' in bedrock usernames?")
    @DefaultBoolean(true)
    boolean replaceSpaces();

    @Comment("The default locale for Floodgate. By default, Floodgate uses the system locale")
    @DefaultString("system")
    String defaultLocale();

    @Hidden
    @DefaultBoolean
    boolean debug();

    DisconnectMessages disconnect();

    DatabaseConfig database();

    @Comment("Configuration for player linking")
    PlayerLinkConfig playerLink();

    MetricsConfig metrics();

    default int version() {
        return Constants.CONFIG_VERSION;
    }

    @Field
    Key key();

    @Field
    void key(Key key);

    @Field
    String rawUsernamePrefix();

    @Field
    void rawUsernamePrefix(String usernamePrefix);

    @ConfigSerializable
    interface DisconnectMessages {
        @Comment(
                """
                The disconnect message Geyser users should get when connecting
                to the server with an invalid key""")
        @DefaultString("Please connect through the official Geyser")
        String invalidKey();

        @Comment(
                """
                The disconnect message Geyser users should get when connecting
                to the server with the correct key but not with the correct data format""")
        @DefaultString("Expected {} arguments, got {}. Is Geyser up-to-date?")
        String invalidArgumentsLength();
    }

    @ConfigSerializable
    interface DatabaseConfig {
        @Comment("Whether a database can be used (required for for example local linking)")
        @DefaultBoolean(true)
        boolean enabled();

        @Comment("The database type you want to use.")
        @DefaultString("h2")
        String type();
    }

    @ConfigSerializable
    interface PlayerLinkConfig {
        @Comment(
                """
                Whether to enable the linking system. Turning this off will prevent
                players from using the linking feature even if they are already linked.""")
        @DefaultBoolean(true)
        boolean enabled();

        @Comment("Whether to require a linked account in order to be able to join the server.")
        @DefaultBoolean
        boolean requireLink();

        @Comment(
                """
                Set the following option to true when you want to host your own linking database.
                -> This can work in addition to global linking.
                Note that you have to enable the database in the database section as well.
                """)
        @DefaultBoolean
        boolean enableOwnLinking();

        @Comment(
                """
                The following two options only apply when 'enable-own-linking' is set to 'true'

                Whether to allow the use of /linkaccount and /unlinkaccount
                You can also use allow specific people to use the commands using the
                permissions floodgate.command.linkaccount and floodgate.command.unlinkaccount.
                This is only for linking, already connected people will stay connected""")
        @DefaultBoolean(true)
        boolean allowed();

        @Comment("The amount of seconds until a link code expires.")
        @DefaultNumeric(300)
        long linkCodeTimeout();

        @Comment(
                """
                Whether to allow Bedrock servers to create a link request by logging in twice.
                This requires both 'enable-own-linking' and 'require-link' to be enabled.
                This option does not work properly with multi-proxy setups.""")
        @DefaultBoolean
        boolean allowCreateLinkRequest();

        @Comment(
                """
                Whether to enable global linking. Global Linking is a central server where people can link their
                accounts (Java and Bedrock) and join on servers that have Global Linking enabled. The goal of
                Global Linking is to make linking easier by not having to link your accounts on every server.
                -> Your server-specific linking database will have priority over global linking.
                Global Linking should normally only be disabled when you don't have internet access or when
                you have limited internet access.
                """)
        @DefaultBoolean(true)
        boolean enableGlobalLinking();
    }

    @ConfigSerializable
    interface MetricsConfig {
        @DefaultBoolean(true)
        boolean enabled();

        default UUID uuid() {
            return UUID.randomUUID();
        }
    }
}
