package com.minekube.connect.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.minekube.connect.api.logger.ConnectLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {
    @TempDir Path tempDir;

    @Test
    void loadsDocumentedAllowOfflineModePlayersKey() throws Exception {
        Files.writeString(tempDir.resolve("config.yml"), String.join("\n",
                "endpoint: codexp2p3",
                "allow-offline-mode-players: true",
                "metrics:",
                "  disabled: true",
                "  uuid: 00000000-0000-0000-0000-000000000000",
                "config-version: 1",
                ""));

        ConfigLoader loader = new ConfigLoader(
                tempDir,
                ConnectConfig.class,
                new ConfigLoader.EndpointNameGenerator(new OkHttpClient()),
                mock(ConnectLogger.class));

        ConnectConfig config = loader.load();

        assertEquals(Boolean.TRUE, config.getAllowOfflineModePlayers());
    }
}
