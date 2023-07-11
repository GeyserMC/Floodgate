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

package org.geysermc.floodgate.core.database.entity;

import io.micronaut.core.annotation.AccessorsStyle;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;

@Entity
@AccessorsStyle(readPrefixes = "", writePrefixes = "")
public class LinkedPlayer {
    @Id
    private UUID bedrockId;
    private UUID javaUniqueId;
    private String javaUsername;

    public UUID bedrockId() {
        return bedrockId;
    }

    public LinkedPlayer bedrockId(UUID bedrockId) {
        this.bedrockId = bedrockId;
        return this;
    }

    public UUID javaUniqueId() {
        return javaUniqueId;
    }

    public LinkedPlayer javaUniqueId(UUID javaUniqueId) {
        this.javaUniqueId = javaUniqueId;
        return this;
    }

    public String javaUsername() {
        return javaUsername;
    }

    public LinkedPlayer javaUsername(String javaUsername) {
        this.javaUsername = javaUsername;
        return this;
    }

    @Override
    @Transient
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (LinkedPlayer) obj;
        return Objects.equals(this.bedrockId, that.bedrockId) &&
                Objects.equals(this.javaUniqueId, that.javaUniqueId);
    }

    @Override
    @Transient
    public int hashCode() {
        return Objects.hash(bedrockId, javaUniqueId);
    }

    @Override
    @Transient
    public String toString() {
        return "LinkedPlayer[" +
                "bedrockId=" + bedrockId + ", " +
                "javaUniqueId=" + javaUniqueId + ", " +
                "javaUsername=" + javaUsername + ']';
    }

}
