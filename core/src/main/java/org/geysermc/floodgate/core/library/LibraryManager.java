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

package org.geysermc.floodgate.core.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.library.classloader.LibraryClassLoader;

// Credits for the idea of downloading dependencies on runtime go to LuckPerms
public final class LibraryManager {
    private final LibraryClassLoader classLoader;
    private final Path cacheDirectory;

    private final Set<Library> toApply = new HashSet<>();

    public LibraryManager(ClassLoader parent, Path cacheDirectory) {
        this.classLoader = new LibraryClassLoader(parent);
        this.cacheDirectory = cacheDirectory;
    }

    public LibraryManager addLibrary(Library library) {
        toApply.add(library);
        return this;
    }

    public void apply() {
        CompletableFuture.allOf(
                toApply.stream()
                        .map(library -> CompletableFuture.runAsync(() -> loadLibrary(library)))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

    private void loadLibrary(Library library) {
        var libPath = cacheDirectory.resolve(library.filePath());
        if (classLoader.isLoaded(libPath)) {
            return;
        }

        if (!Files.exists(libPath)) {
            library.repository().downloadTo(library, libPath);
        }

        classLoader.addPath(libPath);
    }
}
