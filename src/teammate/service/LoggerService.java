package teammate.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Provides a simple logging system that writes info and error messages to timestamped log files.
public class LoggerService {

    private static final String LOG_DIR = "src/teammate/Log/";

    // info log file: one per hour
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");

    // timestamp inside log
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // single error log file
    private static final String ERROR_FILE = "error.log";

    // -------- Singleton --------
    private static final LoggerService INSTANCE = new LoggerService();

    public LoggerService() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static LoggerService getInstance() {
        return INSTANCE;
    }

    // -------- Public APIs --------
    public void info(String message) {
        writeInfo(message);
    }
    // [UPD 1.2.4.5] log invalid index for update
    // [UPD 3.2.3.1.1.2] info "Participant permanently updated"
    // [UPD 3.2.3.2.2] error "Failed to save updated participant"
    // [UPD 3.2.4.1] info "Update kept in memory only"

    public void error(String message) {
        writeError(message, null);
    }

    public void error(String message, Throwable t) {
        writeError(message, t);
    }

    // -------- Internal writers --------
    private synchronized void writeInfo(String message) {
        LocalDateTime now = LocalDateTime.now();
        String fileName = "log_" + now.format(FILE_FORMAT) + ".log";
        File file = new File(LOG_DIR + fileName);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            pw.println("[" + now.format(TIME_FORMAT) + "] " + message);
        } catch (IOException e) {
            // last fallback – don't crash app
            e.printStackTrace();
        }
    }

    private synchronized void writeError(String message, Throwable t) {
        LocalDateTime now = LocalDateTime.now();
        File file = new File(LOG_DIR + ERROR_FILE);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            pw.println("[" + now.format(TIME_FORMAT) + "] ERROR: " + message);
            if (t != null) {
                t.printStackTrace(pw);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}