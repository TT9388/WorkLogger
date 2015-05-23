package WorkLogger;

import org.joda.time.DateTime;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventLogger {

    private static final Logger log = Config.initLogger(EventLogger.class.getName());

    /**
     * Triggered from windows task scheduler
     * @param args [login | logout]
     */
    public static void main(String[] args) {

        if(args.length == 1) {
            log.log(Level.INFO, "Action: " + args[0]);
            if(args[0].equals("login"))
                logLoginEvent(null);
            else if(args[0].equals("logout")){
                logLogoutEvent();
            }
        } else {
            //el.log.log(Level.WARNING, "Bad arguments!");
        }
    }

    /**
     * Returns name of currently used wifi, if failed to get name or no connection is made returns null.
     * @return String
     */
    public static String getWifiName() {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "netsh wlan show interfaces");
        builder.redirectErrorStream(true);
        try{
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                String[] lineData = line.split(":");
                if(lineData.length == 2){
                    lineData[0] = lineData[0].trim();
                    if (lineData[0].equals("SSID")){
                        return lineData[1].trim();
                    }
                }
            }
        } catch(Exception e){
            log.log( Level.SEVERE, e.toString(), e );
        }
        return null;
    }

    /**
     * Save wifi name to @LOCK_FILE at login event
     * @param forceWifiName - (optional) can be null
     * @return true if log successful else false
     */
    public static boolean logLoginEvent(String forceWifiName){
        String wifiName = (forceWifiName == null) ? getWifiName() : forceWifiName;
        if(wifiName != null) {
            try {
                FileWriter fw = new FileWriter(Config.LOCK_FILE);
                fw.write(String.format("%d\n%s\n%s", new DateTime().getMillis(), wifiName, new DateTime().toString()));
                fw.close();
                //Notifications.create()
                //        .text("Workstation monitoring started.")
                //        .showInformation();
                return  true;
            } catch (IOException e) {
                log.log(Level.SEVERE, e.toString(), e);
            }
        }
        return false;
    }

    public static String[] getLockFileContent(boolean deleteLockFile){
        File lockFile = new File(Config.LOCK_FILE);
        if(lockFile.exists()){
            try {
                Scanner s = new Scanner(lockFile);
                String[] r = null;
                try {
                    r = new String[]{ s.nextLine(), s.nextLine() };
                } catch (Exception e){
                    log.log( Level.SEVERE, e.toString(), e);
                }
                s.close();
                if(deleteLockFile) {
                    if (!lockFile.delete()) {
                        log.log(Level.INFO, "Couldn't delete lock file: " + Config.LOCK_FILE);
                    }
                }
                return r;
            } catch (FileNotFoundException e) {
                log.log( Level.SEVERE, e.toString(), e);
            }
        } else {
            log.log( Level.INFO, "logLogoutEvent > no lock file: " + Config.LOCK_FILE );
        }
        return null;
    }

    /**
     * Save work done to database. Method reads work start time and location from @LOCK_FILE.
     * @return true if log successful else false
     */
    public static boolean logLogoutEvent(){
        // TODO UPGRADE idea check if is same wifi (problem if windows in shutdown mode or sleep)
        String[] data = getLockFileContent(true);
        if(data != null && data.length >= 2) {
            long timestamp = Long.parseLong(data[0]);
            String wifiName = data[1];
            log.log(Level.INFO, "logLogoutEvent: File data > wifiName: " + wifiName + " timestamp: " + timestamp);
            return logToWorkspace(getWorkspace(wifiName), timestamp);
        }
        return false;
    }

    /**
     * Get workspace name.
     * @param wifiName - name of wifi
     * @return workspace name or null if no workspace found under that wifi name or if wifiName parameter is nul or ""
     */
    public static String getWorkspace(String wifiName){
        if(wifiName != null && !wifiName.isEmpty()) {
            SettingsManager settingsManager = new SettingsManager();
            settingsManager.read();
            for(SettingsManager.Workspace ws: settingsManager.getWorkspaces()){
                for(String wifi: ws.wifi_list){
                    if(wifiName.equals(wifi)){
                        return ws.name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Log work to database. From timestamp to now.
     * @param workspace - w-lan hostname
     * @param startTimestamp - timestamp of login time in milliseconds
     * @return true if OK, false if failed
     */
    public static boolean logToWorkspace(String workspace, long startTimestamp){

        log.log( Level.INFO, "logToWorkspace > workspace: " + workspace);

        if(workspace == null) {
            return false;
        }

        new File(Config.WORKSPACE_LOCATION).mkdir();

        try (FileOutputStream output = new FileOutputStream(Config.WORKSPACE_LOCATION + workspace + ".bin", true)) {
            output.write(ByteBuffer.allocate(8).putLong(startTimestamp).array());              // start time to bin
            output.write(ByteBuffer.allocate(8).putLong(new DateTime().getMillis()).array());  // end   time to bin
            output.close();
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, e.toString(), e);
        }

        return false;
    }
}
