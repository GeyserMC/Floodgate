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

package org.geysermc.floodgate.universal.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import org.geysermc.floodgate.universal.util.Constants;

public class FloodgateLoader {
  public static final class LoadResult {
    public final URLClassLoader classLoader;
    public final Class<?> pluginClass;

    public LoadResult(URLClassLoader classLoader, Class<?> pluginClass) {
      this.classLoader = classLoader;
      this.pluginClass = pluginClass;
    }
  }

  public static LoadResult load(Path dataDirectory, String platformName) throws Exception {
    URL pluginUrl = Constants.cachedPluginPath(dataDirectory, platformName).toUri().toURL();

    URLClassLoader loader = new URLClassLoader(
        new URL[]{pluginUrl},
        FloodgateLoader.class.getClassLoader()
    );

    String mainClassName = Character.toUpperCase(platformName.charAt(0)) +
        platformName.substring(1) + "Platform";

    return new LoadResult(loader, loader.loadClass("org.geysermc.floodgate." + mainClassName));
  }
}
