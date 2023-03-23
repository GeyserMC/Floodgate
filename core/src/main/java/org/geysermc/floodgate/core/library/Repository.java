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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geysermc.floodgate.core.util.HttpClient;
import org.geysermc.floodgate.core.util.HttpClient.HttpResponse;

public enum Repository {
    MAVEN_CENTRAL("https://repo1.maven.org/maven2/");

    private final String baseUrl;

    Repository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private byte[] justDownload(Library library) {
        try {
            HttpResponse<byte[]> response = new HttpClient().getRawData(baseUrl + library.path());

            if (!response.isCodeOk()) {
                throw new RuntimeException(String.format(
                        "Got an invalid response code (%s) while downloading library %s",
                        response.getHttpCode(), library.id()
                ));
            }

            return response.getResponse();
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
            Files.createDirectories(location.getParent());
            Files.write(location, download(library));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save library!", exception);
        }
    }
}
