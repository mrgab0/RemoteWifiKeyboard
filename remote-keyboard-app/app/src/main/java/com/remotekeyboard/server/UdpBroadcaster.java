package com.remotekeyboard.server;

import android.os.Build;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpBroadcaster {
    private static final String TAG = "UdpBroadcaster";
    private static final int BROADCAST_PORT = 9998;
    private static final int SERVER_PORT = 9999;
    private Thread broadcastThread;
    private volatile boolean isRunning = false;

    public void start() {
        if (isRunning) return;
        isRunning = true;

        broadcastThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);

                String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
                // Escape simple quotes if any
                deviceName = deviceName.replace("\"", "\\\"");

                String payload = String.format("{\"app\":\"remote-keyboard\",\"device\":\"%s\",\"port\":%d}", deviceName, SERVER_PORT);
                byte[] buffer = payload.getBytes();
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

                Log.i(TAG, "Iniciando emision UDP Broadcast: " + payload);

                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, BROADCAST_PORT);
                        socket.send(packet);
                        Thread.sleep(3000); // Shout every 3 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Error enviando broadcast UDP: " + e.getMessage());
                        Thread.sleep(3000); // Wait before retrying
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fatal configurando UdpBroadcaster: ", e);
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                Log.i(TAG, "Emision UDP detenida.");
            }
        });

        broadcastThread.start();
    }

    public void stop() {
        isRunning = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
            broadcastThread = null;
        }
    }
}
