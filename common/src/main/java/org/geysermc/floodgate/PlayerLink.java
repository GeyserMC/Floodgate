package org.geysermc.floodgate;

import lombok.Getter;

import java.util.UUID;

// import java.util.Iterator;
// import java.util.List;

// import java.nio.file.Paths;
// import java.nio.file.Files;
import java.nio.file.Path;

// import java.io.IOException;

// import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

@Getter
public class PlayerLink {
    public static boolean enabled; // Whether to enable the player linking system

    public static long linkCodeTimeout;
    public static boolean allowLinking;

    static Statement statement;
    static Connection connection;

    public static LinkedPlayer getLinkedPlayer(UUID bedrockId) {
      // TODO: make it work with Java player UUIDs
      ResultSet rs2;
      String javaUsername2;
      UUID javaUniqueId2;
      UUID bedrockId2;
      try {
          PreparedStatement query = connection.prepareStatement("select * from LinkedPlayers where bedrockId = ?");
          query.setString(1, bedrockId.toString());
          rs2 = query.executeQuery();
          if (!rs2.next()) {
            return null;
          }
          javaUsername2 = rs2.getString("javaUsername");
          javaUniqueId2 = UUID.fromString(rs2.getString("javaUniqueId"));
          bedrockId2 = UUID.fromString(rs2.getString("bedrockId"));
          return new LinkedPlayer(javaUsername2, javaUniqueId2, bedrockId2);
        } catch(SQLException e) {
          System.err.println("Floodgate: Error: " + e.getMessage());
          return null;
        }
    }
    public static boolean isLinkedPlayer(UUID bedrockId) {
      ResultSet rs3;
        try {
          PreparedStatement query = connection.prepareStatement("select * from LinkedPlayers where bedrockId = ? or javaUniqueId = ?");
          query.setString(1, bedrockId.toString());
          query.setString(2, bedrockId.toString());
          rs3 = query.executeQuery();
          System.out.println(rs3);
        if (rs3.next()) {
          return true;
        } else {
          return false;
        }
      } catch(SQLException e) {
        System.err.println("Floodgate: Error: " + e.getMessage());
      }
      return false;
    }
    public static void load(Path configPath, FloodgateConfig config) {
        enabled = config.getPlayerLink().isEnabled();
        if (!enabled) {
          return;
        }
        allowLinking = config.getPlayerLink().isAllowLinking();
        linkCodeTimeout = config.getPlayerLink().getLinkCodeTimeout();
        config.getPlayerLink().isEnabled();

        Path databasePath = configPath.getParent().resolve("linked-players.db");
        System.out.println("Floodgate: Loading linked player database...");
        try { Class.forName("org.sqlite.JDBC"); } catch(ClassNotFoundException e) {}
        try {
          // create a database connection
          connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
          statement = connection.createStatement();
          statement.setQueryTimeout(30);  // set timeout to 30 sec.
          statement.executeUpdate("create table if not exists LinkedPlayers (bedrockId string, javaUniqueId string, javaUsername string)");
        } catch(SQLException e) {
          System.err.println("Floodgate: Error loading database: " + e.getMessage());
        }

        System.out.println("Floodgate: Done!");

    }

    public static boolean linkPlayer(UUID bedrockId, UUID uuid, String username) {
      try {
        PreparedStatement query = connection.prepareStatement("insert into LinkedPlayers values(?, ?, ?)");
        query.setString(1, bedrockId.toString());
        query.setString(2, uuid.toString());
        query.setString(3, username);
        query.executeUpdate();
        return true;
      } catch(SQLException e) {
        System.err.println("Floodgate: Error: " + e.getMessage());
        return false;
      }
    }

    public static String unlinkPlayer(UUID uuid) {
      if (enabled && allowLinking) {
        try {
          PreparedStatement query = connection.prepareStatement("delete from LinkedPlayers where javaUniqueId = ? or bedrockId = ?");
          query.setString(1, uuid.toString());
          query.setString(2, uuid.toString());
          query.executeUpdate();
          return "§aUnlink successful!§f";
        } catch(SQLException e) {
          System.err.println("Floodgate: " + e.getMessage());
        }
        return "Error";
      } else {
        return "§cLinking is not enabled on this server§f";
      }
    }
}
