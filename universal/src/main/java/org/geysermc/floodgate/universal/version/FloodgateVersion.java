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

package org.geysermc.floodgate.universal.version;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geysermc.floodgate.universal.util.Constants;
import org.geysermc.floodgate.universal.util.FileUtils;
import org.geysermc.floodgate.universal.util.HttpClient;

public final class FloodgateVersion {
  private static final Gson GSON = new Gson();

  public static final class VersionResult {
    public final String versionIdentifier;
    public final String downloadUrl;

    public VersionResult(String versionIdentifier, String downloadUrl) {
      this.versionIdentifier = versionIdentifier;
      this.downloadUrl = downloadUrl;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof VersionResult && versionIdentifier != null &&
          versionIdentifier.equals(((VersionResult) obj).versionIdentifier);
    }
  }

  public static VersionResult currentVersion(Path dataDirectory) throws Exception {
    Path pluginVersionPath = Constants.cachedPluginVersionPath(dataDirectory);
    if (!Files.exists(pluginVersionPath)) {
      return null;
    }

    Reader reader = Files.newBufferedReader(pluginVersionPath);
    return GSON.fromJson(reader, VersionResult.class);
  }

  public static VersionResult retrieveLatestVersion(String platformName) {
    String fullName = String.format(Constants.PLUGIN_NAME_FORMAT, platformName);
    JsonObject buildInfo = HttpClient.getInstance().get(String.format(
        "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/%s/lastSuccessfulBuild/api/json",
        Constants.GIT_BRANCH
    )).getResponse();

    String downloadUrl = buildInfo.get("url").getAsString() + "artifact/";
    for (JsonElement artifactElement : buildInfo.getAsJsonArray("artifacts")) {
      JsonObject artifact = artifactElement.getAsJsonObject();
      if (artifact.get("fileName").getAsString().equals(fullName)) {
        downloadUrl += artifact.get("relativePath").getAsString();
        break;
      }
    }

    return new VersionResult(buildInfo.get("number").getAsString(), downloadUrl);
  }

  public static void writeVersion(Path dataDirectory, VersionResult downloadedVersion) {
    FileUtils.writeToPath(
        Constants.cachedPluginVersionPath(dataDirectory),
        GSON.toJson(downloadedVersion).getBytes(StandardCharsets.UTF_8)
    );
  }
}
