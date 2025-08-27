package org.geysermc.floodgate.mod;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.FloodgatePlatform;

import java.nio.file.Path;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public abstract class ModPlatform extends FloodgatePlatform {

    protected ModPlatform(LibraryManager manager) {
        super(manager);
    }

    public @Nullable abstract Path resourcePath(String file);

    public abstract boolean isClient();

    @Override
    public boolean isProxy() {
        return false;
    }
}
