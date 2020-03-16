package org.geysermc.floodgate;

import lombok.Getter;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
// import java.util.Iterator;
// import java.util.List;
import java.util.Random;

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

    static final Map<String, LinkRequest> activeLinkRequests = new HashMap<>(); // Maps Java usernames to LinkRequest objects
    static Statement statement;
    static Connection connection;

    public static LinkedPlayer getLinkedPlayer(UUID bedrockId) {
      // TODO: make it work with Java player UUIDs
      ResultSet rs2;
      String javaUsername2 = ":(";
      UUID javaUniqueId2 = UUID.fromString("00000000-0000-0000-0000-000000000000");
      UUID bedrockId2 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        // linkedPlayers.put("ee2e9ec9-8ef2-34ac-bcb7-5661e9f97458", new LinkedPlayer("circuit10", UUID.fromString("7fb28d57-aafd-42d5-a705-cef18ab5313f"), UUID.fromString("ee2e9ec9-8ef2-34ac-bcb7-5661e9f97458")));
        // System.out.println(bedrockId);
        // return linkedPlayers.get(bedrockId.toString());
        try {
        PreparedStatement query = connection.prepareStatement("select * from LinkedPlayers where bedrockId = ?");
        query.setString(1, bedrockId.toString());
        rs2 = query.executeQuery();
        // rs2 = statement.executeQuery("select * from LinkedPlayers where bedrockId = 'fe2e9ec9-8ef2-34ac-bcb7-5661e9f97458'");
        rs2.next();
        javaUsername2 = rs2.getString("javaUsername");
        javaUniqueId2 = UUID.fromString(rs2.getString("javaUniqueId"));
        bedrockId2 = UUID.fromString(rs2.getString("bedrockId"));
      } catch(SQLException e) {
        System.err.println("Floodgate: Error loading database: " + e.getMessage());
      // } finally {
        try {
          if(connection != null)
            connection.close();
        } catch(SQLException e2) {
          System.err.println("Floodgate: Could not close database: " + e);
        }
      }
      return new LinkedPlayer(javaUsername2, javaUniqueId2, bedrockId2);
    }
    public static boolean isLinkedPlayer(UUID bedrockId) {
      ResultSet rs3;
        // linkedPlayers.put("ee2e9ec9-8ef2-34ac-bcb7-5661e9f97458", new LinkedPlayer("circuit10", UUID.fromString("7fb28d57-aafd-42d5-a705-cef18ab5313f"), UUID.fromString("ee2e9ec9-8ef2-34ac-bcb7-5661e9f97458")));
        // System.out.println(bedrockId);
        // return linkedPlayers.containsKey(bedrockId.toString());
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
        System.err.println("Floodgate: Error loading database: " + e.getMessage());
      // } finally {
        try {
          if(connection != null)
            connection.close();
        } catch(SQLException e2) {
          System.err.println("Floodgate: Could not close database: " + e);
        }
      }
      return false;
    }
    public static void load(Path configPath, FloodgateConfig config) {
        /* enabled = config.getPlayerLink().isEnabled();
        if (!enabled) {
          return;
        }
        allowLinking = config.getPlayerLink().isAllowLinking();
        linkCodeTimeout = config.getPlayerLink().getLinkCodeTimeout(); */
        enabled = true; // config.getPlayerLink().isEnabled();
        allowLinking = true;
        linkCodeTimeout = 300;

        Path databasePath = configPath.getParent().resolve("linked-players.db");
        System.out.println("Floodgate: Loading linked player database...");
        try { Class.forName("org.sqlite.JDBC"); } catch(ClassNotFoundException e) {}
        try {
          // create a database connection
          connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
          statement = connection.createStatement();
          statement.setQueryTimeout(30);  // set timeout to 30 sec.

          // statement.executeUpdate("drop table if exists LinkedPlayers");
          statement.executeUpdate("create table if not exists LinkedPlayers (bedrockId string, javaUniqueId string, javaUsername string)");
          // statement.executeUpdate("insert into LinkedPlayers values('fe2e9ec9-8ef2-34ac-bcb7-5661e9f97458', '7fb28d57-aafd-42d5-a705-cef18ab5313f', 'circuit10')");
          // statement.executeUpdate("insert into LinkedPlayers values('ee2e9ec9-8ef2-34ac-bcb7-5661e9f97458', '7fb28d57-aafd-42d5-a705-cef18ab5313f', 'circuit10')");
        } catch(SQLException e) {
          System.err.println("Floodgate: Error loading database: " + e.getMessage());
        // } finally {
          try {
            if(connection != null)
              connection.close();
          } catch(SQLException e2) {
            System.err.println("Floodgate: Could not close database: " + e2);
          }
        }

        System.out.println("Floodgate: Done!");

	/* byte[] mapData = Files.readAllBytes(Paths.get("/home/heath/floodgate-new/data.txt"));
        Map<String,String> linkedPlayerData = new HashMap<String, String>();

        ObjectMapper objectMapper = new ObjectMapper();
        linkedPlayerData = objectMapper.readValue(mapData, HashMap.class);

        Iterator it = myMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = pair.getKey().toString();
            List value = (List) pair.getValue();
            linkedPlayers.put(key, new LinkedPlayer(value.get(0).toString(), UUID.fromString(value.get(1).toString()), UUID.fromString(key)));
            it.remove(); // avoids a ConcurrentModificationException
        } */
        // System.out.println("Loaded " + linkedPlayers.size().toString() + " linked player(s)!");
    }
    public static String linkPlayer(String[] args, UUID uuid, String username) {
      if (enabled && allowLinking) {
        Random rand = new Random();
        if (isLinkedPlayer(uuid)) {
          return "§cYour account is already linked! If you want to link to a different account, run §6/unlinkaccount§c and try again.§f";
        }
        if (AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
          if (args.length != 2) {
            return "§cStart the process from Java! Usage: /linkaccount <gamertag>§f"; // Print the command usuage message
          }
          if (activeLinkRequests.containsKey(args[0])) {
            LinkRequest request = activeLinkRequests.get(args[0]);
            System.out.println(request.linkCode + args[1]);
            System.out.println(request.linkCode == args[1]);
            if (request.linkCode.equals(args[1])) {
                      activeLinkRequests.remove(args[0]); // Delete the request, whether it has expired or is successful
                      if (request.isExpired()) {
                        return "§cCode expired! Run §6/linkaccount§c again on Java.§f";
                      }
                      try {
                        PreparedStatement query = connection.prepareStatement("insert into LinkedPlayers values(?, ?, ?)");
                        query.setString(1, uuid.toString());
                        query.setString(2, request.javaUniqueId.toString());
                        query.setString(3, request.javaUsername);
                        query.executeUpdate();
                        // return "Link successful! BE UUID: " + uuid + ", JE UUID: " + request.javaUniqueId + ", JE username: " + request.javaUsername;
                        return "§aLink successful!§f";
                    } catch(SQLException e) {
                      System.err.println("Floodgate: Error loading databalinkCodeTimeoutse: " + e.getMessage());
                    // } finally {
                      try {
                        if(connection != null)
                          connection.close();
                      } catch(SQLException e2) {
                        System.err.println("Floodgate: Could not close database: " + e);
                      }
                    }
              } else {
                return "§cInvalid code! Try running §6/linkaccount§c again on Java.§f";
              }

          } else {
            return "§cThis player has not requested an account link! Please log in on Java and request one with §6/linkaccount§c.§f";
          }
        } else {
          if (args.length != 1) {
            return "§cUsage: /linkaccount <gamertag>§f";
          }
          String code = String.format("%04d", rand.nextInt(10000));
          String bedrockUsername = args[0];
          String messageUsername;
          if (bedrockUsername.charAt(0) == '*') {
            messageUsername = args[0].substring(1, args[0].length());
          } else {
            bedrockUsername = "*" + bedrockUsername; // Add a * to match the Floodgate username if it doesn't have one
            messageUsername = args[0];
          }
          bedrockUsername = bedrockUsername.substring(0, Math.min(bedrockUsername.length(), 16)); // Cut the name down to match the Floodgate username
          activeLinkRequests.put(username, new LinkRequest(username, uuid, code, args[0]));
          return "§aLog in as " + messageUsername + " on Bedrock and run §6/linkaccount " + username + " " + code + "§f";
        }
        return "§cUnknown error§f";
      } else {
        return "§cLinking is not enabled on this server§f";
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
        System.err.println("Floodgate: Error loading database: " + e.getMessage());
      // } finally {
        try {
          if(connection != null)
            connection.close();
        } catch(SQLException e2) {
          System.err.println("Floodgate: Could not close database: " + e);
        }
      }
      return "Error";
    } else {
      return "§cLinking is not enabled on this server§f";
    }
    }
}
