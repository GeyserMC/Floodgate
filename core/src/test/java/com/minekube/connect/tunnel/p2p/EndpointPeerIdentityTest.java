package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PubKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndpointPeerIdentityTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsPrivateKeyAndPublishesMarshalPublicKey() throws Exception {
        Path keyFile = tempDir.resolve("native-peer.key");

        EndpointPeerIdentity created = EndpointPeerIdentity.loadOrCreate(keyFile);
        EndpointPeerIdentity loaded = EndpointPeerIdentity.loadOrCreate(keyFile);

        assertTrue(Files.exists(keyFile));
        assertEquals(created.peerId(), loaded.peerId());
        assertEquals(created.publicKeyBase64(), loaded.publicKeyBase64());

        PubKey decodedPublicKey = KeyKt.unmarshalPublicKey(
                Base64.getDecoder().decode(created.publicKeyBase64()));
        assertEquals(created.peerId(), PeerId.fromPubKey(decodedPublicKey).toString());

        byte[] payload = "connect-native-registration".getBytes();
        byte[] signature = loaded.sign(payload);
        assertTrue(decodedPublicKey.verify(payload, signature));
        assertArrayEquals(KeyKt.marshalPrivateKey(created.privateKey()), Files.readAllBytes(keyFile));
    }
}
