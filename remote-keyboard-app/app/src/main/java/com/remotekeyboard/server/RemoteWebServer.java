package com.remotekeyboard.server;

import android.content.Context;
import android.util.Log;
import com.remotekeyboard.RemoteInputMethodService;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteWebServer extends NanoHTTPD {

    private static final String TAG = "RemoteWebServer";
    private final Context context;
    private final AtomicLong totalPacketsReceived = new AtomicLong(0);
    private final AtomicLong lastPacketTime = new AtomicLong(0);

    public RemoteWebServer(Context context, int port) {
        super(port);
        this.context = context.getApplicationContext();
    }

    public long getTotalPacketsReceived() {
        return totalPacketsReceived.get();
    }

    public long getLastPacketTime() {
        return lastPacketTime.get();
    }

    @Override
    public void onWebSocketOpen(WebSocketSession session) {
        Log.d(TAG, "🟢 WebSocket Conectado desde PC. Total clientes activos: " + getActiveWebSocketCount());
    }

    @Override
    public void onWebSocketClose(WebSocketSession session) {
        Log.d(TAG, "🔴 WebSocket Desconectado. Clientes restantes: " + getActiveWebSocketCount());
    }

    @Override
    public void onWebSocketMessage(WebSocketSession session, String message) {
        totalPacketsReceived.incrementAndGet();
        lastPacketTime.set(System.currentTimeMillis());

        if (message == null || message.trim().isEmpty()) return;

        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "");

            // ⚡ 1. Escritura instantánea de texto
            if ("type".equals(type)) {
                String text = json.optString("text", "");
                if (!text.isEmpty()) {
                    RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                    if (ime != null) {
                        ime.typeText(text);
                    }
                }
            } 
            // ⚡ 2. Pulsación instantánea de tecla especial
            else if ("key".equals(type)) {
                String key = json.optString("key", "");
                if (!key.isEmpty()) {
                    RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                    if (ime != null) {
                        ime.sendSpecialKey(key);
                    }
                }
            }
            // ⚡ 3. Ping para cálculo de RTT y latencia en milisegundos en tiempo real
            else if ("ping".equals(type)) {
                long pingId = json.optLong("id", 0);
                long clientTs = json.optLong("t", 0);

                JSONObject pong = new JSONObject();
                pong.put("type", "pong");
                pong.put("id", pingId);
                pong.put("t", clientTs);
                pong.put("server_ts", System.currentTimeMillis());
                pong.put("active_ws", getActiveWebSocketCount());
                pong.put("total_packets", totalPacketsReceived.get());
                pong.put("ime_active", RemoteInputMethodService.getInstance() != null);

                session.sendText(pong.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando mensaje WebSocket: " + message, e);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (Method.OPTIONS.equals(method)) {
            return Response.newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
        }

        // 1. Endpoint Ping
        if ("/api/ping".equals(uri)) {
            totalPacketsReceived.incrementAndGet();
            lastPacketTime.set(System.currentTimeMillis());
            return Response.newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"ok\",\"service\":\"Android Remote Keyboard\",\"version\":\"1.0.4\",\"ws_clients\":" + getActiveWebSocketCount() + "}");
        }

        // 2. Endpoint Diagnóstico Completo
        if ("/api/diag".equals(uri)) {
            try {
                JSONObject diag = new JSONObject();
                diag.put("status", "ok");
                diag.put("active_websockets", getActiveWebSocketCount());
                diag.put("total_packets_received", totalPacketsReceived.get());
                diag.put("last_packet_time", lastPacketTime.get());
                diag.put("ime_service_running", RemoteInputMethodService.getInstance() != null);
                diag.put("server_time", System.currentTimeMillis());
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", diag.toString());
            } catch (Exception e) {
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // 3. Endpoint Type Text (Fallback HTTP REST)
        if ("/api/type".equals(uri) && Method.POST.equals(method)) {
            totalPacketsReceived.incrementAndGet();
            lastPacketTime.set(System.currentTimeMillis());
            try {
                String body = session.getBody();
                if (body != null && !body.trim().isEmpty()) {
                    JSONObject obj = new JSONObject(body);
                    String text = obj.optString("text", "");
                    if (!text.isEmpty()) {
                        RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                        if (ime != null) {
                            ime.typeText(text);
                        }
                    }
                }
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                Log.e(TAG, "Error procesando /api/type: ", e);
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // 4. Endpoint Key Event (Fallback HTTP REST)
        if ("/api/key".equals(uri) && Method.POST.equals(method)) {
            totalPacketsReceived.incrementAndGet();
            lastPacketTime.set(System.currentTimeMillis());
            try {
                String body = session.getBody();
                if (body != null && !body.trim().isEmpty()) {
                    JSONObject obj = new JSONObject(body);
                    String key = obj.optString("key", "");
                    if (!key.isEmpty()) {
                        RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                        if (ime != null) {
                            ime.sendSpecialKey(key);
                        }
                    }
                }
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                Log.e(TAG, "Error procesando /api/key: ", e);
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // 5. Servir el panel web interactivo para el navegador del PC
        try {
            InputStream is = context.getAssets().open("web/index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return Response.newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", sb.toString());
        } catch (Exception e) {
            return Response.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Asset not found: " + e.getMessage());
        }
    }
}
