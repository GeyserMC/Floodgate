package org.geysermc.floodgate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import org.geysermc.floodgate.util.EncryptionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class FloodgateConfig {
    @JsonProperty(value = "key-file-name")
    private String keyFileName;
    @JsonProperty(value = "disconnect")
    private DisconnectMessages messages;
    @JsonProperty(value = "username-prefix")
    private String usernamePrefix;
    @JsonProperty(value = "replace-spaces")
    private boolean replaceSpaces;

    @JsonProperty(value = "player-link")
    private PlayerLinkConfig playerLink;

    @JsonProperty
    private boolean debug;

    @JsonIgnore
    PrivateKey privateKey = null;

    @Getter
    public static class DisconnectMessages {
        @JsonProperty("invalid-key")
        private String invalidKey;
        @JsonProperty("invalid-arguments-length")
        private String invalidArgumentsLength;
    }

    @Getter
    public static class PlayerLinkConfig {
        @JsonProperty("enable")
        private boolean enabled;
        @JsonProperty("type")
        private String type;
        @JsonProperty("allow-linking")
        private boolean allowLinking;
        @JsonProperty("link-code-timeout")
        private long linkCodeTimeout;
    }

    public static FloodgateConfig load(Logger logger, Path configPath) {
        return load(logger, configPath, FloodgateConfig.class);
    }

    public static <T extends FloodgateConfig> T load(Logger logger, Path configPath, Class<T> configClass) {
        T config = null;
        try {
            try {
                if (!configPath.toFile().exists()) {
                    Files.copy(FloodgateConfig.class.getClassLoader().getResourceAsStream("config.yml"), configPath);

                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(2048);
                    KeyPair keyPair = generator.generateKeyPair();

                    String test = "abcdefghijklmnopqrstuvwxyz1234567890";

                    String encrypted = EncryptionUtil.encrypt(keyPair.getPublic(), test);
                    String decrypted = new String(EncryptionUtil.decrypt(keyPair.getPrivate(), encrypted));

                    if (!test.equals(decrypted)) {
                        System.out.println(test +" "+ decrypted +" "+ encrypted +" " + new String(Base64.getDecoder().decode(encrypted.split("\0")[1])));
                        throw new RuntimeException(
                                "Testing the private and public key failed," +
                                "the original message isn't the same as the decrypted!"
                        );
                    }

                    Files.write(configPath.getParent().resolve("encrypted.txt"), encrypted.getBytes());

                    Files.write(configPath.getParent().resolve("public-key.pem"), keyPair.getPublic().getEncoded());
                    Files.write(configPath.getParent().resolve("key.pem"), keyPair.getPrivate().getEncoded());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while creating config", e);
            }

            config = new ObjectMapper(new YAMLFactory()).readValue(
                    Files.readAllBytes(configPath), configClass
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while loading config", e);
        }

        if (config == null) {
            throw new RuntimeException("Failed to load config file! Try to delete the data folder of Floodgate");
        }

        try {
            config.privateKey = EncryptionUtil.getKeyFromFile(
                    configPath.getParent().resolve(config.getKeyFileName()),
                    PrivateKey.class
            );
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error while reading private key", e);
        }
        return config;
    }
}
