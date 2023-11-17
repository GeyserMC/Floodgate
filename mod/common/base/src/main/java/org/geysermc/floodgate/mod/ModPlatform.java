package org.geysermc.floodgate.mod;

import io.micronaut.context.ApplicationContext;
import lombok.Getter;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public abstract class ModPlatform extends FloodgatePlatform {

    @Getter
    protected ApplicationContext context;

    protected ModPlatform(LibraryManager manager) {
        super(manager);
    }

    @Override
    public void enable() throws RuntimeException {
        super.enable();
    }

    @Override
    public boolean isProxy() {
        return false;
    }
}
