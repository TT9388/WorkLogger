package WorkLogger;

import javafx.beans.property.SimpleStringProperty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkspaceManager {

    private static final Logger log = Config.initLogger(WorkspaceManager.class.getName());

    private String name; //name of workspace

    /**
     * One log entry instance.
     */
    public class TimeEntry{

        //region TABLE STUFF

        private SimpleStringProperty tStartTime;
        private SimpleStringProperty tEndTime;
        private SimpleStringProperty tDuration;

        public String gettDuration() {
            return tDuration.get();
        }

        public SimpleStringProperty tDurationProperty() {
            return tDuration;
        }

        public void settDuration(String tDuration) {
            this.tDuration.set(tDuration);
        }

        public String gettEndTime() {
            return tEndTime.get();
        }

        public SimpleStringProperty tEndTimeProperty() {
            return tEndTime;
        }

        public void settEndTime(String tEndTime) {
            DateTime tmp = stringToDateTime(tEndTime);
            if(tmp != null) {
                endTime = tmp;
                recalculate();
            }
        }

        public String gettStartTime() {
            return tStartTime.get();
        }

        public SimpleStringProperty tStartTimeProperty() {
            return tStartTime;
        }

        public void settStartTime(String tStartTime) {
            DateTime tmp = stringToDateTime(tStartTime);
            if(tmp != null) {
                startTime = tmp;
                recalculate();
            }
        }

        // endregion

        public DateTime startTime;
        public DateTime endTime;

        public TimeEntry(DateTime startTime, DateTime endTime) {
            this.endTime = endTime;
            this.startTime = startTime;
            recalculate();
        }

        public TimeEntry(long startTimeLong, long endTimeLong) {
            startTime = new DateTime(startTimeLong);
            endTime = new DateTime(endTimeLong);
            recalculate();
        }

        /**
         * Recalculate values.
         */
        private void recalculate(){
            DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss MM.dd.yy");
            tStartTime = new SimpleStringProperty(fmt.print(startTime));
            tEndTime = new SimpleStringProperty(fmt.print(endTime));
            tDuration = new SimpleStringProperty(getDuration());
        }

        /**
         * Convert string in custom format for entry to dateTime.
         * @param time - String "HH:mm:ss MM.dd.yy"
         * @return DateTime if can convert else NULL
         */
        public DateTime stringToDateTime(String time){
            try {
                return DateTime.parse(time, DateTimeFormat.forPattern("HH:mm:ss dd.MM.yy"));
            } catch (Exception e){

            }
            return null;
        }

        /**
         * Return formatted duration time.
         * @return String
         */
        public String getDuration(){
            return WorkspaceManager.getDuration(getDurationMillis());
        }

        /**
         * Get duration milliseconds.
         * @return long
         */
        public long getDurationMillis(){
            return endTime.getMillis() - startTime.getMillis();
        }

        @Override
        public String toString(){
            return startTime.toString() + " - " + endTime.toString();
        }

        public String getKey() {
            return startTime.getMillis() + "-" + endTime.getMillis();
        }
    }

    /**
     * Times log
     */
    public TreeMap<String, TimeEntry> entries;

    /**
     * @return - returns name of workspace
     */
    public String getName() {
        return name;
    }

    public WorkspaceManager(){}

    /**
     * Init logging
     */
    public WorkspaceManager(String workspace) {
        this(workspace, false);
    }

    /**
     * Init logging
     */
    public WorkspaceManager(String workspace, boolean readFile) {
        name = workspace;
        if (readFile)
            read();
        else
            clean();
    }

    public static String getDuration(long millis){
        return String.format("%dh %dmin",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
        );
    }

    /**
     * Clean entries.
     */
    public void clean(){
        entries = new TreeMap<>();
    }

    /**
     * Save entries.
     * @return true if OK else false
     */
    public boolean save() {
        try {
            FileOutputStream output = new FileOutputStream(Config.WORKSPACE_LOCATION + name);
            for(Map.Entry<String, TimeEntry> e: entries.entrySet()){
                TimeEntry v = e.getValue();
                output.write(ByteBuffer.allocate(8).putLong(v.startTime.getMillis()).array());  // start time to bin
                output.write(ByteBuffer.allocate(8).putLong(v.endTime.getMillis()).array());    // end   time to bin
            }
            output.flush();
            output.close();
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, e.toString(), e);
        }

        return false;
    }

    /**
     * Read data from @name .bin file
     * @return true if Ok else false
     */
    public boolean read() {
        clean();

        try {
            RandomAccessFile aFile = new RandomAccessFile(Config.WORKSPACE_LOCATION + name, "r");
            FileChannel inChannel = aFile.getChannel();

            //create buffer with capacity of 16 bytes  8 bytes for start time & 8 for end
            ByteBuffer buf = ByteBuffer.allocate(16);

            int bytesRead = inChannel.read(buf); //read into buffer: long start time - long end time
            while (bytesRead != -1) {

                buf.flip();  //make buffer ready for read

                if(buf.remaining() == 16) {
                    long startTime = buf.getLong();
                    long endTime = buf.getLong();
                    entries.put(startTime + "-" + endTime, new TimeEntry(startTime, endTime));
                    //System.out.println(new TimeEntry(startTime, endTime).toString());
                }

                buf.clear(); //make buffer ready for writing
                bytesRead = inChannel.read(buf);
            }
            aFile.close();
            return true;
        } catch (FileNotFoundException e){
            log.log(Level.INFO, "read > FileNotFoundException - For workspace: " + name);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.toString(), e);
        }

        return false;
    }
}
