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

package org.geysermc.floodgate.core.crypto.topping;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class Base64Topping implements Topping {
    private static final byte SEPARATOR = 0x21;

    @Override
    public ByteBuffer encode(List<ByteBuffer> dataSections) {
        List<ByteBuffer> sections = new ArrayList<>(dataSections);

        int bufferLength = 0;
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            var encodedSection = encodeSection(section);
            bufferLength += encodedSection.remaining();
            if (i > 0) bufferLength += 1; // Separator
            sections.set(i, encodedSection);
        }

        var buffer = ByteBuffer.allocate(bufferLength);
        for (ByteBuffer section : sections) {
            if (buffer.position() != 0) {
                buffer.put(SEPARATOR);
            }
            buffer.put(section);
        }
        // reset position after filling it
        buffer.position(0);

        return buffer;
    }

    @Override
    public List<ByteBuffer> decode(ByteBuffer data) {
        var sections = new ArrayList<ByteBuffer>();

        int previousPosition = data.position();
        while (data.hasRemaining()) {
            if (data.get() == SEPARATOR) {
                int separatorPosition = data.position() - 1;
                sections.add(decodeSection(data, previousPosition, separatorPosition));
                // don't include the separator
                previousPosition = data.position();
            }
        }
        // The remaining data is also a section
        if (data.position() - previousPosition > 0) {
            sections.add(decodeSection(data, previousPosition, data.position()));
        }

        return sections;
    }

    private ByteBuffer decodeSection(ByteBuffer buffer, int startPosition, int endPosition) {
        return Base64.getUrlDecoder().decode(buffer.slice(startPosition, endPosition - startPosition));
    }

    private ByteBuffer encodeSection(ByteBuffer section) {
        return Base64.getUrlEncoder().encode(section);
    }
}
