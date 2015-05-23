package si.modri.WorkLogger;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class CanvasWeekDraw {

    private Canvas canvas;
    private GraphicsContext gc;
    private TreeMap<String, WorkspaceManager.TimeEntry> data = null; //while null draw will be skipped
    private DateTime start;
    private DateTime end;

    private double dayWidth = 0;
    private double minuteHeight = 0;

    private final double offestLeft = 30;
    private final double offestBottom = 30;

    private boolean forceCancelDraw = false;

    public CanvasWeekDraw(Canvas canvas) {
        this.canvas = canvas;
        this.canvas.widthProperty().addListener(observable -> { draw(); });
        this.canvas.heightProperty().addListener(observable -> { draw(); });
        gc = canvas.getGraphicsContext2D();
        resetRange();
    }

    public void resetRange(){
        start = new DateTime();
        start = start.minusDays(start.getDayOfWeek() - 1).toDateTime();
        end = start.toDateTime().plusDays(6);
        start = start.minusMillis(start.millisOfDay().get());
        end = end.minusMillis(end.millisOfDay().get());
        draw();
    }

    public void setData(TreeMap<String,WorkspaceManager.TimeEntry> data) {
        this.data = data;
    }

    public boolean setStart(LocalDate localDate){
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        start = new DateTime(date);
        if(start.isBefore(end)){
            draw();
            return true;
        }
        return false;
    }

    public boolean setEnd(LocalDate localDate){
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        end = new DateTime(date);
        if(start.isBefore(end)){
            draw();
            return true;
        }
        return false;
    }

    public DateTime getEnd() {
        return end;
    }

    public DateTime getStart() {
        return start;
    }

    public void setForceCancelDraw(boolean forceCancelDraw) {
        this.forceCancelDraw = forceCancelDraw;
    }

    /**
     * Draw graph.
     */
    public void draw(){

        if(data == null)
            return;

        if(forceCancelDraw) {
            forceCancelDraw = false;
            return;
        }

        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.CORAL);

        int days = Days.daysBetween(start, end.plusDays(1)).getDays();
        dayWidth = (canvasWidth - offestLeft) / days;
        minuteHeight = (canvasHeight - offestBottom) / (24 * 60);

        gc.setStroke(new Color(0, 0, 0 , 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(offestLeft, 0, offestLeft, canvasHeight);
        gc.strokeLine(0, canvasHeight - offestBottom, canvasWidth, canvasHeight - offestBottom);

        gc.setStroke(new Color(0, 0, 0 , 0.1));
        for(int i = 1; i < days; i++){
            gc.strokeLine(offestLeft + dayWidth * i, 0, offestLeft  + dayWidth * i, canvasHeight - offestBottom);
        }

        double hourHeight = minuteHeight * 60;
        gc.setFont(new Font("System", (hourHeight < 12) ? (hourHeight - 1) : 12));
        for(int i = 0; i < 24; i++){
            gc.setStroke(new Color(0, 0, 0 , 0.03));
            double y = i * hourHeight;
            gc.strokeLine(offestLeft, y, canvasWidth, y);
            gc.setStroke(Color.GRAY);
            gc.strokeText(String.format("%2d", i), 10, y + hourHeight/2);
        }

        DateTimeFormatter dtf = DateTimeFormat.forPattern( (days <= 7) ? "EE, d.MM" : "d.MM");
        DateTime tmpTime = new DateTime(start);
        int dayC = 0;
        while(tmpTime.isBefore(end.plusDays(1))){
            gc.strokeText(dtf.print(tmpTime), offestLeft  + dayWidth * dayC + 5, canvasHeight - offestBottom/2);
            dayC++;
            tmpTime = tmpTime.plusDays(1);
        }

        long endTime = end.plusDays(1).getMillis();
        for(Map.Entry<String, WorkspaceManager.TimeEntry> entry : data.tailMap( start.getMillis() + "" ).entrySet()) {
            WorkspaceManager.TimeEntry timeEntry = entry.getValue();
            if(timeEntry.startTime.getMillis() > endTime) {
                break;
            }
            drawDay(timeEntry.startTime, timeEntry.endTime);
        }
    }

    /**
     * Draw day entry if range param not in one days split it recursively.
     * @param eStartTime
     * @param eEndTime
     */
    private void drawDay(DateTime eStartTime, DateTime eEndTime){

        DateTime zeroStart = eStartTime.minusMillis(eStartTime.getMillisOfDay());
        DateTime zeroEnd = eEndTime.minusMillis(eEndTime.getMillisOfDay());
        if(zeroStart.getMillis() != zeroEnd.getMillis()){
            zeroStart = zeroStart.plusDays(1);
            drawDay(zeroStart, eEndTime.toDateTime());
            eEndTime = zeroStart;
        }

        int minLen = Minutes.minutesBetween(eStartTime, eEndTime).getMinutes();
        int day = Days.daysBetween(start, eStartTime).getDays();

        gc.fillRect(offestLeft + day * dayWidth, eStartTime.getMinuteOfDay() * minuteHeight, dayWidth, minLen * minuteHeight);
    }
}