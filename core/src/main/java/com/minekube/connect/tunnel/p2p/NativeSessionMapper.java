/*
 * Copyright (c) 2021-2022 Minekube. https://minekube.com
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
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import minekube.connect.v1alpha1.ConnectLibp2P.SessionGameProfileProperty;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionOffer;
import minekube.connect.v1alpha1.WatchServiceOuterClass;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Authentication;
import minekube.connect.v1alpha1.WatchServiceOuterClass.GameProfile;
import minekube.connect.v1alpha1.WatchServiceOuterClass.GameProfileProperty;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Player;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport;

final class NativeSessionMapper {
    private NativeSessionMapper() {
    }

    static Session toWatchSession(SessionOffer offer) {
        GameProfile.Builder profile = GameProfile.newBuilder();
        if (offer.hasPlayer() && offer.getPlayer().hasProfile()) {
            profile.setId(offer.getPlayer().getProfile().getId())
                    .setName(offer.getPlayer().getProfile().getName());
            for (SessionGameProfileProperty property : offer.getPlayer().getProfile().getPropertiesList()) {
                profile.addProperties(GameProfileProperty.newBuilder()
                        .setName(property.getName())
                        .setValue(property.getValue())
                        .setSignature(property.getSignature()));
            }
        }

        Player.Builder player = Player.newBuilder().setProfile(profile);
        if (offer.hasPlayer()) {
            player.setAddr(offer.getPlayer().getAddr());
        }

        boolean passthrough = offer.hasAuth() && offer.getAuth().getPassthrough();
        return WatchServiceOuterClass.Session.newBuilder()
                .setId(offer.getSessionId())
                .setPlayer(player)
                .setAuth(Authentication.newBuilder().setPassthrough(passthrough))
                .addTunnelTransports(TunnelTransport.newBuilder()
                        .setType(TunnelTransport.Type.TYPE_LIBP2P)
                        .setAddress("same-stream"))
                .build();
    }
}
