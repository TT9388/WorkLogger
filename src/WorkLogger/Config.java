package WorkLogger;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Config {

    public static final Level LOG_LEVEL = Level.ALL;

    public static final String BASE_PATH = "app\\";
    public static final String WORKSPACE_LOCATION = BASE_PATH + "ws_data\\";
    public static final String LOCK_FILE = BASE_PATH + "lock"; //name of lock file "used at login event"
    public static final String CONFIG_FILE = BASE_PATH + "settings.json"; //file with settings


    public static Logger initLogger(String className){

        new File(BASE_PATH + "log").mkdirs();

        Logger log = Logger.getLogger(EventLogger.class.getName());
        try {
            FileHandler fh = new FileHandler(BASE_PATH + "log/" + className + ".log", true);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
            log.setLevel(Config.LOG_LEVEL);
        } catch (IOException | SecurityException e) {
            //e.printStackTrace();
        }
        return log;
    }
}
