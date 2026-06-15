package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;

import minekube.connect.v1alpha1.ConnectLibp2P.SessionAuthentication;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionGameProfile;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionGameProfileProperty;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionOffer;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionPlayer;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;
import org.junit.jupiter.api.Test;

class NativeSessionMapperTest {

    @Test
    void mapsNativeOfferToExistingWatchSessionShape() {
        SessionOffer offer = SessionOffer.newBuilder()
                .setSessionId("session-1")
                .setEndpoint("endpoint")
                .setAuth(SessionAuthentication.newBuilder().setPassthrough(false))
                .setPlayer(SessionPlayer.newBuilder()
                        .setAddr("127.0.0.1")
                        .setProfile(SessionGameProfile.newBuilder()
                                .setId("00000000-0000-0000-0000-000000000000")
                                .setName("Player")
                                .addProperties(SessionGameProfileProperty.newBuilder()
                                        .setName("textures")
                                        .setValue("value")
                                        .setSignature("sig"))))
                .build();

        Session session = NativeSessionMapper.toWatchSession(offer);

        assertEquals("session-1", session.getId());
        assertEquals("", session.getTunnelServiceAddr());
        assertEquals(false, session.getAuth().getPassthrough());
        assertEquals("127.0.0.1", session.getPlayer().getAddr());
        assertEquals("Player", session.getPlayer().getProfile().getName());
        assertEquals("textures", session.getPlayer().getProfile().getProperties(0).getName());
        assertEquals(Type.TYPE_LIBP2P, session.getTunnelTransports(0).getType());
    }
}
