package WorkLogger;

import javafx.scene.control.Alert;
import javafx.stage.Modality;
import org.controlsfx.dialog.Dialogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsManager {

    private final Logger log = Config.initLogger(SettingsManager.class.getName());

    public class Workspace{

        public String name;
        public String[] wifi_list; //list of wifi names
        public double earning = 0;

        /**
         * Init workspace
         * @param items [workspace name, wifi nae 1, wifi name 2, ... , wifi name n]
         */
        public Workspace(String[] items) {
            this.name = items[0];
            this.earning = Double.parseDouble(items[1]);
            this.wifi_list = Arrays.copyOfRange(items, 2, items.length);
        }

        /**
         * Init workspace with it's name
         * @param name
         */
        public Workspace(String name) {
            this.name = name;
        }
    }

    private Workspace[] workspaces;

    public Workspace[] getWorkspaces(){
        return workspaces;
    }

    public void setWorkspaces(Workspace[] wss){
        workspaces = wss;
    }

    public SettingsManager(){
        clean();
    }

    /**
     * Read data from  @CONFIG_FILE 'json'
     * @return true if OK else false
     */
    public boolean read(){
        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader(Config.CONFIG_FILE));
            JSONObject jsonObject = (JSONObject) obj;

            ArrayList<Workspace> workspaces = new ArrayList<>();

            JSONArray wssObj = (JSONArray) jsonObject.get("workspaces");
            Iterator<JSONObject> ws_iterator = wssObj.iterator();
            while (ws_iterator.hasNext()) {
                JSONObject wsobj = ws_iterator.next();
                String name = (String) wsobj.get("name");

                Workspace workspace = new Workspace(name);

                ArrayList<String> wifi_list = new ArrayList<>();
                JSONArray wifi_list_obj = (JSONArray) wsobj.get("list");
                Iterator<String> wifi_iterator = wifi_list_obj.iterator();
                while (wifi_iterator.hasNext()) {
                    String wifi_name = wifi_iterator.next();
                    wifi_list.add(wifi_name);
                }

                workspace.earning = (double)wsobj.getOrDefault("earning", 0);
                workspace.wifi_list = wifi_list.toArray(new String[wifi_list.size()]);
                workspaces.add(workspace);
            }

            setWorkspaces( workspaces.toArray(new Workspace[workspaces.size()]) );

        } catch (FileNotFoundException e){
            log.log(Level.INFO, "read > FileNotFoundException");
        } catch (Exception e) {
            log.log(Level.SEVERE, e.toString(), e);

            Alert dlg = new Alert(Alert.AlertType.WARNING, "");
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.initOwner(null);
            dlg.setTitle("Error");
            dlg.getDialogPane().setContentText("File location \"" + Config.CONFIG_FILE + "\"");
            dlg.getDialogPane().setHeaderText("Damaged config file.");
            dlg.show();
        }
        return false;
    }

    /**
     * Save data of this instance @CONFIG_FILE "json"
     * @return true if OK else false
     */
    public boolean save(){

        JSONArray workspaces_list = new JSONArray();
        for(Workspace workspace: workspaces){

            JSONObject objWorkspace = new JSONObject();
            JSONArray list = new JSONArray();
            Collections.addAll(list, workspace.wifi_list);
            objWorkspace.put("name", workspace.name);
            objWorkspace.put("list", list);
            objWorkspace.put("earning", workspace.earning);

            workspaces_list.add(objWorkspace);
        }

        JSONObject obj = new JSONObject();
        obj.put("workspaces", workspaces_list);

        try {
            FileWriter file = new FileWriter(Config.CONFIG_FILE);
            file.write(obj.toJSONString());
            file.flush();
            file.close();
            return true;

        } catch (IOException e) {
            log.log(Level.SEVERE, e.toString(), e);
        }

        return false;
    }

    public void clean(){
        workspaces = new Workspace[]{};
    }
}
