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

    config(String configFile) {
        this.configFile = configFile;
        log = Logger.getLogger(config.class.getName());
    }

    public String load(String key) throws IOException, NullPointerException {
        prop = new Properties();
        input = new FileInputStream(configFile);
        prop.load(input);
        input.close();
        /*if (Boolean.valueOf(prop.getProperty(key))) {
                throw new NullPointerException("Not found key value in config file!");
        } else {
            return prop.getProperty(key);
        }*/

        if (prop.getProperty(key).length() > 0) {
            throw new NullPointerException("Not found key value in config file!");
        } else {
            return prop.getProperty(key);
        }
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
        }
    }
}
