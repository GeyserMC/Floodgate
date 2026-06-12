package com.minekube.connect.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.platform.util.PlatformUtils;
import com.minekube.connect.util.Constants;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommonModuleTest {
    @TempDir Path tempDir;

    @Test
    void connectHttpClientSendsPluginVersionHeader() throws Exception {
        CommonModule module = new CommonModule(tempDir);
        PlatformUtils platformUtils = mock(PlatformUtils.class);
        when(platformUtils.authType()).thenReturn(PlatformUtils.AuthType.ONLINE);
        when(platformUtils.serverImplementationName()).thenReturn("Paper");
        when(platformUtils.minecraftVersion()).thenReturn("1.21.11");
        when(platformUtils.getPlayerCount()).thenReturn(7);

        OkHttpClient client = module.connectOkHttpClient(
                module.defaultOkHttpClient(),
                platformUtils,
                "spigot",
                new SimpleConnectApi(mock(ConnectLogger.class))
        );

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("ok"));
            server.start();

            Request request = new Request.Builder()
                    .url(server.url("/watch"))
                    .build();
            try (Response ignored = client.newCall(request).execute()) {
                RecordedRequest recorded = server.takeRequest();

                assertEquals(Constants.VERSION, recorded.getHeader("Connect-Version"));
                assertEquals("spigot", recorded.getHeader("Connect-Platform"));
                assertEquals("Paper", recorded.getHeaders().values("Connect-Platform").get(1));
            }
        }
    }
}
