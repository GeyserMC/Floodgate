/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.database;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.geysermc.floodgate.link.CommonPlayerLink;
import org.geysermc.floodgate.util.LinkedPlayer;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SqliteDatabase extends CommonPlayerLink {
    private Connection connection;

    @Inject
    @Named("dataDirectory")
    private Path dataDirectory;

    @Override
    public void load() {
        Path databasePath = dataDirectory.resolve("linked-players.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate("create table if not exists LinkedPlayers (bedrockId string, javaUniqueId string, javaUsername string)");
        } catch (ClassNotFoundException exception) {
            getLogger().error("The required class to load the SQLite database wasn't found");
        } catch (SQLException exception) {
            getLogger().error("Error while loading database", exception);
        }
    }

    @Override
    public void stop() {
        super.stop();
        try {
            connection.close();
        } catch (SQLException exception) {
            getLogger().error("Error while closing database connection", exception);
        }
    }

    @Override
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("select * from LinkedPlayers where bedrockId = ?");
                query.setString(1, bedrockId.toString());
                ResultSet result = query.executeQuery();
                if (!result.next()) return null;

                String javaUsername = result.getString("javaUsername");
                UUID javaUniqueId = UUID.fromString(result.getString("javaUniqueId"));
                return new LinkedPlayer(javaUsername, javaUniqueId, bedrockId);
            } catch (SQLException | NullPointerException exception) {
                getLogger().error("Error while getting LinkedPlayer", exception);
                throw new CompletionException("Error while getting LinkedPlayer", exception);
            }
        }, getExecutorService());
    }

    @Override
    public CompletableFuture<Boolean> isLinkedPlayer(UUID bedrockId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("select javaUniqueId from LinkedPlayers where bedrockId = ? or javaUniqueId = ?");
                query.setString(1, bedrockId.toString());
                query.setString(2, bedrockId.toString());
                ResultSet result = query.executeQuery();
                return result.next();
            } catch (SQLException | NullPointerException exception) {
                getLogger().error("Error while checking if player is a LinkedPlayer", exception);
                throw new CompletionException(
                        "Error while checking if player is a LinkedPlayer", exception
                );
            }
        }, getExecutorService());
    }

    @Override
    public CompletableFuture<Void> linkPlayer(UUID bedrockId, UUID javaId, String username) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("insert into LinkedPlayers values(?, ?, ?)");
                query.setString(1, bedrockId.toString());
                query.setString(2, javaId.toString());
                query.setString(3, username);
                query.executeUpdate();
            } catch (SQLException | NullPointerException exception) {
                getLogger().error("Error while linking player", exception);
                throw new CompletionException("Error while linking player", exception);
            }
        }, getExecutorService());
    }

    @Override
    public CompletableFuture<Void> unlinkPlayer(UUID javaId) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("delete from LinkedPlayers where javaUniqueId = ? or bedrockId = ?");
                query.setString(1, javaId.toString());
                query.setString(2, javaId.toString());
                query.executeUpdate();
            } catch (SQLException | NullPointerException exception) {
                getLogger().error("Error while unlinking player", exception);
                throw new CompletionException("Error while unlinking player", exception);
            }
        }, getExecutorService());
    }
}
