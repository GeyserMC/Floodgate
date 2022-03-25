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

package com.minekube.connect.command.util;

import static com.minekube.connect.command.util.PermissionDefault.OP;
import static com.minekube.connect.command.util.PermissionDefault.TRUE;

public enum Permission {
    COMMAND_MAIN("connect.command.floodgate", TRUE),
    COMMAND_MAIN_FIREWALL(COMMAND_MAIN, "firewall", OP),
    COMMAND_LINK("connect.command.linkaccount", TRUE),
    COMMAND_UNLINK("connect.command.unlinkaccount", TRUE),
    COMMAND_WHITELIST("connect.command.fwhitelist", OP),

    NEWS_RECEIVE("connect.news.receive", OP);

    private final String permission;
    private final PermissionDefault defaultValue;

    Permission(String permission, PermissionDefault defaultValue) {
        this.permission = permission;
        this.defaultValue = defaultValue;
    }

    Permission(Permission parent, String child, PermissionDefault defaultValue) {
        this(parent.get() + "." + child, defaultValue);
    }

    public String get() {
        return permission;
    }

    public PermissionDefault defaultValue() {
        return defaultValue;
    }
}
