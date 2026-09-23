package com.remotekeyboard.server;

import android.content.Context;
import android.util.Log;
import com.remotekeyboard.RemoteInputMethodService;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

public class RemoteWebServer extends NanoHTTPD {

    private static final String TAG = "RemoteWebServer";
    private final Context context;

    public RemoteWebServer(Context context, int port) {
        super(port);
        this.context = context.getApplicationContext();
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
            return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\",\"service\":\"Android Remote Keyboard\",\"version\":\"1.0.2\"}");
        }

        // 2. Endpoint Type Text (Escritura desde PC a Android)
        if ("/api/type".equals(uri) && Method.POST.equals(method)) {
            try {
                String body = session.getBody();
                Log.d(TAG, "POST /api/type body: " + body);
                if (body != null && !body.trim().isEmpty()) {
                    JSONObject obj = new JSONObject(body);
                    String text = obj.optString("text", "");
                    if (!text.isEmpty()) {
                        RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                        if (ime != null) {
                            ime.typeText(text);
                        } else {
                            Log.w(TAG, "IME service no activo al recibir /api/type");
                        }
                    }
                }
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                Log.e(TAG, "Error procesando /api/type: ", e);
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // 3. Endpoint Key Event (Teclas especiales desde PC a Android)
        if ("/api/key".equals(uri) && Method.POST.equals(method)) {
            try {
                String body = session.getBody();
                Log.d(TAG, "POST /api/key body: " + body);
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

        // 4. Servir el panel web interactivo para el navegador del PC
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
