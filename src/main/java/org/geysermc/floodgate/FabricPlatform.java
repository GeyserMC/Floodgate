package org.geysermc.floodgate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.geysermc.floodgate.FloodgatePlatform;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.logger.FloodgateLogger;

public final class FabricPlatform extends FloodgatePlatform {
    @Inject
    public FabricPlatform(FloodgateApi api, PlatformInjector platformInjector, FloodgateLogger logger, Injector guice) {
        super(api, platformInjector, logger, guice);
    }
}
