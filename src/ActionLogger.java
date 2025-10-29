import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class ActionLogger {
    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private volatile boolean running = true;

    public ActionLogger(File file) {
        this.logFile = file;

        // Start the logging thread
        Thread loggerThread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    String line = logQueue.poll(1, TimeUnit.SECONDS);
                    if (line != null) writeLine(line);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    private void writeLine(String line) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String filename, String action) {
        String timestamp = dateFormat.format(new Date());
        String entry = String.format("%s [%s] %s", timestamp, filename, action);
        logQueue.offer(entry);
    }

    public void stop() {
        running = false;
    }
}
