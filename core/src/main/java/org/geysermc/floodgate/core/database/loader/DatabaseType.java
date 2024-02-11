/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.core.database.loader;

import java.util.Locale;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.isolation.library.Library;
import org.geysermc.floodgate.isolation.library.Repository;

public enum DatabaseType {
    H2(hikariCp(), library("h2", Repository.MAVEN_CENTRAL, "com.h2database", "h2", "2.2.224"));

    private static final DatabaseType[] VALUES = values();
    private final Set<Library> libraries;

    DatabaseType(@NonNull Library... libraries) {
        this.libraries = Set.of(libraries);
    }

    static Library hikariCp() {
        return library("HikariCP", Repository.MAVEN_CENTRAL, "com.zaxxer", "HikariCP", "5.1.0");
    }

    static Library library(
            String id,
            Repository repo,
            String groupId,
            String artifactId,
            String version
    ) {
        return Library.builder()
                .id(id)
                .repository(repo)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();
    }

    public static DatabaseType byId(String id) {
        id = id.toUpperCase(Locale.ROOT);
        for (DatabaseType value : VALUES) {
            if (value.name().equals(id)) {
                return value;
            }
        }
        return null;
    }

    public Set<Library> libraries() {
        return libraries;
    }
}
