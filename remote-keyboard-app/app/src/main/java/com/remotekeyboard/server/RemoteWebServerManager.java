package com.remotekeyboard.server;

import android.content.Context;
import android.util.Log;
import com.remotekeyboard.DebugLogger;

public class RemoteWebServerManager {

    private static final String TAG = "WebServerManager";
    public static final int SERVER_PORT = 9999;
    private static RemoteWebServer serverInstance;
    private static UdpBroadcaster broadcasterInstance;

    public static synchronized void ensureServerStarted(Context context) {
        if (serverInstance == null) {
            try {
                DebugLogger.log("Intentando iniciar RemoteWebServer en puerto " + SERVER_PORT);
                serverInstance = new RemoteWebServer(context, SERVER_PORT);
                serverInstance.start();
                
                broadcasterInstance = new UdpBroadcaster();
                broadcasterInstance.start();

                Log.i(TAG, "Servidor web HTTP & WebSocket iniciado exitosamente en puerto " + SERVER_PORT);
                DebugLogger.log("RemoteWebServer INICIADO exitosamente en puerto " + SERVER_PORT);
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar el servidor web: ", e);
                DebugLogger.log("ERROR CRÍTICO al iniciar RemoteWebServer: " + e.getMessage());
            }
        } else {
             DebugLogger.log("ensureServerStarted llamado, pero el servidor ya estaba corriendo.");
        }
    }

    public static synchronized RemoteWebServer getServerInstance() {
        return serverInstance;
    }

    public static synchronized void stopServer() {
        if (broadcasterInstance != null) {
            broadcasterInstance.stop();
            broadcasterInstance = null;
        }
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
