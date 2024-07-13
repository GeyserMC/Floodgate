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

package org.geysermc.floodgate.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.internal.Base64;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.link.LinkRequest;
import org.geysermc.floodgate.api.link.LinkRequestResult;
import org.geysermc.floodgate.database.config.MongoConfig;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.link.LinkRequestImpl;
import org.geysermc.floodgate.util.LinkedPlayer;

public class MongoDbDatabase extends CommonPlayerLink {
    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> linkedPlayer;
    private MongoCollection<Document> linkedPlayerRequests;

    @Override
    public void load() {
        getLogger().info("Connecting to MongoDB database...");
        try {
            MongoConfig databaseConfig = getConfig(MongoConfig.class);

            MongoClientSettings.Builder settings = MongoClientSettings.builder();
            settings.applyToConnectionPoolSettings(builder -> {
                builder.maxSize(10);
                builder.minSize(2);
            });

            if (databaseConfig.getMongouri().isEmpty()) {
                settings.credential(
                        MongoCredential.createCredential(
                                databaseConfig.getUsername(),
                                databaseConfig.getDatabase(),
                                databaseConfig.getPassword().toCharArray()
                        )
                );
            } else {
                settings.applyConnectionString(new ConnectionString(databaseConfig.getMongouri()));
            }

            client = MongoClients.create(settings.build());

            database = client.getDatabase(databaseConfig.getDatabase());

            linkedPlayer = database.getCollection("LinkedPlayers");
            if (collectionNotExists("LinkedPlayers")) {
                database.createCollection("LinkedPlayers");

                linkedPlayer.createIndex(new Document("bedrockId", 1),
                        new IndexOptions().unique(true)); // primary key equivalent
                linkedPlayer.createIndex(Indexes.ascending("javaUniqueId"));
            }

            linkedPlayerRequests = database.getCollection("LinkedPlayerRequests");
            if (collectionNotExists("LinkedPlayerRequests")) {
                database.createCollection("LinkedPlayerRequests");

                linkedPlayerRequests.createIndex(new Document("bedrockId", 1),
                        new IndexOptions().unique(true)); // primary key equivalent
                linkedPlayerRequests.createIndex(Indexes.ascending("requestTime"));
            }

            getLogger().info("Connected to MongoDB database.");
        } catch (Exception exception) {
            getLogger().error("Error while loading database", exception);
        }
    }

    @Override
    public void stop() {
        super.stop();
        client.close();
    }

