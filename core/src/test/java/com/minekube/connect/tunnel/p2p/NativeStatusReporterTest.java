package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.platform.util.PlatformUtils;
import io.libp2p.core.Host;
import java.util.Collections;
import minekube.connect.v1alpha1.ConnectLibp2P.StatusReport;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterResult;
import org.junit.jupiter.api.Test;

class NativeStatusReporterTest {

    @Test
    void buildsGenericJavaStatusReport() {
        PlatformUtils platformUtils = mock(PlatformUtils.class);
        when(platformUtils.minecraftVersion()).thenReturn("1.21.11");
        when(platformUtils.serverImplementationName()).thenReturn("Paper");
        when(platformUtils.getPlayerCount()).thenReturn(4);

        NativeStatusReporter reporter = new NativeStatusReporter(
                mock(Host.class),
                "12D3Endpoint",
                Collections.singletonList("/ip4/127.0.0.1/tcp/4001/p2p/12D3Moxy"),
                PeerRegisterResult.newBuilder()
                        .setEndpointId("endpoint-id")
                        .setEndpointHash("endpoint-hash")
                        .setKvRevision(10)
                        .build(),
                platformUtils,
                mock(ConnectLogger.class));

        StatusReport report = reporter.buildReport(1_000);

        assertEquals("endpoint-id", report.getEndpointId());
        assertEquals("endpoint-hash", report.getEndpointHash());
        assertEquals("12D3Endpoint", report.getEndpointInstanceId());
        assertEquals("12D3Endpoint", report.getEndpointPeerId());
        assertEquals(1_000, report.getObservedAtUnixMs());
        assertEquals(1, report.getStatusesCount());
        assertEquals("java", report.getStatuses(0).getEdition());
        assertEquals(NativeStatusReporter.GENERIC_HOST, report.getStatuses(0).getRequestedHost());
        assertEquals(25565, report.getStatuses(0).getRequestedPort());
        assertEquals(0, report.getStatuses(0).getProtocol());
        assertEquals("1.21.11", report.getStatuses(0).getVersionName());
        assertEquals(4, report.getStatuses(0).getPlayersOnline());
        assertEquals(512, report.getStatuses(0).getPlayersMax());
        assertEquals("{\"text\":\"Paper\"}", report.getStatuses(0).getDescriptionJson());
        assertTrue(report.getStatuses(0).getExpiresAtUnixMs() > report.getObservedAtUnixMs());
    }
}
