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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import minekube.connect.v1alpha1.ConnectLibp2P.EndpointPeerRecord;

final class PeerRecordSigningPayload {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private PeerRecordSigningPayload() {
    }

    static byte[] bytes(EndpointPeerRecord record) {
        return json(record).getBytes(StandardCharsets.UTF_8);
    }

    static String json(EndpointPeerRecord record) {
        StringBuilder out = new StringBuilder();
        out.append('{');
        field(out, "version", Long.toString(record.getVersion()));
        field(out, "endpoint", quote(record.getEndpoint()));
        field(out, "endpoint_hash", quote(record.getEndpointHash()));
        field(out, "endpoint_id", quote(record.getEndpointId()));
        field(out, "endpoint_instance_id", quote(record.getEndpointInstanceId()));
        field(out, "endpoint_peer_id", quote(record.getEndpointPeerId()));
        field(out, "endpoint_public_key", quote(record.getEndpointPublicKey()));
        field(out, "publisher_id", quote(record.getPublisherId()));
        field(out, "publisher_peer_id", quote(record.getPublisherPeerId()));
        field(out, "region", quote(record.getRegion()));
        field(out, "addrs", stringArray(record.getAddrsList()));
        field(out, "direct_addrs", stringArray(record.getDirectAddrsList()));
        field(out, "capabilities", stringArray(record.getCapabilitiesList()));
        field(out, "capacity", "{\"max_sessions\":" + record.getCapacity().getMaxSessions()
                + ",\"active_sessions\":" + record.getCapacity().getActiveSessions() + "}");
        field(out, "offline_mode", Integer.toString(record.getOfflineModeValue()));
        field(out, "sequence", Long.toUnsignedString(record.getSequence()));
        field(out, "issued_at_unix_ms", Long.toString(record.getIssuedAtUnixMs()));
        field(out, "renewed_at_unix_ms", Long.toString(record.getRenewedAtUnixMs()));
        field(out, "expires_at_unix_ms", Long.toString(record.getExpiresAtUnixMs()));
        field(out, "nonce", quote(Base64.getEncoder().encodeToString(record.getNonce().toByteArray())));
        out.append('}');
        return out.toString();
    }

    private static void field(StringBuilder out, String name, String value) {
        if (out.length() > 1) {
            out.append(',');
        }
        out.append(quote(name)).append(':').append(value);
    }

    private static String stringArray(List<String> values) {
        return values.stream().map(PeerRecordSigningPayload::quote)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String quote(String value) {
        return GSON.toJson(value);
    }
}
