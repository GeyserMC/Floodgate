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

package com.minekube.connect.platform.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.minekube.connect.player.UserAudience;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CommandUtilTest {
    @Test
    void getOnlineUsernamesReturnsEmptyWhenThereAreNoOnlinePlayers() {
        TestCommandUtil util = new TestCommandUtil(Collections.emptyList(), Object::toString);

        assertEquals(Collections.emptyList(), util.getOnlineUsernames());
    }

    @Test
    void getOnlineUsernamesReturnsOneNamePerOnlinePlayerInOrder() {
        TestCommandUtil util = new TestCommandUtil(
                Arrays.asList("first", "second", "third"),
                source -> "name-" + source);

        assertEquals(Arrays.asList("name-first", "name-second", "name-third"), util.getOnlineUsernames());
    }

    @Test
    void getOnlineUsernamesPreservesNullUsernames() {
        TestCommandUtil util = new TestCommandUtil(
                Arrays.asList("first", "missing", "third"),
                source -> "missing".equals(source) ? null : "name-" + source);

        assertEquals(Arrays.asList("name-first", null, "name-third"), util.getOnlineUsernames());
    }

    private static final class TestCommandUtil extends CommandUtil {
        private final Collection<?> onlinePlayers;
        private final Function<Object, String> usernameResolver;

        private TestCommandUtil(Collection<?> onlinePlayers, Function<Object, String> usernameResolver) {
            super(null, null);
            this.onlinePlayers = onlinePlayers;
            this.usernameResolver = usernameResolver;
        }

        @Override
        public UserAudience getUserAudience(Object source) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String getUsernameFromSource(Object source) {
            return usernameResolver.apply(source);
        }

        @Override
        protected UUID getUuidFromSource(Object source) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Collection<?> getOnlinePlayers() {
            return onlinePlayers;
        }

        @Override
        public Object getPlayerByUuid(UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getPlayerByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasPermission(Object player, String permission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sendMessage(Object target, String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void kickPlayer(Object player, String message) {
            throw new UnsupportedOperationException();
        }
    }
}
