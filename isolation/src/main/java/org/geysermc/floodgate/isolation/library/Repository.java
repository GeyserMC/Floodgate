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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geysermc.floodgate.isolation.util.HttpUtil;
import org.geysermc.floodgate.isolation.util.HttpUtil.HttpResponse;
import org.geysermc.floodgate.isolation.util.StreamUtil;

public enum Repository {
    MAVEN_CENTRAL("https://repo1.maven.org/maven2/"),
    OPEN_COLLAB("https://repo.opencollab.dev/main/"),
    BUNDLED(null) {
        @Override
        public byte[] download(Library library) {
            var loader = getClass().getClassLoader();
            try (var resource = loader.getResourceAsStream("bundled/" + library.id() + ".jar")) {
                return StreamUtil.readStream(resource);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not load bundled jar " + library.id(),
                        exception
                );
            }
        }
    };

    private final String baseUrl;

    Repository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private byte[] justDownload(Library library) {
        try {
            HttpResponse<byte[]> result = HttpUtil.getRawData(baseUrl + library.path());

            if (!result.isCodeOk()) {
                throw new RuntimeException(String.format(
                        "Got an invalid response code (%s) while downloading library %s",
                        result.httpCode(), library.id()
                ));
            }

            return result.response();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download library " + library.id(), exception);
        }
    }

    public byte[] download(Library library) {
        byte[] data = justDownload(library);
        library.validateChecksum(data);
        return data;
    }

    public void downloadTo(Library library, Path location) {
        try {
            // delete old versions of a given library
            if (Files.isDirectory(location.getParent())) {
                try (var list = Files.list(location.getParent())) {
                    list.forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    "Could not delete old version of library " + library.id(),
                                    exception
                            );
                        }
                    });
                }
            }

            Files.createDirectories(location.getParent());
            Files.write(location, download(library));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save library!", exception);
        }
    }
}
