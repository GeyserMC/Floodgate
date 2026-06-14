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

package com.minekube.connect.tunnel;

import com.google.inject.Inject;
import com.minekube.connect.tunnel.TunnelConn.Handler;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;

public class Tunneler implements Closeable {

    private final Map<Type, TunnelClientTransport> transports;

    @Inject
    public Tunneler(Set<TunnelClientTransport> transports) {
        this.transports = new EnumMap<>(Type.class);
        for (TunnelClientTransport transport : transports) {
            this.transports.put(transport.type(), transport);
        }
    }

    public Tunneler(TunnelClientTransport transport) {
        this(Collections.singleton(transport));
    }

    public void prepare(Session session) {
        for (SelectedTransport selected : select(session)) {
            selected.transport.prepare(selected.address);
        }
    }

    public TunnelConn tunnel(final String tunnelServiceAddr, String sessionId, Handler handler) {
        TunnelClientTransport transport = transports.get(Type.TYPE_WEBSOCKET);
        if (transport == null) {
            throw new IllegalStateException("no websocket tunnel transport configured");
        }
        return transport.tunnel(tunnelServiceAddr, sessionId, handler);
    }

    public TunnelConn tunnel(Session session, Handler handler) {
        RuntimeException lastFailure = null;
        for (SelectedTransport selected : select(session)) {
            try {
                return selected.transport.tunnel(selected.address, session.getId(), handler);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IllegalStateException("no compatible tunnel transport configured");
    }

    @Override
    public void close() {
        for (TunnelClientTransport transport : transports.values()) {
            transport.close();
        }
    }

    private List<SelectedTransport> select(Session session) {
        List<SelectedTransport> selected = new ArrayList<>();
        addAdvertisedTransport(session, selected, Type.TYPE_LIBP2P);
        addAdvertisedTransport(session, selected, Type.TYPE_WEBSOCKET);

        TunnelClientTransport websocket = transports.get(Type.TYPE_WEBSOCKET);
        if (websocket != null && !session.getTunnelServiceAddr().isEmpty()) {
            selected.add(new SelectedTransport(websocket, session.getTunnelServiceAddr()));
        }
        return selected;
    }

    private void addAdvertisedTransport(
            Session session,
            List<SelectedTransport> selected,
            Type type
    ) {
        TunnelClientTransport transport = transports.get(type);
        if (transport == null) {
            return;
        }
        for (TunnelTransport tunnelTransport : session.getTunnelTransportsList()) {
            if (tunnelTransport.getType() == type && !tunnelTransport.getAddress().isEmpty()) {
                selected.add(new SelectedTransport(transport, tunnelTransport.getAddress()));
            }
        }
    }

    private static final class SelectedTransport {
        private final TunnelClientTransport transport;
        private final String address;

        private SelectedTransport(TunnelClientTransport transport, String address) {
            this.transport = transport;
            this.address = address;
        }
    }
}
