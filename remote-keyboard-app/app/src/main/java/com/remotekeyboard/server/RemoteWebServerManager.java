package com.remotekeyboard.server;

import android.content.Context;
import android.util.Log;

public class RemoteWebServerManager {

    private static final String TAG = "WebServerManager";
    public static final int SERVER_PORT = 8080;
    private static RemoteWebServer serverInstance;

    public static synchronized void ensureServerStarted(Context context) {
        if (serverInstance == null) {
            try {
                serverInstance = new RemoteWebServer(context, SERVER_PORT);
                serverInstance.start();
                Log.i(TAG, "Servidor web HTTP iniciado exitosamente en puerto " + SERVER_PORT);
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar el servidor web: ", e);
            }
        }
    }

    public static synchronized void stopServer() {
        if (serverInstance != null) {
            try {
                serverInstance.stop();
                Log.i(TAG, "Servidor web HTTP detenido.");
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo servidor: ", e);
            }
            serverInstance = null;
        }
    }
}
