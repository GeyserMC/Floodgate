package org.geysermc.floodgate.core.database.loader;

import io.micronaut.context.ApplicationContext;
import java.nio.file.Path;
import org.geysermc.databaseutils.DatabaseUtils;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.sql.SqlDialect;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public final class DatabaseLoader {
    private DatabaseLoader() {}

    public static void load(FloodgateConfig config, LibraryManager manager, Path dataDirectory, ApplicationContext context) {
        var databaseConfig = config.database();
        if (databaseConfig.enabled()) {
            var type = DatabaseType.byId(databaseConfig.type());
            if (type == null) {
                throw new IllegalStateException(
                        "Unable to find database type that matches: " + databaseConfig.type()
                );
            }

            type.libraries().forEach(manager::addLibrary);
            manager.apply();

            try {
                Class.forName("org.h2.Driver", true, manager.classLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            var databaseUtils = DatabaseUtils.builder()
                    .credentialsFile(dataDirectory.resolve("database-config.properties"))
                    .poolName("floodgate")
                    .dialect(SqlDialect.requireByName(config.database().type()))
                    .build();
            context.registerSingleton(databaseUtils);
            for (IRepository<?> repository : databaseUtils.start()) {
                context.registerSingleton(repository);
            }
        }
    }
}
