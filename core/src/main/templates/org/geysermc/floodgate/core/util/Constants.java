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

public final class Constants {
    public static final String VERSION = "@floodgateVersion@";
    public static final int BUILD_NUMBER = Integer.parseInt("@buildNumber@");
    public static final String GIT_BRANCH = "@branch@";
    public static final String GIT_MAIN_BRANCH = "master";
    public static final int METRICS_ID = 14649;

    public static final char COLOR_CHAR = '\u00A7';

    public static final boolean DEBUG_MODE = false;
    public static final boolean PRINT_ALL_PACKETS = false;

    public static final String DATABASE_NAME_FORMAT = "^floodgate-[a-zA-Z0-9_]{0,16}-database.jar$";


    private static final String API_BASE_URL = "s://api.geysermc.org";
    public static final String HEALTH_URL = "http" + API_BASE_URL + "/health";

    public static final String WEBSOCKET_URL = "ws" + API_BASE_URL + "/ws";
    public static final String GET_XUID_URL = "http" + API_BASE_URL + "/v2/xbox/xuid/";
    public static final String GET_GAMERTAG_URL = "http" + API_BASE_URL + "/v2/xbox/gamertag/";
    public static final String NEWS_OVERVIEW_URL = "http" + API_BASE_URL + "/v2/news/";
    public static final String GET_BEDROCK_LINK = "http" + API_BASE_URL + "/v2/link/bedrock/";

    public static final String PROJECT_NAME = "floodgate";
    public static final String LINK_INFO_URL = "https://link.geysermc.org/";
    public static final String LATEST_DOWNLOAD_URL =
            "https://geysermc.org/download#%s";
    public static final String LATEST_VERSION_URL =
            "https://download.geysermc.org/v2/projects/%s/versions/latest/builds/latest";

    public static final String PROFILE_WITH_PROPERTIES_URL =
            "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";


    public static final String NTP_SERVER = "time.cloudflare.com";
    public static final String INTERNAL_ERROR_MESSAGE =
            "An internal error happened while handling Floodgate data." +
            " Try logging in again or contact a server administrator if the issue persists.";
    public static final String UNSUPPORTED_DATA_VERSION =
            "Received an unsupported Floodgate data version." +
            " This Floodgate version is made for data version %s, received %s." +
            " Make sure that Floodgate is up-to-date.";


    public static final int HANDSHAKE_PACKET_ID = 0;
    public static final int LOGIN_SUCCESS_PACKET_ID = 2;
    public static final int SET_COMPRESSION_PACKET_ID = 3;

    public static final String DEFAULT_MINECRAFT_JAVA_SKIN_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxNTcxNzM1NTI2MywKICAicHJvZmlsZUlkIiA6ICIyMWUzNjdkNzI1Y2Y0ZTNiYjI2OTJjNGEzMDBhNGRlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJHZXlzZXJNQyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zMWY0NzdlYjFhN2JlZWU2MzFjMmNhNjRkMDZmOGY2OGZhOTNhMzM4NmQwNDQ1MmFiMjdmNDNhY2RmMWI2MGNiIgogICAgfQogIH0KfQ";
    public static final String DEFAULT_MINECRAFT_JAVA_SKIN_SIGNATURE = "dFKIZ5d6vNqCSe1IFGiVLjt3cnW8qh4qNP2umg9zqkX9bvAQawuR1iuO1kCD/+ye8A6GQFv2wRCdxdrjp5+Vrr0SsWqMnsYDN8cEg6CD18mAnaKI1TYDuGbdJaqLyGqN5wqSMdHxchs9iovFkde5ir4aYdvHkA11vOTi11L4kUzETGzJ4iKVuZOv4dq+B7wFAWqp4n8QZfhixyvemFazQHlLmxnuhU+jhpZMvYY9MAaRAJonfy/wJe9LymbTe0EJ8N+NwZQDrEUzgfBFo4OIGDqRZwvydInCqkjhPMtHCSL25VOKwcFocYpRYbk4eIKM4CLjYlBiQGki+XKsPaljwjVhnT0jUupSf7yraGb3T0CsVBjhDbIIIp9nytlbO0GvxHu0TzYjkr4Iji0do5jlCKQ/OasXcL21wd6ozw0t1QZnnzxi9ewSuyYVY9ErmWdkww1OtCIgJilceEBwNAB8+mhJ062WFaYPgJQAmOREM8InW33dbbeENMFhQi4LIO5P7p9ye3B4Lrwm20xtd9wJk3lewzcs8ezh0LUF6jPSDQDivgSKU49mLCTmOi+WZh8zKjjxfVEtNZON2W+3nct0LiWBVsQ55HzlvF0FFxuRVm6pxi6MQK2ernv3DQl0hUqyQ1+RV9nfZXTQOAUzwLjKx3t2zKqyZIiNEKLE+iAXrsE=";
}
