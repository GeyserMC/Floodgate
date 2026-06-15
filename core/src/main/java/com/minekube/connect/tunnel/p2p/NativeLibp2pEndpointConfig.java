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
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class NativeLibp2pEndpointConfig {
    static final String MOXY_ADDR_ENV = "CONNECT_LIBP2P_NATIVE_MOXY_ADDR";
    static final String LISTEN_ADDR_ENV = "CONNECT_LIBP2P_NATIVE_LISTEN_ADDR";
    static final String ADVERTISE_ADDRS_ENV = "CONNECT_LIBP2P_NATIVE_ADVERTISE_ADDRS";

    private final List<String> registerAddrs;
    private final List<String> listenAddrs;
    private final List<String> advertiseAddrs;

    private NativeLibp2pEndpointConfig(
            List<String> registerAddrs,
            List<String> listenAddrs,
            List<String> advertiseAddrs) {
        this.registerAddrs = registerAddrs;
        this.listenAddrs = listenAddrs;
        this.advertiseAddrs = advertiseAddrs;
    }

    static NativeLibp2pEndpointConfig fromEnvironment(Map<String, String> env) {
        List<String> registerAddrs = split(env.get(MOXY_ADDR_ENV));
        List<String> listenAddrs = split(env.get(LISTEN_ADDR_ENV));
        if (listenAddrs.isEmpty()) {
            listenAddrs = Collections.singletonList("/ip4/127.0.0.1/tcp/0");
        }
        return new NativeLibp2pEndpointConfig(
                registerAddrs,
                listenAddrs,
                split(env.get(ADVERTISE_ADDRS_ENV)));
    }

    static NativeLibp2pEndpointConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    boolean enabled() {
        return !registerAddrs.isEmpty();
    }

    List<String> registerAddrs() {
        return registerAddrs;
    }

    List<String> listenAddrs() {
        return listenAddrs;
    }

    List<String> advertiseAddrs() {
        return advertiseAddrs;
    }

    private static List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }
}
