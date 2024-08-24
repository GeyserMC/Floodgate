package org.geysermc.floodgate.core.database.loader;

import io.micronaut.context.ApplicationContext;
import java.nio.file.Path;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.databaseutils.DatabaseUtils;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public final class DatabaseLoader {
    private DatabaseLoader() {}

    public static void load(FloodgateConfig config, LibraryManager manager, Path dataDirectory, ApplicationContext context) {
        var databaseConfig = config.database();
        if (!databaseConfig.enabled()) {
            return;
        }

        var type = DatabaseType.byName(databaseConfig.type());
        if (type == null) {
            throw new IllegalStateException("Unsupported database type: " + databaseConfig.type());
        }

        var libraries = DatabaseTypeLibraries.byDatabaseType(type);
        if (libraries == null) {
            throw new IllegalStateException("Unable to find database type that matches: " + type);
        }

        libraries.libraries().forEach(manager::addLibrary);
        manager.apply();

        var databaseUtils = DatabaseUtils.builder()
                .credentialsFile(dataDirectory.resolve("database-config.properties"))
                .poolName("floodgate")
                .type(type)
                .build();

        context.registerSingleton(databaseUtils);
        for (IRepository<?> repository : databaseUtils.start()) {
            context.registerSingleton(repository);
        }
    }
}
