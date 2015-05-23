package si.modri.WorkLogger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML public TextArea ta_workspaces;
    @FXML public Button btn_save_workspaces_config;
    @FXML public TabPane tp_workspaces;
    @FXML public VBox vb_workspaces_list;
    @FXML public Label l_username;
    @FXML public Button btn_logg_clear;
    @FXML public Tab tab_start_page;
    @FXML public TitledPane tp_select_workspace;
    @FXML public Accordion accordion_startp;
    @FXML private ComboBox cb_wifi;
    @FXML private Button btn_log_stop;
    @FXML private Button btn_log_start;

    SettingsManager settingsManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshSettingsManager();
        btn_save_workspaces_config.setDisable(true);
        ta_workspaces.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue))
                btn_save_workspaces_config.setDisable(false);
        });
        try {
            l_username.setText("Current user: " + InetAddress.getLocalHost().getHostName() + "/" + System.getProperty("user.name"));
        } catch (UnknownHostException e) {
            l_username.setText("Current user: " + "[xml generate will fail!]/" + System.getProperty("user.name"));
        }

        accordion_startp.setExpandedPane(tp_select_workspace);
    }

    public void clearWorkspaceList(){
        vb_workspaces_list.getChildren().clear();
    }

    public void addWorkspaceToList(String workspaceName){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("layout/workspaceItem.fxml"));
            Pane pane = new Pane();
            pane.getChildren().add(loader.load());
            WorkspaceItemController controller = loader.<WorkspaceItemController>getController();
            controller.init(workspaceName);

            //pane.setStyle("-fx-background-color: rgba(0,0,0,0.02)");
            pane.setCursor(Cursor.HAND);
            pane.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    openWorkspaceTab(workspaceName);
                }
            });

            vb_workspaces_list.getChildren().add(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open new workspace tab.
     * @param workspaceName
     */
    public void openWorkspaceTab( String workspaceName){
        for(Tab t: tp_workspaces.getTabs()){
            String id = t.getId();
            if(id != null && id.equals("tab-" + workspaceName)){
                tp_workspaces.getSelectionModel().select(t);
                return;
            }
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("layout/activityTabWorkspace.fxml"));
            Tab tab = new Tab(workspaceName);
            tab.setId("tab-" + workspaceName);
            tab.setClosable(true);
            tab.setContent(loader.load());
            TabWorkspaceController controller = loader.<TabWorkspaceController>getController();
            controller.loadWorkspace(workspaceName, tab, settingsManager);

            tp_workspaces.getTabs().add(tab);
            tp_workspaces.getSelectionModel().select(tab);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fill gui elements with settings data.
     */
    public void refreshSettingsManager(){
        settingsManager = new SettingsManager();
        settingsManager.read();

        StringBuilder txtWss = new StringBuilder();

        String[] lockFileData = EventLogger.getLockFileContent(false);
        if(lockFileData == null){
            btn_log_stop.setDisable(true);
            btn_logg_clear.setDisable(true);
        } else if(lockFileData.length >= 2) {
            cb_wifi.setValue(lockFileData[1]);
            cb_wifi.setDisable(true);
            btn_log_start.setDisable(true);
        }

        clearWorkspaceList();

        //cb_wifi & ta_workspaces & cb_workspace
        ObservableList<String> wifi_options = FXCollections.observableArrayList();
        final ObservableList<String> workspace_options = FXCollections.observableArrayList();
        for(SettingsManager.Workspace ws: settingsManager.getWorkspaces()){
            txtWss.append(ws.name + ";" + ws.earning);
            workspace_options.add(ws.name);
            addWorkspaceToList(ws.name);
            for(String wifi: ws.wifi_list){
                txtWss.append(";").append(wifi);
                wifi_options.add(wifi);
            }
            txtWss.append("\n");
        }
        cb_wifi.setItems(wifi_options);
        ta_workspaces.setText(txtWss.toString());
    }

    /**
     * Start fake logging, for selected wifi name.
     */
    public void startLogging() {
        //EventLogger.main(new String[]{"login", cb_wifi.getValue().toString()});
        if(cb_wifi.getValue() != null) {
            String selectedWifi = cb_wifi.getValue().toString();
            if (!selectedWifi.isEmpty()) {
                EventLogger el = new EventLogger();
                if(el.logLoginEvent(selectedWifi)){
                    Notifications.create()
                            .text("Workstation monitoring started.")
                            .showInformation();
                }
                btn_log_stop.setDisable(false);
                btn_log_start.setDisable(true);
                cb_wifi.setDisable(true);
                btn_logg_clear.setDisable(false);
            }
        }
    }

    /**
     * Stop logging of active lock file.
     */
    public void stopLogging() {
        //EventLogger.main(new String[]{"logout", cb_wifi.getValue().toString()});
        EventLogger el = new EventLogger();
        if(el.logLogoutEvent()){
            Notifications.create()
                    .text("Workstation monitoring stopped.")
                    .showInformation();
        }
        btn_log_stop.setDisable(true);
        btn_log_start.setDisable(false);
        cb_wifi.setDisable(false);
        btn_logg_clear.setDisable(true);
    }

    /**
     * Clear logging of active lock file without saving.
     */
    public void clearlogging() {
        File lockFile = new File(Config.LOCK_FILE);
        if(lockFile.delete()){
            Notifications.create()
                    .text("Workstation monitoring canceled.")
                    .showInformation();
        } else {
            Notifications.create()
                    .title("Error")
                    .text("Workstation monitoring couldn't be canceled.")
                    .showWarning();
        }
        btn_log_stop.setDisable(true);
        btn_log_start.setDisable(false);
        btn_logg_clear.setDisable(true);
        cb_wifi.setDisable(false);
    }

    /**
     * Save workspaces to settings file
     */
    public void saveWorkspaces(){
        ArrayList<SettingsManager.Workspace> workspaces = new ArrayList<>();
        String[] txtw = ta_workspaces.getText().split("\n");
        for(String ws: txtw){
            String[] items = ws.split(";");
            if(items.length > 1 && !items[0].equals("")){
                workspaces.add(new SettingsManager().new Workspace(items));
            }
        }
        settingsManager.setWorkspaces( workspaces.toArray(new SettingsManager.Workspace[workspaces.size()]) );
        if(settingsManager.save()){
            Notifications.create()
                    .text("Settings saved.")
                    .showInformation();
        } else {
            Notifications.create()
                    .title("Error")
                    .text("Failed to save settings.")
                    .showWarning();
        }
        refreshSettingsManager();
        btn_save_workspaces_config.setDisable(true);
    }

    public void generateBat(ActionEvent actionEvent) {
        String location = "";
        try {
            location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
            location = "cd /D \""+location+"\"";
        } catch (URISyntaxException e) {
            Notifications.create()
                    .title("Error")
                    .text("Couldn't get application location. *.bat files might not work.")
                    .showWarning();
        }
        try {
            PrintWriter writer = new PrintWriter("login.bat", "UTF-8");
            writer.println(location);
            writer.println("java -jar WorkLogger.jar login");
            writer.close();
            Notifications.create()
                    .text("File login.bat created.")
                    .showInformation();
        } catch (FileNotFoundException | UnsupportedEncodingException e){
            Notifications.create()
                    .title("Error")
                    .text("Failed to create login.bat")
                    .showWarning();
        }
        try {
            PrintWriter writer = new PrintWriter("logout.bat", "UTF-8");
            writer.println(location);
            writer.println("java -jar WorkLogger.jar logout");
            writer.close();
            Notifications.create()
                    .text("File logout.bat created.")
                    .showInformation();
        } catch (FileNotFoundException | UnsupportedEncodingException e){
            Notifications.create()
                    .title("Error")
                    .text("Failed to create logout.bat")
                    .showWarning();
        }
    }

    public void generateXml(ActionEvent actionEvent) {
        boolean a = generateXmlFile("WorkLogger - login.xml");
        boolean b = generateXmlFile("WorkLogger - logout.xml");
        if(a || b){
            try {
                Desktop.getDesktop().open(new File(Config.XML_OUT_LOCATION));
            } catch (IOException e) {
            }
        }
    }

    public boolean generateXmlFile(String name){
        String result = null;
        try {
            result = IOUtils.toString(getClass().getResourceAsStream("resource/" + name), "UTF-16");
            result = processXmlImport(result);
        }
        catch (IOException e){}

        try {
            new File(Config.XML_OUT_LOCATION).mkdir();
            File f = new File(Config.XML_OUT_LOCATION + "\\" + name);
            f.delete();
            FileUtils.write(f, result, "UTF-16");
        } catch (IOException e) {
            result = null;
        }

        if(result != null){
            Notifications.create()
                    .text("File " + name + " created.")
                    .showInformation();
            return true;
        } else {
            Notifications.create()
                    .title("Error")
                    .text("Failed to create " + name)
                    .showWarning();
            return false;
        }
    }

    public String processXmlImport(String txt){
        String location;
        try {
            location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
        } catch (URISyntaxException e) {
            return null;
        }
        String user;
        try {
            user = InetAddress.getLocalHost().getHostName() + "\\" + System.getProperty("user.name");
        } catch (UnknownHostException e) {
            return null;
        }
        txt = txt.replace("[[***USER***]]", user);
        txt = txt.replace("[[***APP-PATH***]]", location);
        return txt;
    }

    public void onGithubLinkClick(ActionEvent actionEvent) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI("https://github.com/TT9388/WorkLogger"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
