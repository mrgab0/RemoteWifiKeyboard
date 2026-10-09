package com.remotekeyboard;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugLogger {
    private static File logFile = null;
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    public static void init(Context context) {
        if (logFile == null) {
            try {
                File dir = context.getExternalFilesDir(null);
                if (dir != null) {
                    logFile = new File(dir, "keyboard_debug_log.txt");
                    log("--- NUEVA SESIÓN DE DEBUG INICIADA ---");
                }
            } catch (Exception e) {
                Log.e("DebugLogger", "No se pudo crear el archivo de log", e);
            }
        }
    }

    public static void log(final String message) {
        Log.d("RemoteKB_DEBUG", message);
        if (logFile != null) {
            logExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileWriter writer = new FileWriter(logFile, true);
                        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                        writer.append(time).append(" - ").append(message).append("\n");
                        writer.flush();
                        writer.close();
                    } catch (Exception ignored) {}
                }
            });
        }
    }
}
