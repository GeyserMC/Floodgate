package org.geysermc.floodgate.core.http;

import io.avaje.http.client.HttpClient;
import io.avaje.http.client.JsonbBodyAdapter;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.Executor;
import org.geysermc.floodgate.core.http.api.GlobalApiClient;
import org.geysermc.floodgate.core.http.downloads.DownloadClient;
import org.geysermc.floodgate.core.http.link.GlobalLinkClient;
import org.geysermc.floodgate.core.http.minecraft.MinecraftClient;
import org.geysermc.floodgate.core.http.mojang.SessionServerClient;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.isolation.library.LibraryManager;

@Factory
public class HttpClientFactory {
    @Inject LibraryManager manager;
    @Named("commonPool") Executor pool;

    @Bean
    GlobalApiClient globalApiClient(@Property(name = "http.baseUrl.api") String url) {
        return create(url, GlobalApiClient.class);
    }

    @Bean
    GlobalLinkClient globalLinkClient(@Value("${http.baseUrl.api}/v2/link") String url) {
        return create(url, GlobalLinkClient.class);
    }

    @Bean
    XboxClient xboxClient(@Value("${http.baseUrl.api}/v2/xbox") String url) {
        return create(url, XboxClient.class);
    }

    @Bean
    DownloadClient downloadClient(@Property(name = "http.baseUrl.download") String url) {
        return create(url, DownloadClient.class);
    }

    @Bean
    MinecraftClient minecraftClient() {
        return create("https://api.minecraftservices.com/minecraft", MinecraftClient.class);
    }

    @Bean
    SessionServerClient sessionServerClient() {
        return create("https://sessionserver.mojang.com/session/minecraft", SessionServerClient.class);
    }

    private <T> T create(String baseUrl, Class<T> type) {
        var thread = Thread.currentThread();
        // for both Avaje's jsonb and http-client to work with it's generated classes.
        // The generated classes are in the inner-jar (isolated), and the current context is on the outer-jar
        var context = thread.getContextClassLoader();
        thread.setContextClassLoader(manager.classLoader());

        var result = HttpClient.builder()
                .baseUrl(baseUrl)
                .bodyAdapter(new JsonbBodyAdapter())
                .executor(pool)
                .build()
                .create(type);

        thread.setContextClassLoader(context);
        return result;
    }
}
