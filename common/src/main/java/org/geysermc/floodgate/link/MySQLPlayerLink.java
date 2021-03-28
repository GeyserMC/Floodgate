package org.geysermc.floodgate.link;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.geysermc.floodgate.LinkedPlayer;
import org.geysermc.floodgate.PlayerLink;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public class MySQLPlayerLink extends PlayerLink {

    private HikariDataSource dataSource;

    private void setupPool() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + getDatabaseHostname() + ":" + getDatabasePort() + "/" + getDatabaseName());
        config.setUsername(getDatabaseUsername());
        config.setPassword(getDatabasePassword());

        config.addDataSourceProperty("useSSL", true);
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.addDataSourceProperty("autoReconnect", true);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats", false);

        dataSource = new HikariDataSource(config);
    }

    @Override
    protected void load(Path dataFolder) {

        getLogger().info("Loading Floodgate linked player database...");
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            getLogger().severe("The required class to load the MySQL database wasn't found");
            return;
        }

        setupPool();

        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + getDatabasePrefix() + "LinkedPlayers (bedrockId VARCHAR(36), javaUniqueId VARCHAR(46), javaUsername VARCHAR(16))");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error while loading database", e);
        }
    }

    @Override
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId) {
        // TODO: make it work with Java player UUIDs
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement query = connection.prepareStatement("SELECT * FROM " + getDatabasePrefix() + "LinkedPlayers WHERE bedrockId = ?");
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
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement query = connection.prepareStatement("SELECT javaUniqueId FROM " + getDatabasePrefix() + "LinkedPlayers WHERE bedrockId = ? OR javaUniqueId = ?");
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
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement query = connection.prepareStatement("INSERT INTO " + getDatabasePrefix() + "LinkedPlayers VALUES(?, ?, ?)");
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
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement query = connection.prepareStatement("DELETE FROM " + getDatabasePrefix() + "LinkedPlayers WHERE javaUniqueId = ? OR bedrockId = ?");
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
