package WorkLogger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.joda.time.Minutes;

import java.net.URL;
import java.time.*;
import java.util.Map;
import java.util.ResourceBundle;

public class TabWorkspaceController implements Initializable {

    @FXML public TableView tv_entries;
    @FXML public TableColumn tc_start;
    @FXML public TableColumn tc_end;
    @FXML public TableColumn tc_duration;
    @FXML public Canvas c_week_graph;
    @FXML public DatePicker dp_from_date;
    @FXML public DatePicker dp_to_date;
    @FXML public Button btn_save;
    @FXML public TextField tf_earning;
    @FXML public Button btn_add_entry;
    @FXML public Label l_totalTime;
    @FXML public Label l_totalEarning;
    //@FXML public Label l_avgPerDay;
    //@FXML public Label l_maxDay;
    //@FXML public Label l_minDay;

    Tab ownerTab;

    WorkspaceManager workspaceManager;
    CanvasWeekDraw canvasWeekDraw;
    ValidationSupport validationSupport = new ValidationSupport();

    private String workspaceName;
    private double earning;

    /**
     * Load data for passed workspace.
     * @param workspaceName
     */
    public void loadWorkspace(String workspaceName, Tab ownerTab, SettingsManager settingsManager){
        this.workspaceName = workspaceName;
        workspaceManager = new WorkspaceManager(workspaceName + ".bin", true);
        for(SettingsManager.Workspace w: settingsManager.getWorkspaces()){
            if(w.name.equals(workspaceName))
                tf_earning.setText(w.earning + "");
        }

        fillEntriesTable();
        canvasWeekDraw.setData(workspaceManager.entries);
        canvasWeekDraw.draw();
        this.ownerTab = ownerTab;
        calculateStatistics();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //init table view
        tc_start.setCellFactory(TextFieldTableCell.forTableColumn());
        tc_start.setCellValueFactory(
                new PropertyValueFactory<WorkspaceManager.TimeEntry, String>("tStartTime")
        );
        tc_end.setCellFactory(TextFieldTableCell.forTableColumn());
        tc_end.setCellValueFactory(
                new PropertyValueFactory<WorkspaceManager.TimeEntry, String>("tEndTime")
        );
        tc_duration.setCellFactory(TextFieldTableCell.forTableColumn());
        tc_duration.setCellValueFactory(
                new PropertyValueFactory<WorkspaceManager.TimeEntry, String>("tDuration")
        );

        canvasWeekDraw = new CanvasWeekDraw(c_week_graph);
        onResetRangeClick(false);

        /*ap_week_graph_container_width = ap_week_graph_container.getWidth();
        ap_week_graph_container_height = ap_week_graph_container.getHeight();

        ap_week_graph_container.addEventHandler(EventType.ROOT, event -> {
            if(ap_week_graph_container_width != ap_week_graph_container.getWidth() || ap_week_graph_container_height != ap_week_graph_container.getHeight()){
                ap_week_graph_container_width = ap_week_graph_container.getWidth();
                ap_week_graph_container_height = ap_week_graph_container.getHeight();
                c_week_graph.setWidth(ap_week_graph_container_width);
                c_week_graph.setHeight(ap_week_graph_container_height);
            }
        });*/

        validationSupport.registerValidator(dp_from_date, false, (Control c, LocalDate newValue) ->
                ValidationResult.fromWarningIf( dp_from_date, "'From date' should be before 'to date'.", !dp_from_date.getValue().isBefore(dp_to_date.getValue())));
        validationSupport.registerValidator(dp_to_date, false, (Control c, LocalDate newValue) ->
                ValidationResult.fromWarningIf( dp_to_date, "'To date' should be after 'from date'", !dp_from_date.getValue().isBefore(dp_to_date.getValue())));
    }

    /**
     * Refresh displayed data for currently selected workspace.
     */
    public void onRefreshWorkspaceClick(){
        workspaceManager.read();
        fillEntriesTable();
    }

