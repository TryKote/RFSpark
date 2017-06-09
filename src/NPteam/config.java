package NPteam;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * RFSpark
 * Created by kote on 09.06.17.
 */
public class config {
    private Properties prop;
    private InputStream input;
    private OutputStream output;
    private String configFile;
    private static Logger log;
    private static remoteLog rLog;

    config(String configFile) {
        this.configFile = configFile;
        log = Logger.getLogger(config.class.getName());
        rLog = new remoteLog("http://TryKote.suroot.com/");
    }

    public String load(String key) throws NullPointerException {
        try {
            prop = new Properties();
            input = new FileInputStream(configFile);
            prop.load(input);
            input.close();
            if (Boolean.valueOf(prop.getProperty(key))) {
                throw new NullPointerException("Not found key value in config file!");
            } else {
                return prop.getProperty(key);
            }
        } catch (FileNotFoundException e) {
            log.warning(e.getMessage());
            rLog.send(e.getMessage());
            //e.printStackTrace();
        } catch (IOException e) {
            log.warning("Can't load config from file! " + e.getMessage());
            rLog.send("Can't load config from file! " + e.getMessage());
            //e.printStackTrace();
        }
        return null;
    }

    public void save(String key, String val, String comment) {
        try {
            prop = new Properties();
            output = new FileOutputStream(configFile, true);
            prop.put(key, val);
            prop.store(output, comment);
            output.flush();
            output.close();
        } catch (IOException e) {
            log.warning("Can't add pair to config! " + e.getMessage());
            rLog.send("Can't add pair to config! " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public void save(String key, String val) {
        try {
            prop = new Properties();
            output = new FileOutputStream(configFile, true);
            prop.put(key, val);
            prop.store(output, "");
            output.flush();
            output.close();
        } catch (IOException e) {
            log.warning("Can't add pair to config! " + e.getMessage());
            rLog.send("Can't add pair to config! " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public void recreate() {
        File file = new File(configFile);
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            log.warning("Can't recreate config! " + e.getMessage());
            rLog.send("Can't recreate config! " + e.getMessage());
        }
    }
}
