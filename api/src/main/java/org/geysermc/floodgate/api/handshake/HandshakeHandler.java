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

package org.geysermc.floodgate.api.handshake;

/**
 * This class allows you to change and/or get specific data of the Bedrock client before Floodgate
 * does something with this data. This means that Floodgate decrypts the data, then calls the
 * handshake handlers and then applies the data to the connection.<br>
 * <br>
 * /!\ Note that this class will be called for both Java and Bedrock connections, but {@link
 * HandshakeData#isFloodgatePlayer()} will be false and Floodgate related methods will return null
 * for Java players
 */
@FunctionalInterface
public interface HandshakeHandler {
    /**
     * Method that will be called during the time that Floodgate handles the handshake.
     *
     * @param data the data usable during the handshake
     */
    void handle(HandshakeData data);
}
