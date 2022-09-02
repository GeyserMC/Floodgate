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

package org.geysermc.floodgate.universal;

import java.nio.file.Files;
import java.nio.file.Path;
import org.geysermc.floodgate.universal.downloader.FloodgateDownloader;
import org.geysermc.floodgate.universal.holder.FloodgateHolder;
import org.geysermc.floodgate.universal.loader.FloodgateLoader;
import org.geysermc.floodgate.universal.loader.FloodgateLoader.LoadResult;
import org.geysermc.floodgate.universal.util.Constants;
import org.geysermc.floodgate.universal.util.Messages;
import org.geysermc.floodgate.universal.util.UniversalLogger;
import org.geysermc.floodgate.universal.version.FloodgateVersion;
import org.geysermc.floodgate.universal.version.FloodgateVersion.VersionResult;

public class UniversalLoader {
  private final String platformName;
  private final Path dataDirectory;
  private final UniversalLogger logger;

  private VersionResult currentVersion;

  public UniversalLoader(String platformName, Path dataDirectory, UniversalLogger logger) {
    this.platformName = platformName;
    this.dataDirectory = dataDirectory;
    this.logger = logger;
  }

  public FloodgateHolder start() throws Exception {
    LoadResult result = checkDownloadAndLoad();
    return new FloodgateHolder(result);
  }

  private LoadResult checkDownloadAndLoad() throws Exception {
    currentVersion = FloodgateVersion.currentVersion(dataDirectory);

    boolean cachedPluginFound =
        Files.exists(Constants.cachedPluginPath(dataDirectory, platformName));

    if (currentVersion == null) {
      logger.info(Messages.NOT_CACHED);
      return downloadAndLoad();
    }

    if (Constants.shouldCheck(dataDirectory)) {
      logger.info(cachedPluginFound ? Messages.CHECKING : Messages.NOT_CACHED);
      return downloadAndLoad();
    }

    if (!cachedPluginFound) {
      logger.warn(Messages.FOUND_BUT_NOT_CACHED);
      return downloadAndLoad(null, currentVersion);
    }

    logger.info(Messages.FOUND_NO_CHECKING);
    return load();
  }

  private LoadResult downloadAndLoad() throws Exception {
    VersionResult latestVersion = FloodgateVersion.retrieveLatestVersion(platformName);
    return downloadAndLoad(currentVersion, latestVersion);
  }

  private LoadResult downloadAndLoad(VersionResult currentVersion, VersionResult latestVersion)
      throws Exception {

    boolean cachedPluginFound = currentVersion != null &&
        Files.exists(Constants.cachedPluginPath(dataDirectory, platformName));

    if (cachedPluginFound && currentVersion.equals(latestVersion)) {
      logger.info(Messages.ON_LATEST);
      return load();
    }

    logger.info(String.format(Messages.DOWNLOADING_NOW, latestVersion.versionIdentifier));

    FloodgateDownloader.download(dataDirectory, platformName, latestVersion.downloadUrl);
    FloodgateVersion.writeVersion(dataDirectory, latestVersion);

    logger.info(String.format(Messages.DOWNLOADED_LATEST, latestVersion.versionIdentifier));

    return load();
  }

  private LoadResult load() throws Exception {
    logger.info(String.format(Messages.LOADING_NOW, platformName));
    return FloodgateLoader.load(dataDirectory, platformName);
  }
}
