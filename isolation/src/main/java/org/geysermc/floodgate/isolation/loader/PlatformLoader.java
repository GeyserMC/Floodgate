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

package org.geysermc.floodgate.isolation.loader;

import java.nio.file.Path;
import org.geysermc.floodgate.isolation.library.Library;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.geysermc.floodgate.isolation.library.Repository;
import org.geysermc.floodgate.isolation.util.UrlUtil;

public class PlatformLoader {

    //TODO: Mixins need to be loaded *before* we try to enable/download Floodgate...
    public static LibraryManager createLibraryManager(ClassLoader loader, Path cacheDirectory) {
        return new LibraryManager(loader, cacheDirectory, true)
                .addLibrary(
                        Library.builder()
                                .id("platform-base")
                                .repository(Repository.BUNDLED)
                                .artifactId("platform-base")
                                .forceOverride(true)
                                .build()
                )
                .apply();
    }

    public static PlatformHolder load(LibraryManager manager) {
        var classLoader = manager.classLoader();

        String mainClassString =
                UrlUtil.readSingleLine(classLoader.getResource("org.geysermc.mainClass"));

        try {
            var mainClass = classLoader.loadClass(mainClassString);
            return new PlatformHolder(mainClass, manager);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load platform!", exception);
        }
    }

    public static PlatformHolder loadDefault(ClassLoader pluginClassLoader, Path cacheDirectory) {
        return load(createLibraryManager(pluginClassLoader, cacheDirectory));
    }
}
