package com.minekube.connect.util;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.config.ConnectConfig.MetricsConfig;
import com.minekube.connect.platform.util.PlatformUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bstats.MetricsBase;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.json.JsonObjectBuilder;

public final class Metrics {
    private final MetricsBase metricsBase;

    @Inject
    Metrics(ConnectConfig config,
            PlatformUtils platformUtils,
            ConnectApi api,
            @Named("platformName") String implementationName,
            ConnectLogger logger) {

        MetricsConfig metricsConfig = config.getMetrics();

        metricsBase = new MetricsBase(
                "server-implementation",
                metricsConfig.getUuid(),
                Constants.METRICS_ID,
                !metricsConfig.isDisabled(),
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
                            categoryName,
                            Collections.singletonMap(implementationName, 1)
                    );
                })
        );

        metricsBase.addCustomChart(
                new SimplePie("authentication",
                        () -> platformUtils.authType().name().toLowerCase(Locale.ROOT))
        );

        metricsBase.addCustomChart(
                new DrilldownPie("platform", () -> Collections.singletonMap(
                        implementationName,
                        Collections.singletonMap(platformUtils.serverImplementationName(), 1)
                )));

        metricsBase.addCustomChart(new SimplePie("platform", () -> implementationName));

        metricsBase.addCustomChart(new AdvancedPie("passthrough_auth_player_count", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            api.getPlayers().forEach(connectPlayer -> {
                String key = Boolean.toString(connectPlayer.getAuth().isPassthrough());
                valueMap.put(key, valueMap.getOrDefault(key, 0) + 1);
            });
            return valueMap;
        }));

        metricsBase.addCustomChart(
                new DrilldownPie("minecraft_version", () -> {
                    // e.g.: 1.16.5 => (Spigot, 1)
                    return Collections.singletonMap(
                            platformUtils.minecraftVersion(),
                            Collections.singletonMap(implementationName, 1)
                    );
                })
        );

        // Source: Geyser
        metricsBase.addCustomChart(new DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = JAVA_VERSION;
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
        builder.appendField("osName", OS_NAME);
        builder.appendField("osArch", OS_ARCH);
        builder.appendField("osVersion", OS_VERSION);
        builder.appendField("coreCount", CORE_COUNT);
    }

    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String OS_NAME = System.getProperty("os.name");
    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String OS_VERSION = System.getProperty("os.version");
    public static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
}
