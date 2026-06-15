package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PubKey;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import minekube.connect.v1alpha1.ConnectLibp2P.EndpointPeerRecord;
import minekube.connect.v1alpha1.ConnectLibp2P.OfflineMode;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerCapacity;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterChallenge;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterCommit;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterInit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PeerRegistrationHandshakeTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsInitAndSignedCommitFromChallenge() throws Exception {
        EndpointPeerIdentity identity = EndpointPeerIdentity.loadOrCreate(tempDir.resolve("native-peer.key"));
        PeerRegistrationHandshake handshake = new PeerRegistrationHandshake(
                identity,
                "endpoint",
                "token",
                "instance",
                Collections.singletonList("parent"),
                OfflineMode.OFFLINE_MODE_ALLOWED,
                Arrays.asList("session", "status"),
                PeerCapacity.newBuilder().setMaxSessions(100).setActiveSessions(3).build());

        PeerRegisterInit init = handshake.init(Arrays.asList(
                "/ip4/127.0.0.1/tcp/1234/p2p/" + identity.peerId()));

        assertEquals("endpoint", init.getEndpoint());
        assertEquals("token", init.getToken());
        assertEquals("instance", init.getEndpointInstanceId());
        assertEquals(identity.peerId(), init.getEndpointPeerId());
        assertEquals(identity.publicKeyBase64(), init.getEndpointPublicKey());
        assertEquals(OfflineMode.OFFLINE_MODE_ALLOWED, init.getOfflineMode());
        assertEquals(Arrays.asList("session", "status"), init.getCapabilitiesList());

        PeerRegisterChallenge challenge = PeerRegisterChallenge.newBuilder()
                .setEndpointId("endpoint-id")
                .setEndpointHash("endpoint-hash")
                .setPublisherId("publisher")
                .setPublisherPeerId("publisher-peer")
                .setRegion("local")
                .setKvTtlMs(45_000)
                .setNonce(ByteString.copyFromUtf8("nonce"))
                .build();
        PeerRegisterCommit commit = handshake.commit(challenge, init.getObservedAddrsList(), 7, 1_000);

        EndpointPeerRecord record = commit.getRecord();
        assertEquals("endpoint", record.getEndpoint());
        assertEquals("endpoint-hash", record.getEndpointHash());
        assertEquals("endpoint-id", record.getEndpointId());
        assertEquals("publisher", record.getPublisherId());
        assertEquals("publisher-peer", record.getPublisherPeerId());
        assertEquals("local", record.getRegion());
        assertEquals(Arrays.asList("session", "status"), record.getCapabilitiesList());
        assertEquals(init.getObservedAddrsList(), record.getAddrsList());
        assertArrayEquals(challenge.getNonce().toByteArray(), record.getNonce().toByteArray());
        assertEquals(7, record.getSequence());
        assertEquals(1_000, record.getIssuedAtUnixMs());
        assertEquals(1_000, record.getRenewedAtUnixMs());
        assertEquals(46_000, record.getExpiresAtUnixMs());

        PubKey publicKey = KeyKt.unmarshalPublicKey(Base64.getDecoder().decode(identity.publicKeyBase64()));
        assertTrue(publicKey.verify(
                PeerRecordSigningPayload.bytes(record),
                commit.getSignature().toByteArray()));
    }
}