    /**
     * Put workspace data into table 'tv_entries'.
     */
    public void fillEntriesTable() {
        ObservableList<WorkspaceManager.TimeEntry> data = FXCollections.observableArrayList();
        for(Map.Entry<String, WorkspaceManager.TimeEntry> e: workspaceManager.entries.entrySet()){
            data.add(e.getValue());
        }
        tv_entries.setItems(data);
    }

    /**
     * Edit entry
     * @param stCellEditEvent
     */
    public void onEntriesColumnEditCommit(TableColumn.CellEditEvent<WorkspaceManager.TimeEntry, String> stCellEditEvent) {
        String colId = stCellEditEvent.getTableColumn().getId();

        if(colId.equals("tc_start")){
            stCellEditEvent.getRowValue().settStartTime(stCellEditEvent.getNewValue());
        } else if(colId.equals("tc_end")){
            stCellEditEvent.getRowValue().settEndTime(stCellEditEvent.getNewValue());
        }

        refreshTableEntriesView();
        btn_save.setDisable(false);
    }

    /**
     * Trick for refreshing table (redraw).
     */
    private void refreshTableEntriesView(){
        tc_duration.setVisible(false);
        tc_duration.setVisible(true);
        canvasWeekDraw.draw();
    }

    /**
     * Save.
     */
    public void onSaveClick() {
        if(!workspaceManager.save()){
            Dialogs.create()
                    .lightweight()
                    .owner(ownerTab)
                    .title("Error")
                    .message("Couldn't save.")
                    .showWarning();
            /*Alert dlg = new Alert(Alert.AlertType.WARNING, "");
            dlg.initModality(Modality.NONE);
            dlg.initOwner(null);
            dlg.setTitle("Error");
            dlg.getDialogPane().setContentText("Couldn't save.");
            dlg.getDialogPane().setHeaderText("Damaged config file.");
            dlg.show();*/
        } else {
            Notifications.create()
                    .text("Workspace " + workspaceName + " saved.")
                    .showInformation();
            btn_save.setDisable(true);
        }
    }

    public void onFromDateSelect() {
        if(canvasWeekDraw.setStart(dp_from_date.getValue())){
            calculateStatistics();
        }
    }

    public void onToDateSelect() {
        if(canvasWeekDraw.setEnd(dp_to_date.getValue())) {
            calculateStatistics();
        }
    }

    /**
     * Reset range from to
     */
    public void onResetRangeClick() {
        onResetRangeClick(false);
    }

    /**
     * Reset range from to
     * @param cancelDraw DEPRECIATED
     */
    public void onResetRangeClick(boolean cancelDraw) {
        if(cancelDraw)
            canvasWeekDraw.setForceCancelDraw(true); //do not redraw // FIXME CURRENTLY NOT USED
        canvasWeekDraw.resetRange();
        LocalDate date1 = canvasWeekDraw.getStart().toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        dp_from_date.setValue(date1);

        LocalDate date2 = canvasWeekDraw.getEnd().toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        dp_to_date.setValue(date2);
        calculateStatistics();
    }

    public void calculateStatistics() {

        if(workspaceManager == null)
            return;

        long totalTime = 0;
        int totalEarning = 0;
        double earningPerMinute = earning / 60;

        long endTime = canvasWeekDraw.getEnd().getMillis();
        for(Map.Entry<String, WorkspaceManager.TimeEntry> entry : workspaceManager.entries.tailMap( canvasWeekDraw.getStart().getMillis() + "" ).entrySet()) {
            WorkspaceManager.TimeEntry timeEntry = entry.getValue();
            if(timeEntry.startTime.getMillis() > endTime) {
                break;
            }
            int minuteDuration = Minutes.minutesBetween(timeEntry.startTime, timeEntry.endTime).getMinutes();
            totalTime += minuteDuration;
            totalEarning += minuteDuration * earningPerMinute;
        }
        l_totalTime.setText(WorkspaceManager.getDuration(totalTime * 1000 * 60));
        l_totalEarning.setText(totalEarning + " \u20ac");
        //l_avgPerDay.setText("");
        //l_maxDay.setText("");
        //l_minDay.setText("");
    }

    public void reCalculateStatistics(ActionEvent actionEvent) {
        earning = Double.parseDouble(tf_earning.getText());
        calculateStatistics();
    }
}