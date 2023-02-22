/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.universal.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Constants {
  public static final String GIT_BRANCH = "${branch}";

  public static final Path CACHE_PATH = Paths.get("cache");
  public static final String PLUGIN_VERSION_FILE_NAME = "plugin_version.json";
  public static final String PLUGIN_NAME_FORMAT = "floodgate-%s.jar";

  public static Path cachedPluginPath(Path dataDirectory, String platformName) {
    return dataDirectory
        .resolve(CACHE_PATH)
        .resolve(String.format(PLUGIN_NAME_FORMAT, platformName));
  }

  public static Path cachedPluginVersionPath(Path dataDirectory) {
    return dataDirectory
        .resolve(CACHE_PATH)
        .resolve(PLUGIN_VERSION_FILE_NAME);
  }

  public static boolean shouldCheck(Path dataDirectory) {
    return Files.notExists(dataDirectory.resolve(".no_version_check"));
  }
}
