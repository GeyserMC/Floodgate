package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ByteString;
import minekube.connect.v1alpha1.ConnectLibp2P.EndpointPeerRecord;
import minekube.connect.v1alpha1.ConnectLibp2P.OfflineMode;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerCapacity;
import org.junit.jupiter.api.Test;

class PeerRecordSigningPayloadTest {

    @Test
    void matchesGoPeerRecordJsonFieldOrder() {
        EndpointPeerRecord record = EndpointPeerRecord.newBuilder()
                .setVersion(1)
                .setEndpoint("endpoint")
                .setEndpointHash("hash")
                .setEndpointId("endpoint-id")
                .setEndpointInstanceId("instance")
                .setEndpointPeerId("peer")
                .setEndpointPublicKey("pub")
                .setPublisherId("publisher")
                .setPublisherPeerId("publisher-peer")
                .setRegion("local")
                .addAddrs("/ip4/127.0.0.1/tcp/1/p2p/peer")
                .addDirectAddrs("/ip4/127.0.0.1/tcp/2/p2p/peer")
                .addCapabilities("session")
                .setCapacity(PeerCapacity.newBuilder().setMaxSessions(10).setActiveSessions(2))
                .setOfflineMode(OfflineMode.OFFLINE_MODE_ALLOWED)
                .setSequence(7)
                .setIssuedAtUnixMs(1000)
                .setRenewedAtUnixMs(1100)
                .setExpiresAtUnixMs(2000)
                .setNonce(ByteString.copyFromUtf8("nonce"))
                .build();

        assertEquals("{\"version\":1,\"endpoint\":\"endpoint\",\"endpoint_hash\":\"hash\","
                        + "\"endpoint_id\":\"endpoint-id\",\"endpoint_instance_id\":\"instance\","
                        + "\"endpoint_peer_id\":\"peer\",\"endpoint_public_key\":\"pub\","
                        + "\"publisher_id\":\"publisher\",\"publisher_peer_id\":\"publisher-peer\","
                        + "\"region\":\"local\",\"addrs\":[\"/ip4/127.0.0.1/tcp/1/p2p/peer\"],"
                        + "\"direct_addrs\":[\"/ip4/127.0.0.1/tcp/2/p2p/peer\"],"
                        + "\"capabilities\":[\"session\"],"
                        + "\"capacity\":{\"max_sessions\":10,\"active_sessions\":2},"
                        + "\"offline_mode\":1,\"sequence\":7,\"issued_at_unix_ms\":1000,"
                        + "\"renewed_at_unix_ms\":1100,\"expires_at_unix_ms\":2000,"
                        + "\"nonce\":\"bm9uY2U=\"}",
                PeerRecordSigningPayload.json(record));
    }
}
