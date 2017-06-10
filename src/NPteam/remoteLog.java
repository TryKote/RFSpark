package NPteam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * RFSpark
 * Created by kote on 09.06.17.
 */
public class remoteLog {
    private URL ServerRequest;
    private String strServerURL;
    private String Message;
    private String User;
    private Logger log;
    private final String DEFAULT_SERVER = "http://TryKote.suroot.com/";

    remoteLog(String ServerURL) {
        log = Logger.getLogger(remoteLog.class.getName());
        User = System.getProperty("user.name");
        if (ServerURL.endsWith("/")) {
            strServerURL = ServerURL + "index.php?user=" + User + "&msg=";
            //this.ServerRequest = new URL(ServerURL + "index.php?user=" + User + "&msg=");
        } else {
            strServerURL = ServerURL + "/index.php?user=" + User + "&msg=";
            //this.ServerRequest = new URL(ServerURL + "/index.php?user=" + User + "&msg=");
        }
    }
    remoteLog() {
        log = Logger.getLogger(remoteLog.class.getName());
        config cfg = new config("config");
        String ServerURL;
        try {
            ServerURL = cfg.load("ServerURL");
        } catch (Exception e) {
            log.warning("Using default remote server! " + e.getMessage() + "\nDefault server: " + DEFAULT_SERVER);
            ServerURL = DEFAULT_SERVER;
        }
        User = System.getProperty("user.name");
        if (ServerURL.endsWith("/")) {
            strServerURL = ServerURL + "index.php?user=" + User + "&msg=";
            //this.ServerRequest = new URL(ServerURL + "index.php?user=" + User + "&msg=");
        } else {
            strServerURL = ServerURL + "/index.php?user=" + User + "&msg=";
            //this.ServerRequest = new URL(ServerURL + "/index.php?user=" + User + "&msg=");
        }
    }

    String send(String msg) {
        try {

            ServerRequest = new URL(strServerURL + URLEncoder.encode(msg, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) ServerRequest.openConnection();
            con.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line,
                   ans = new String();
            while ((line = rd.readLine()) != null) {
                ans += line;
            }
            rd.close();
            return ans;
        } catch (IOException e) {
            log.warning("Can't send message! " + e.getLocalizedMessage());
            //e.printStackTrace();
            return null;
        }
    }
}
