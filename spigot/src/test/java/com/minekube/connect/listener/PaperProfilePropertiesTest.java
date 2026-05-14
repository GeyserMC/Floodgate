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

package com.minekube.connect.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.minekube.connect.api.player.GameProfile;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperProfilePropertiesTest {
    @Test
    void convertsSignedTexturePropertyForPaperProfilePrefill() {
        GameProfile connectProfile = new GameProfile(
                "RoboFlax2",
                UUID.fromString("c66dfcbc-4bd2-4a29-8c76-eadf80faa08a"),
                Collections.singletonList(new GameProfile.Property("textures", "skin-value", "skin-signature"))
        );

        ProfileProperty texture = PaperProfileProperties.fromConnectProfile(connectProfile)
                .iterator()
                .next();

        assertEquals("textures", texture.getName());
        assertEquals("skin-value", texture.getValue());
        assertEquals("skin-signature", texture.getSignature());
        assertTrue(texture.isSigned());
    }
}
