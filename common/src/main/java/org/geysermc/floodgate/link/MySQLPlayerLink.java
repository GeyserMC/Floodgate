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

public class MySQLPlayerLink extends PlayerLink {
    @Getter private Connection connection;

    @Override
    public void load(Path dataFolder) {
        getLogger().info("Loading Floodgate linked player database...");
        try {
            // Normally optional since Java 1.6
            Class.forName("com.mysql.jdbc.Driver");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:mysql://" + getPath(), 
                getSqlUser(), getSqlPassword());
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS LinkedPlayers (bedrockId varchar(36), javaUniqueId varchar(36), javaUsername varchar(16));");
            statement.executeUpdate("ALTER TABLE LinkedPlayers ADD PRIMARY KEY(bedrockId);");
            statement.executeUpdate("ALTER TABLE `LinkedPlayers` ADD UNIQUE(`javaUniqueId`);");
        } catch (ClassNotFoundException e) {
            getLogger().severe("The required class to load the MySQL database wasn't found");
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
                PreparedStatement query = connection.prepareStatement("INSERT INTO LinkedPlayers values(?, ?, ?)");
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