    @Override
    @NonNull
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(@NonNull UUID bedrockId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Bson filter = Filters.eq("bedrockId", uuidToBytes(bedrockId));

                try (MongoCursor<Document> cursor = linkedPlayer.find(filter).cursor()) {
                    if (cursor.hasNext()) {
                        Document document = cursor.next();
                        String javaUsername = document.getString("javaUsername");
                        UUID javaUniqueId = bytesToUUID(document.getString("javaUniqueId"));

                        return LinkedPlayer.of(javaUsername, javaUniqueId, bedrockId);
                    }
                }

                return null;
            } catch (Exception exception) {
                getLogger().error("Error while getting LinkedPlayer", exception);
                throw new CompletionException("Error while getting LinkedPlayer", exception);
            }
        }, getExecutorService());
    }

    @Override
    @NonNull
    public CompletableFuture<Boolean> isLinkedPlayer(@NonNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuidBytes = uuidToBytes(playerId);
                Bson filter = Filters.or(
                        Filters.eq("bedrockId", uuidBytes),
                        Filters.eq("javaUniqueId", uuidBytes)
                );
                try (MongoCursor<Document> cursor = linkedPlayer.find(filter).cursor()) {
                    return cursor.hasNext();
                }
            } catch (Exception exception) {
                getLogger().error("Error while checking if player is a LinkedPlayer", exception);
                throw new CompletionException(
                        "Error while checking if player is a LinkedPlayer", exception
                );
            }
        }, getExecutorService());
    }

    @Override
    @NonNull
    public CompletableFuture<Void> linkPlayer(
            @NonNull UUID bedrockId,
            @NonNull UUID javaId,
            @NonNull String javaUsername) {
        return CompletableFuture.runAsync(
                () -> linkPlayer0(bedrockId, javaId, javaUsername),
                getExecutorService());
    }

    private void linkPlayer0(UUID bedrockId, UUID javaId, String javaUsername) {
        try {
            Bson filter = Filters.eq("javaUsername", javaUsername);
            Document create = new Document("bedrockId", uuidToBytes(bedrockId))
                    .append("javaUniqueId", uuidToBytes(javaId))
                    .append("javaUsername", javaUsername);
            Document update = new Document("$set", create);

            linkedPlayer.updateOne(filter, update, new UpdateOptions().upsert(true));
            // The upsert option will create a new document if the filter doesn't match anything.
            // Or will update the document if it does match.
        } catch (Exception exception) {
            getLogger().error("Error while linking player", exception);
            throw new CompletionException("Error while linking player", exception);
        }
    }

    @Override
    @NonNull
    public CompletableFuture<Void> unlinkPlayer(@NonNull UUID javaId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String uuidBytes = uuidToBytes(javaId);

                Bson filter = Filters.and(
                        Filters.eq("javaUniqueId", uuidBytes),
                        Filters.eq("bedrockId", uuidBytes)
                );

                linkedPlayer.deleteMany(filter);
            } catch (Exception exception) {
                getLogger().error("Error while unlinking player", exception);
                throw new CompletionException("Error while unlinking player", exception);
            }
        }, getExecutorService());
    }

    @Override
    @NonNull
    public CompletableFuture<String> createLinkRequest(
            @NonNull UUID javaId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername) {
        return CompletableFuture.supplyAsync(() -> {
            String linkCode = createCode();

            createLinkRequest0(javaUsername, javaId, linkCode, bedrockUsername);

            return linkCode;
        }, getExecutorService());
    }

    private void createLinkRequest0(
            String javaUsername,
            UUID javaId,
            String linkCode,
            String bedrockUsername) {
        try {
            Bson filter = Filters.eq("javaUsername", javaUsername);
            Document create = new Document("javaUsername", javaUsername)
                    .append("javaUniqueId", uuidToBytes(javaId))
                    .append("linkCode", linkCode)
                    .append("bedrockUsername", bedrockUsername)
                    .append("requestTime", Instant.now().getEpochSecond());
            Document update = new Document("$set", create);

            linkedPlayerRequests.updateOne(filter, update, new UpdateOptions().upsert(true));
            // The upsert option will create a new document if the filter doesn't match anything.
            // Or will update the document if it does match.
        } catch (Exception exception) {
            getLogger().error("Error while linking player", exception);
            throw new CompletionException("Error while linking player", exception);
        }
    }

    private void removeLinkRequest(String javaUsername) {
        try {
            Document filter = new Document("javaUsername", javaUsername);
            linkedPlayerRequests.deleteMany(filter);
        } catch (Exception exception) {
            getLogger().error("Error while cleaning up LinkRequest", exception);
        }
    }

    @Override
    @NonNull
    public CompletableFuture<LinkRequestResult> verifyLinkRequest(
            @NonNull UUID bedrockId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        return CompletableFuture.supplyAsync(() -> {
            LinkRequest request = getLinkRequest0(javaUsername);

            if (request == null || !isRequestedPlayer(request, bedrockId)) {
                return LinkRequestResult.NO_LINK_REQUESTED;
            }

            if (!request.getLinkCode().equals(code)) {
                return LinkRequestResult.INVALID_CODE;
            }

            // link request can be removed. Doesn't matter if the request is expired or not
            removeLinkRequest(javaUsername);

            if (request.isExpired(getVerifyLinkTimeout())) {
                return LinkRequestResult.REQUEST_EXPIRED;
            }

            linkPlayer0(bedrockId, request.getJavaUniqueId(), javaUsername);
            return LinkRequestResult.LINK_COMPLETED;
        }, getExecutorService());
    }

    private LinkRequest getLinkRequest0(String javaUsername) {
        try {
            Bson filter = Filters.eq("javaUsername", javaUsername);
            try (MongoCursor<Document> cursor = linkedPlayerRequests.find(filter).cursor()) {
                if (cursor.hasNext()) {
                    Document document = cursor.next();
                    UUID javaId = bytesToUUID(document.getString("javaUniqueId"));
                    String linkCode = document.getString("linkCode");
                    String bedrockUsername = document.getString("bedrockUsername");
                    long requestTime = document.getLong("requestTime");
                    return new LinkRequestImpl(javaUsername, javaId, linkCode, bedrockUsername,
                            requestTime);
                }
            }
        } catch (Exception exception) {
            getLogger().error("Error while getLinkRequest", exception);
            throw new CompletionException("Error while getLinkRequest", exception);
        }
        return null;
    }

    public void cleanLinkRequests() {
        try {
            Document filter = new Document("requestTime",
                    new Document("$lt", Instant.now().getEpochSecond() - getVerifyLinkTimeout()));
            linkedPlayerRequests.deleteMany(filter);
        } catch (Exception exception) {
            getLogger().error("Error while cleaning up link requests", exception);
        }
    }

    private String uuidToBytes(UUID uuid) {
        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return Base64.encode(uuidBytes);
    }

    private UUID bytesToUUID(String uuidBytes) {
        ByteBuffer buf = ByteBuffer.wrap(Base64.decode(uuidBytes));
        return new UUID(buf.getLong(), buf.getLong());
    }

    public boolean collectionNotExists(final String collectionName) {
        return !database.listCollectionNames().into(new ArrayList<>()).contains(collectionName);
    }

}
