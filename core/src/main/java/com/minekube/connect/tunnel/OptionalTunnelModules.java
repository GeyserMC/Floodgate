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

import com.google.inject.Module;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OptionalTunnelModules {
    private static final String LIBP2P_MODULE =
            "com.minekube.connect.tunnel.p2p.Libp2pTunnelModule";

    private OptionalTunnelModules() {
    }

    public static Module[] append(Module... modules) {
        List<Module> all = new ArrayList<>(Arrays.asList(modules));
        Module libp2p = load(LIBP2P_MODULE);
        if (libp2p != null) {
            all.add(libp2p);
        }
        return all.toArray(new Module[0]);
    }

    private static Module load(String className) {
        if (javaFeatureVersion() < 11) {
            return null;
        }
        try {
            Class<?> type = Class.forName(className);
            if (!Module.class.isAssignableFrom(type)) {
                return null;
            }
            Constructor<?> constructor = type.getDeclaredConstructor();
            return (Module) constructor.newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("failed to load optional tunnel module " + className, e);
        }
    }

    private static int javaFeatureVersion() {
        String spec = System.getProperty("java.specification.version", "8");
        if (spec.startsWith("1.")) {
            return Integer.parseInt(spec.substring(2));
        }
        int dot = spec.indexOf('.');
        if (dot >= 0) {
            spec = spec.substring(0, dot);
        }
        return Integer.parseInt(spec);
    }
}
