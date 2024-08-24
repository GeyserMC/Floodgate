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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.util.NamingSchemes;

public final class ConfigAsPropertySource {
    private ConfigAsPropertySource() {}

    public static PropertySource toPropertySource(ConfigurationNode rootNode) {
        Objects.requireNonNull(rootNode);

        var root = CommentedConfigurationNode.root();
        root.node("config").from(rootNode.copy());

        return PropertySource.of(flatten(root));
    }

    private static Map<String, Object> flatten(ConfigurationNode node) {
        var result = new HashMap<String, Object>();
        node.childrenMap().values().forEach(value -> {
            // we expect the properties in camelCase
            var key = NamingSchemes.CAMEL_CASE.coerce(value.key().toString());

            if (value.isMap()) {
                flatten(value).forEach((childKey, childValue) -> result.put(key + "." + childKey, childValue));
                return;
            }
            result.put(key, value.raw());
        });
        return result;
    }
}
