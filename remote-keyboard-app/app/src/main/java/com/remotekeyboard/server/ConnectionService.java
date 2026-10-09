package com.remotekeyboard.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.remotekeyboard.MainActivity;

public class ConnectionService extends Service {
    private static final String CHANNEL_ID = "RemoteKeyboardConnectionChannel";
    private static final int NOTIFICATION_ID = 9999;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        RemoteWebServerManager.ensureServerStarted(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RemoteWebServerManager.ensureServerStarted(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RemoteWebServerManager.stopServer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Conexión con PC",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Mantiene la conexión WebSocket con la PC");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Remote WiFi Keyboard")
                .setContentText("Servidor conectado. Portapapeles activo.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }
}
