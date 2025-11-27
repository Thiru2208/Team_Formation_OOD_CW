package teammate.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {

    private static final String LOG_DIR = "src/teammate/Log/";

    // log file changes every hour
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Ensure folder exists
    public LoggerService() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Log a message
    public void log(String message) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // File name based on hour
            String fileName = "log_" + now.format(FILE_FORMAT) + ".log";
            File file = new File(LOG_DIR + fileName);

            try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
                String timestamp = now.format(TIME_FORMAT);
                pw.println("[" + timestamp + "] " + message);
            }

        } catch (IOException e) {
            System.out.println("Logging failed: " + e.getMessage());
        }
    }
}
