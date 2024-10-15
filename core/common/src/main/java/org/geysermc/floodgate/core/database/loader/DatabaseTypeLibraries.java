/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.database.loader;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.floodgate.isolation.library.Library;
import org.geysermc.floodgate.isolation.library.Repository;

public enum DatabaseTypeLibraries {
    H2(hikariCp(), library("h2", Repository.MAVEN_CENTRAL, "com.h2database", "h2", "2.2.224")),
    SQLITE(hikariCp(), library("sqlite", Repository.MAVEN_CENTRAL, "org.xerial", "sqlite-jdbc", "3.46.1.0"));

    private final Set<Library> libraries;

    DatabaseTypeLibraries(@NonNull Library... libraries) {
        this.libraries = Set.of(libraries);
    }

    static Library hikariCp() {
        return library("HikariCP", Repository.MAVEN_CENTRAL, "com.zaxxer", "HikariCP", "5.1.0");
    }

    static Library library(String id, Repository repo, String groupId, String artifactId, String version) {
        return Library.builder()
                .id(id)
                .repository(repo)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();
    }

    public static DatabaseTypeLibraries byDatabaseType(DatabaseType type) {
        return switch (type) {
            case H2 -> H2;
            case SQLITE -> SQLITE;
            default -> null;
        };
    }

    public Set<Library> libraries() {
        return libraries;
    }
}
