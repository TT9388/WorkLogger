package si.modri.WorkLogger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getResource("layout/activityMain.fxml"));
        primaryStage.setTitle("Work logger");
        primaryStage.setScene(new Scene(root, 1000, 700));
        //primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        if(args.length > 0){
            EventLogger.main(args);
            System.exit(0);
        } else {
            launch(args);
        }
    }

}