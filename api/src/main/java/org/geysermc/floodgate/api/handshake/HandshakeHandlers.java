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

package org.geysermc.floodgate.api.handshake;

/**
 * @deprecated Handshake handlers will be removed with the launch of Floodgate 3.0. Please look at
 * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
 */
@Deprecated
public interface HandshakeHandlers {
    /**
     * Register a custom handshake handler. This can be used to check and edit the player during the
     * handshake handling.
     *
     * @param handshakeHandler the handshake handler to register
     * @return a random (unique) int to identify this handshake handler or -1 if null
     */
    int addHandshakeHandler(HandshakeHandler handshakeHandler);

    /**
     * Removes a custom handshake handler by id.
     *
     * @param handshakeHandlerId the id of the handshake handler to remove
     */
    void removeHandshakeHandler(int handshakeHandlerId);

    /**
     * Remove a custom handshake handler by instance.
     *
     * @param handshakeHandler the instance to remove
     */
    void removeHandshakeHandler(Class<? extends HandshakeHandler> handshakeHandler);
}
