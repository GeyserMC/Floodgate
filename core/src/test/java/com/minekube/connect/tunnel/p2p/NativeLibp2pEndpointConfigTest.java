package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NativeLibp2pEndpointConfigTest {

    @Test
    void disabledWhenRegisterAddressIsMissing() {
        NativeLibp2pEndpointConfig config = NativeLibp2pEndpointConfig.fromEnvironment(Map.of());

        assertFalse(config.enabled());
    }

    @Test
    void parsesRegisterListenAndAdvertiseAddresses() {
        NativeLibp2pEndpointConfig config = NativeLibp2pEndpointConfig.fromEnvironment(Map.of(
                "CONNECT_LIBP2P_NATIVE_MOXY_ADDR", " /ip4/127.0.0.1/tcp/1/p2p/a , /ip4/127.0.0.1/tcp/2/p2p/b ",
                "CONNECT_LIBP2P_NATIVE_LISTEN_ADDR", "/ip4/127.0.0.1/tcp/0",
                "CONNECT_LIBP2P_NATIVE_ADVERTISE_ADDRS", "/ip4/127.0.0.1/tcp/1234/p2p/c"));

        assertTrue(config.enabled());
        assertEquals(2, config.registerAddrs().size());
        assertEquals("/ip4/127.0.0.1/tcp/0", config.listenAddrs().get(0));
        assertEquals("/ip4/127.0.0.1/tcp/1234/p2p/c", config.advertiseAddrs().get(0));
    }
}
