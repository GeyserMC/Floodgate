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

package org.geysermc.floodgate.core.util;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bstats.MetricsBase;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.json.JsonObjectBuilder;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.config.FloodgateConfig.MetricsConfig;
import org.geysermc.floodgate.core.event.lifecycle.ShutdownEvent;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;

@Singleton
public final class Metrics {
    private final MetricsBase metricsBase;

    @Inject
    Metrics(
            FloodgateConfig config,
            PlatformUtils platformUtils,
            GeyserApiBase api,
            @Named("implementationName") String implementationName,
            FloodgateLogger logger
    ) {
        MetricsConfig metricsConfig = config.metrics();

        metricsBase = new MetricsBase(
                "server-implementation",
                metricsConfig.uuid().toString(),
                Constants.METRICS_ID,
                metricsConfig.enabled(),
                this::appendPlatformData,
                jsonObjectBuilder -> { /* NOP */ },
                null,
                () -> true, // remove this if/when we add some form of reload support
                logger::error,
                logger::info,
                Constants.DEBUG_MODE,
                Constants.DEBUG_MODE,
                Constants.DEBUG_MODE
        );

        metricsBase.addCustomChart(
                new SingleLineChart("players", api::onlineConnectionsCount)
        );

        metricsBase.addCustomChart(
                new DrilldownPie("player_count", () -> {
                    int playerCount = api.onlineConnectionsCount();
                    // 0 = 0 - 4, 9 = 5 - 9, etc.
                    int category = playerCount / 5 * 5;
                    String categoryName = category + " - " + (category + 4);

                    return Collections.singletonMap(
                            implementationName,
                            Collections.singletonMap(categoryName, 1)
                    );
                })
        );

        metricsBase.addCustomChart(
                new SimplePie("authentication",
                        () -> platformUtils.authType().name().toLowerCase(Locale.ROOT))
        );

        metricsBase.addCustomChart(
                new SimplePie("floodgate_version", () -> DynamicConstants.FULL_VERSION)
        );

        metricsBase.addCustomChart(
                new DrilldownPie("platform", () -> Collections.singletonMap(
                        implementationName,
                        Collections.singletonMap(platformUtils.serverImplementationName(), 1)
                )));

        metricsBase.addCustomChart(
                new DrilldownPie("minecraft_version", () -> {
                    // e.g.: 1.16.5 => (Spigot, 1)
                    return Collections.singletonMap(
                            implementationName,
                            Collections.singletonMap(platformUtils.minecraftVersion(), 1)
                    );
                })
        );

        // Source: Geyser
        metricsBase.addCustomChart(new DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);

            String majorVersion = javaVersion.split("\\.")[0];
            String release;

            int indexOf = javaVersion.lastIndexOf('.');

            if (majorVersion.equals("1")) {
                release = "Java " + javaVersion.substring(0, indexOf);
            } else {
                Matcher versionMatcher = Pattern.compile("\\d+").matcher(majorVersion);
                if (versionMatcher.find()) {
                    majorVersion = versionMatcher.group(0);
                }
                release = "Java " + majorVersion;
            }
            map.put(release, entry);
            return map;
        }));
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    @EventListener
    public void onShutdown(ShutdownEvent ignored) {
        metricsBase.shutdown();
    }
}
