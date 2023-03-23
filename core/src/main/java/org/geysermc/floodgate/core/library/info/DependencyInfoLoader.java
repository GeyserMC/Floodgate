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

package org.geysermc.floodgate.core.library.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class DependencyInfoLoader {
    private final Map<String, DependencyInfo> infoMap = new HashMap<>();

    private DependencyInfoLoader() {}

    public static DependencyInfoLoader load(URL infoUrl) {
        var loader = new DependencyInfoLoader();

        try (var reader = new BufferedReader(new InputStreamReader(infoUrl.openStream()))) {
            reader.lines().forEach(line -> {
                var info = DependencyInfo.fromString(line);
                loader.infoMap.put(info.groupId() + ":" + info.artifactId(), info);
            });
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return loader;
    }

    public DependencyInfo byCombinedId(String groupId, String artifactId) {
        return infoMap.get(groupId + ":" + artifactId);
    }
}
