package org.geysermc.floodgate.mod;

import io.micronaut.context.ApplicationContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.FloodgatePlatform;

import java.nio.file.Path;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.slf4j.LoggerFactory;

public abstract class ModPlatform extends FloodgatePlatform {

    protected ApplicationContext context;

    protected ModPlatform(LibraryManager manager) {
        super(manager);
    }

    @Override
    protected void onContextCreated(ApplicationContext context) {
        context.registerSingleton(LoggerFactory.getLogger("floodgate"));
        this.context = context;
    }

    public @Nullable abstract Path resourcePath(String file);

    public abstract boolean isClient();

    @Override
    public boolean isProxy() {
        return false;
    }
}
