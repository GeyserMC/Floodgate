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

package org.geysermc.floodgate.isolation.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UrlUtil {
    public static List<String> readAllLines(URL url) {
        try {
            var connection = url.openConnection();
            var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            var list = new ArrayList<String>();
            String line;

            while ((line = reader.readLine()) != null) {
                list.add(line);
            }

            reader.close();
            return list;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read url %s".formatted(url), exception);
        }
    }
    public static String readSingleLine(URL url) {
        var lines = readAllLines(url);
        if (lines.size() != 1) {
            throw new IllegalStateException(
                    "Url %s didn't return one line of data, got %s".formatted(url, lines.size())
            );
        }
        return lines.get(0);
    }
}
