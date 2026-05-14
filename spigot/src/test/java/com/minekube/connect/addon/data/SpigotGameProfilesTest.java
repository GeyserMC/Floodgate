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

package com.minekube.connect.addon.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minekube.connect.api.player.GameProfile;
import com.minekube.connect.util.SpigotGameProfiles;
import com.mojang.authlib.properties.Property;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpigotGameProfilesTest {
    @Test
    void copiesSignedTexturePropertyFromConnectProfile() {
        UUID uuid = UUID.fromString("c66dfcbc-4bd2-4a29-8c76-eadf80faa08a");
        GameProfile connectProfile = new GameProfile(
                "RoboFlax2",
                uuid,
                Collections.singletonList(new GameProfile.Property("textures", "skin-value", "skin-signature"))
        );

        com.mojang.authlib.GameProfile profile = SpigotGameProfiles.fromConnectProfile(connectProfile);

        Property texture = profile.getProperties().get("textures").iterator().next();
        assertEquals(uuid, profile.getId());
        assertEquals("RoboFlax2", profile.getName());
        assertEquals("skin-value", texture.getValue());
        assertEquals("skin-signature", texture.getSignature());
        assertTrue(texture.hasSignature());
    }
}
