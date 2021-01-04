/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.skin;

import java.awt.image.BufferedImage;
import org.geysermc.floodgate.util.RawSkin;

public class SkinUtils {
    /**
     * Get the ARGB int for a given index in some image data
     *
     * @param index Index to get
     * @param data  Image data to find in
     * @return An int representing ARGB
     */
    private static int getARGB(int index, byte[] data) {
        return (data[index + 3] & 0xFF) << 24 | (data[index] & 0xFF) << 16 |
                (data[index + 1] & 0xFF) << 8 | (data[index + 2] & 0xFF);
    }

    /**
     * Convert a byte[] to a BufferedImage
     *
     * @param imageData   The byte[] to convert
     * @param width  The width of the target image
     * @param height The height of the target image
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(byte[] imageData, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, getARGB((y * width + x) * 4, imageData));
            }
        }
        return image;
    }

    public static BufferedImage toBufferedImage(RawSkin rawSkin) {
        return toBufferedImage(rawSkin.data, rawSkin.width, rawSkin.height);
    }
}
