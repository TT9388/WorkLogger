package WorkLogger;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class WorkspaceItemController {

    @FXML public Canvas c_icon;
    @FXML public Label l_name;

    public void init(String s){
        l_name.setText(s);
        GraphicsContext gc = c_icon.getGraphicsContext2D();
        int c = s.hashCode();
        double r = (c % 2 == 0) ? 0.2 : 0.5;
        double g = (c % 3 == 0) ? 0.2 : 0.5;
        double b = (c % 5 == 0) ? 0.2 : 0.5;
        gc.setFill(new Color(r, g, b, 1));
        gc.fillRoundRect(0, 0, c_icon.getWidth(), c_icon.getHeight(), 10, 10);
        gc.setStroke(Color.WHITESMOKE);
        gc.strokeText(s.substring(0, (s.length() >= 2) ? 2 : s.length()).toUpperCase(), 5, 15);
    }

}
