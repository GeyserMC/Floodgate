/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.database;

import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;
import org.geysermc.floodgate.core.database.entity.LinkRequest;

@Repository
public interface PendingLinkRepository extends IRepository<LinkRequest> {
    @Query("deleteByJavaUsernameAndBedrockUsernameAndLinkCodeAndJavaUniqueIdIsNotNull")
    CompletableFuture<LinkRequest> getAndInvalidateLinkRequestForBedrock(
            String javaUsername, String bedrockUsername, String linkCode);

    @Query("deleteByJavaUsernameAndBedrockUsernameAndLinkCodeAndJavaUniqueIdIsNull")
    CompletableFuture<LinkRequest> getAndInvalidateLinkRequestForJava(
            String javaUsername, String bedrockUsername, String linkCode);

    @Query("deleteByBedrockUsernameAndLinkCodeAndJavaUsernameIsNullAndJavaUniqueIdIsNull")
    CompletableFuture<LinkRequest> getAndInvalidateLinkRequestForJava(String bedrockUsername, String linkCode);

    CompletableFuture<Void> insert(LinkRequest request);

    CompletableFuture<Void> delete(LinkRequest request);
}
