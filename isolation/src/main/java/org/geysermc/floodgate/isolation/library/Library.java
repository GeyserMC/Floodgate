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

package org.geysermc.floodgate.isolation.library;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.isolation.library.info.DependencyInfo;
import org.geysermc.floodgate.isolation.library.info.DependencyInfoLoader;

public record Library(
        @NonNull String id,
        @NonNull Repository repository,
        String groupId,
        @NonNull String artifactId,
        String version,
        byte[] sha256,
        boolean forceOverride
) {
    public Library {
        Objects.requireNonNull(id);
        Objects.requireNonNull(repository);
        Objects.requireNonNull(artifactId);
        if (repository != Repository.BUNDLED) {
            Objects.requireNonNull(groupId);
            Objects.requireNonNull(version);
        }
    }

    public String path() {
        return "%s/%s/%s/%s-%s.jar".formatted(
                groupId.replace('.', '/'),
                artifactId,
                version,
                artifactId,
                version
        );
    }

    public static LibraryBuilder builder() {
        return builder(null);
    }

    public static LibraryBuilder builder(DependencyInfoLoader info) {
        return new LibraryBuilder(info);
    }

    public Path filePath() {
        var fileName = id();
        if (!forceOverride) {
            fileName += "-" + version();
        }
        return Path.of(id(), fileName + ".jar");
    }

    public void validateChecksum(byte[] data) {
        if (sha256 == null) {
            return;
        }

        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }

        if (Arrays.equals(hash, sha256)) {
            return;
        }

        throw new IllegalStateException(String.format(
                "Downloaded library hash (%s) didn't match expected hash (%s)!",
                Base64.getEncoder().encodeToString(data),
                Base64.getEncoder().encodeToString(sha256())
        ));
    }

    public static class LibraryBuilder {
        private final DependencyInfoLoader dependencyInfoLoader;

        private String id;
        private Repository repository;
        private String groupId;
        private String artifactId;

        private String version;
        private byte[] sha256;
        private boolean forceOverride;

        LibraryBuilder(DependencyInfoLoader info) {
            this.dependencyInfoLoader = info;
        }

        public LibraryBuilder id(String id) {
            this.id = id;
            return this;
        }

        public LibraryBuilder repository(@NonNull Repository repository) {
            this.repository = Objects.requireNonNull(repository);
            return this;
        }

        public LibraryBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public LibraryBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public LibraryBuilder version(String version) {
            this.version = version;
            return this;
        }

        public LibraryBuilder sha256(byte[] sha256) {
            this.sha256 = sha256;
            return this;
        }

        public LibraryBuilder sha256(String sha256) {
            return sha256(Base64.getDecoder().decode(sha256));
        }

        public LibraryBuilder forceOverride(boolean forceOverride) {
            this.forceOverride = forceOverride;
            return this;
        }

        public Library build() {
            if (dependencyInfoLoader != null) {
                DependencyInfo info = dependencyInfoLoader.byCombinedId(groupId, artifactId);
                version = info.version();
                sha256 = info.sha256();
            }
            return new Library(id, repository, groupId, artifactId, version, sha256, forceOverride);
        }
    }
}
