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

package org.geysermc.floodgate.core.crypto.ed25519;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

final class Ed25519DataCodecTest {
    @ParameterizedTest
    @CsvSource({
            "Hello!!, /ywjHxqp70GbTITLypZeovSG8YQs3xrv7BTP6tb9QIAD7J+xuH4E768nmv5QylvCk9Iai/t+1k/y5JCSLR3PAw==, MCowBQYDK2VwAyEA3lAOkEnih8ucexsKDtJb+eiwmMZgSLcCWNjSm+RCF9w=",
            "What's up?, tGGiuu+114Ta2OGCdj8Ntpu3m3chhGG6NQN1WMXpg+5XGwO8efmpyU6ISlmqRY1+mCSmXQhS1VKXfvCaCelXAA==, MCowBQYDK2VwAyEAepV2vnvTx68hraRTtF8jKK8POX/60i3jVMHc9BxEVkE="
    })
    void decode(ArgumentsAccessor arguments) throws Exception {
        var messageBytes = arguments.getString(0).getBytes(StandardCharsets.UTF_8);
        var signature = Base64.getDecoder().decode(arguments.getString(1));
        var encodedPublicKey = arguments.getString(2).getBytes(StandardCharsets.UTF_8);

        var keyCodec = new Ed25519KeyCodec();
        var publicKey = (PublicKey) keyCodec.decode(encodedPublicKey, false);

        var codec = new Ed25519DataCodec();
        codec.init(new KeyPair(publicKey, null));

        var decoded = codec.decode(List.of(ByteBuffer.wrap(messageBytes), ByteBuffer.wrap(signature)));
        assertEquals(0, ByteBuffer.wrap(messageBytes).compareTo(decoded));
        assertThrowsExactly(IllegalStateException.class, () -> codec.encode(ByteBuffer.wrap(messageBytes)));
    }
    @ParameterizedTest
    @CsvSource({
            "Hello!!, /ywjHxqp70GbTITLypZeovSG8YQs3xrv7BTP6tb9QIAD7J+xuH4E768nmv5QylvCk9Iai/t+1k/y5JCSLR3PAw==, MC4CAQAwBQYDK2VwBCIEINFyuLU8O7U/w4nkC5RCzTb2BnlUy8kjo1jwCkluqgYZ",
            "What's up?, tGGiuu+114Ta2OGCdj8Ntpu3m3chhGG6NQN1WMXpg+5XGwO8efmpyU6ISlmqRY1+mCSmXQhS1VKXfvCaCelXAA==, MC4CAQAwBQYDK2VwBCIEII49NjdgnRL/EZsat0qsx1owAkEMj3rtLbNpjd9mbKSf"
    })
    void encode(ArgumentsAccessor arguments) throws Exception {
        var messageBytes = arguments.getString(0).getBytes(StandardCharsets.UTF_8);
        var signature = Base64.getDecoder().decode(arguments.getString(1));
        var encodedPrivateKey = arguments.getString(2).getBytes(StandardCharsets.UTF_8);

        var keyCodec = new Ed25519KeyCodec();
        var privateKey = (PrivateKey) keyCodec.decode(encodedPrivateKey, true);

        var codec = new Ed25519DataCodec();
        codec.init(new KeyPair(null, privateKey));

        var encoded = codec.encode(ByteBuffer.wrap(messageBytes));
        assertEquals(messageBytes.length, encoded.get(0).remaining());
        assertEquals(0, ByteBuffer.wrap(signature).compareTo(encoded.get(1)));
        assertThrowsExactly(
                IllegalStateException.class,
                () -> codec.decode(List.of(ByteBuffer.wrap(messageBytes), ByteBuffer.wrap(signature)))
        );
    }

    @ParameterizedTest
    @CsvSource({
            "Hello!!, MC4CAQAwBQYDK2VwBCIEINFyuLU8O7U/w4nkC5RCzTb2BnlUy8kjo1jwCkluqgYZ, MCowBQYDK2VwAyEA3lAOkEnih8ucexsKDtJb+eiwmMZgSLcCWNjSm+RCF9w=",
            "What's up?, MC4CAQAwBQYDK2VwBCIEII49NjdgnRL/EZsat0qsx1owAkEMj3rtLbNpjd9mbKSf, MCowBQYDK2VwAyEAepV2vnvTx68hraRTtF8jKK8POX/60i3jVMHc9BxEVkE="
    })
    void roundtrip(ArgumentsAccessor arguments) throws Exception {
        var messageBuffer = ByteBuffer.wrap(arguments.getString(0).getBytes(StandardCharsets.UTF_8));
        var encodedPrivateKey = arguments.getString(1).getBytes(StandardCharsets.UTF_8);
        var encodedPublicKey = arguments.getString(2).getBytes(StandardCharsets.UTF_8);

        var keyCodec = new Ed25519KeyCodec();
        var privateKey = (PrivateKey) keyCodec.decode(encodedPrivateKey, true);
        var publicKey = (PublicKey) keyCodec.decode(encodedPublicKey, false);

        var codec = new Ed25519DataCodec();
        codec.init(new KeyPair(publicKey, privateKey));

        var encoded = codec.encode(messageBuffer);
        assertEquals(0, messageBuffer.remaining()); // input should be consumed
        assertEquals(2, encoded.size()); // data and signature

        messageBuffer.position(0); // reset input so we can verify it
        assertEquals(0, messageBuffer.compareTo(encoded.get(0))); // data should be equal

        var decoded = codec.decode(List.of(messageBuffer, encoded.get(1)));
        // input should be consumed
        assertEquals(0, messageBuffer.remaining());
        assertEquals(0, encoded.get(1).remaining());
        assertNotEquals(0, decoded.remaining()); // output should not be consumed
        messageBuffer.position(0); // reset input so we can verify it

        assertEquals(0, messageBuffer.compareTo(decoded));
    }
}
