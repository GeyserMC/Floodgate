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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@AccessorsStyle(readPrefixes = "", writePrefixes = "")
public class LinkRequest {
    @Id
    private UUID javaUniqueId;
    private String javaUsername;
    private String bedrockUsername;
    private String linkCode;
    private long requestTime = Instant.now().getEpochSecond();

    public UUID javaUniqueId() {
        return javaUniqueId;
    }

    public LinkRequest javaUniqueId(UUID javaUniqueId) {
        this.javaUniqueId = javaUniqueId;
        return this;
    }

    public String javaUsername() {
        return javaUsername;
    }

    public LinkRequest javaUsername(String javaUsername) {
        this.javaUsername = javaUsername;
        return this;
    }

    public String bedrockUsername() {
        return bedrockUsername;
    }

    public LinkRequest bedrockUsername(String bedrockUsername) {
        this.bedrockUsername = bedrockUsername;
        return this;
    }

    public String linkCode() {
        return linkCode;
    }

    public LinkRequest linkCode(String linkCode) {
        this.linkCode = linkCode;
        return this;
    }

    public long requestTime() {
        return requestTime;
    }

    public LinkRequest requestTime(long requestTime) {
        this.requestTime = requestTime;
        return this;
    }

    @Transient
    public boolean isExpired(long linkTimeout) {
        long timePassed = Instant.now().getEpochSecond() - requestTime;
        return timePassed > linkTimeout;
    }

    @Override
    @Transient
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LinkRequest that = (LinkRequest) o;
        return Objects.equals(javaUniqueId, that.javaUniqueId);
    }

    @Override
    @Transient
    public int hashCode() {
        return Objects.hash(javaUniqueId);
    }

    @Override
    @Transient
    public String toString() {
        return "LinkRequest{" +
                "javaUniqueId=" + javaUniqueId +
                ", javaUsername='" + javaUsername + '\'' +
                ", bedrockUsername='" + bedrockUsername + '\'' +
                ", linkCode='" + linkCode + '\'' +
                ", requestTime=" + requestTime +
                '}';
    }
}
