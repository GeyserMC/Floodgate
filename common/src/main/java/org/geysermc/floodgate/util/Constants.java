/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
    public static final char COLOR_CHAR = '§';

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

    public static final String LINK_INFO_URL = "https://link.geysermc.org/";

    public static final String NEWS_PROJECT_NAME = "floodgate";


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
}
