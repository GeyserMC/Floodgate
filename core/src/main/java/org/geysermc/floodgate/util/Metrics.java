/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.util;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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
import org.geysermc.event.Listener;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfig.MetricsConfig;
import org.geysermc.floodgate.event.lifecycle.ShutdownEvent;
import org.geysermc.floodgate.platform.util.PlatformUtils;
import org.geysermc.floodgate.platform.util.PlatformUtils.AuthType;

@Listener
@AutoBind
public final class Metrics {
    private final MetricsBase metricsBase;

    @Inject
    Metrics(FloodgateConfig config, PlatformUtils platformUtils, FloodgateApi api,
            @Named("implementationName") String implementationName, FloodgateLogger logger) {

        MetricsConfig metricsConfig = config.getMetrics();

        metricsBase = new MetricsBase(
                "server-implementation",
                metricsConfig.getUuid(),
                Constants.METRICS_ID,
                metricsConfig.isEnabled(),
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
                new SingleLineChart("players", api::getPlayerCount)
        );

        metricsBase.addCustomChart(
                new DrilldownPie("player_count", () -> {
                    int playerCount = api.getPlayerCount();
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
                new SimplePie("floodgate_version", () -> Constants.VERSION)
        );

        metricsBase.addCustomChart(
                new SimplePie("using-backend-server-linking", () -> {
                    if (platformUtils.authType() == AuthType.PROXIED) {
                        return String.valueOf(config.getPlayerLink().isEnableOwnLinking());
                    }
                    return "false";
                })
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

    @Subscribe
    public void onShutdown(ShutdownEvent ignored) {
        metricsBase.shutdown();
    }
}
