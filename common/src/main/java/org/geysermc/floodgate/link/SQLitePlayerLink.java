package org.geysermc.floodgate.link;

import lombok.Getter;
import org.geysermc.floodgate.LinkedPlayer;
import org.geysermc.floodgate.PlayerLink;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public final class SQLitePlayerLink extends PlayerLink {
    @Getter private Connection connection;

    @Override
    public void load(Path dataFolder) {
        Path databasePath = dataFolder.resolve("linked-players.db");
        getLogger().info("Loading Floodgate linked player database...");
        try {
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate("create table if not exists LinkedPlayers (bedrockId string, javaUniqueId string, javaUsername string)");
        } catch (ClassNotFoundException e) {
            getLogger().severe("The required class to load the SQLite database wasn't found");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error while loading database", e);
        }
    }

    @Override
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId) {
        // TODO: make it work with Java player UUIDs
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("select * from LinkedPlayers where bedrockId = ?");
                query.setString(1, bedrockId.toString());
                ResultSet result = query.executeQuery();
                if (!result.next()) return null;

                String javaUsername = result.getString("javaUsername");
                UUID javaUniqueId = UUID.fromString(result.getString("javaUniqueId"));
                return createLinkedPlayer(javaUsername, javaUniqueId, bedrockId);
            } catch (SQLException | NullPointerException e) {
                getLogger().log(Level.SEVERE, "Error while getting LinkedPlayer", e);
                throw new CompletionException("Error while getting LinkedPlayer", e);
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
            } catch (SQLException | NullPointerException e) {
                getLogger().log(Level.SEVERE, "Error while checking if player is a LinkedPlayer", e);
                throw new CompletionException("Error while checking if player is a LinkedPlayer", e);
            }
        }, getExecutorService());
    }

    @Override
    public CompletableFuture<Void> linkPlayer(UUID bedrockId, UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("insert into LinkedPlayers values(?, ?, ?)");
                query.setString(1, bedrockId.toString());
                query.setString(2, uuid.toString());
                query.setString(3, username);
                query.executeUpdate();
            } catch (SQLException | NullPointerException e) {
                getLogger().log(Level.SEVERE, "Error while linking player", e);
                throw new CompletionException("Error while linking player", e);
            }
        }, getExecutorService());
    }

    @Override
    public CompletableFuture<Void> unlinkPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement query = connection.prepareStatement("delete from LinkedPlayers where javaUniqueId = ? or bedrockId = ?");
                query.setString(1, uuid.toString());
                query.setString(2, uuid.toString());
                query.executeUpdate();
            } catch (SQLException | NullPointerException e) {
                getLogger().log(Level.SEVERE, "Error while unlinking player", e);
                throw new CompletionException("Error while unlinking player", e);
            }
        }, getExecutorService());
    }
}
