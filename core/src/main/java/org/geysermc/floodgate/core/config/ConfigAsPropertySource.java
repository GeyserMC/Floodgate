/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.core.config;

import io.micronaut.context.env.PropertySource;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.geysermc.configutils.node.codec.strategy.object.ProxyEmbodimentStrategy;

public final class ConfigAsPropertySource {
    private ConfigAsPropertySource() {}

    public static PropertySource toPropertySource(FloodgateConfig config) {
        Objects.requireNonNull(config);
        return PropertySource.of(flatten(Map.of("config", config)));
    }

    private static Map<String, Object> flatten(Map<String, Object> map) {
        return flatten0(Map.of("", map).get(""));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten0(Map<String, Object> map) {
        var result = new HashMap<String, Object>();
        map.forEach((key, value) -> {
            if (Proxy.isProxyClass(value.getClass())) {
                value = new ProxyEmbodimentStrategy().disembody(value);
            }
            if (value instanceof Map<?,?>) {
                flatten0((Map<String, Object>) value).forEach(
                        (key1, value1) -> result.put(key + "." + key1, value1)
                );
                return;
            }
            result.put(key, value);
        });
        return result;
    }
}
