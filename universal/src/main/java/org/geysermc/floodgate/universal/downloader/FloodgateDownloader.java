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

package org.geysermc.floodgate.universal.downloader;

import java.io.IOException;
import java.nio.file.Path;
import org.geysermc.floodgate.universal.util.Constants;
import org.geysermc.floodgate.universal.util.FileUtils;
import org.geysermc.floodgate.universal.util.HttpClient;
import org.geysermc.floodgate.universal.util.HttpClient.HttpResponse;

public class FloodgateDownloader {
  public static void download(Path dataDirectory, String platformName, String downloadUrl) {
    try {
      HttpResponse<byte[]> response = HttpClient.getInstance().getRawData(downloadUrl);

      if (!response.isCodeOk()) {
        throw new RuntimeException(String.format(
            "Got an invalid response code (%s) while downloading Floodgate for %s",
            response.getHttpCode(), platformName
        ));
      }

      FileUtils.writeToPath(
          Constants.cachedPluginPath(dataDirectory, platformName),
          response.getResponse()
      );

    } catch (IOException exception) {
      throw new RuntimeException(
          "Something went wrong while downloading Floodgate for " + platformName,
          exception
      );
    }
  }
}
