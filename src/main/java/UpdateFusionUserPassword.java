import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UpdateFusionUserPassword {
  @Option(name = "-zkConnect", usage = "Zookeepr connect string such as localhost:9983.", required = true)
  String zkConnect;

  @Option(name = "-fusionVersion", usage = "Fusion Version, for example: 4.1.0.", required = true)
  String fusionVersion;

  @Option(name = "-username", usage = "Username you want to set password for", required = true)
  String username;

  public void run() throws Exception {
    try (CuratorFramework client = CuratorFrameworkFactory
        .builder()
        .connectString(zkConnect)
        .sessionTimeoutMs(15000)
        .connectionTimeoutMs(15000)
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .build()) {
      client.start();

      String path = "/lwfusion/" + fusionVersion + "/proxy/user";

      List<String> users = client.getChildren().forPath(path);

      ObjectMapper objectMapper = new ObjectMapper();

      Map foundUser = null;
      String userPath = null;

      for (String user : users) {
        String json = new String(client.getData().forPath(path + "/" + user));
        Map userMap = objectMapper.readValue(json, Map.class);
        if (username.equalsIgnoreCase((String) userMap.get("username"))) {
          foundUser = userMap;
          userPath = path + "/" + user;
        }
      }
      if (foundUser == null) {
        System.err.println("Could not find user " + username + " in zookeeper.");
        System.exit(1);
      }

      Console console = System.console();
      if (console == null) {
        System.out.println("Couldn't get Console instance");
        System.exit(0);
      }

      char passwordArray[];
      char passwordArrayVerify[];

      while (true) {
        passwordArray = console.readPassword("Enter new password: ");
        passwordArrayVerify = console.readPassword("Enter new password one more time: ");
        if (Arrays.equals(passwordArray, passwordArrayVerify)) {
          break;
        }
        System.err.println("Passwords do not match! Try again.");
      }

      String bcryptHashString = BCrypt.withDefaults().hashToString(8, passwordArray);

      foundUser.put("password-hash", bcryptHashString);

      String newJson = objectMapper.writeValueAsString(foundUser);

      client.setData().forPath(userPath, newJson.getBytes());
    }
  }

  public static void main(String[] args) throws Exception {
    UpdateFusionUserPassword o = new UpdateFusionUserPassword();
    CmdLineParser parser = new CmdLineParser(o);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      parser.printUsage(System.out);
      System.out.println(e.getLocalizedMessage());
      throw e;
    }
    o.run();
  }
}
