package com.remotekeyboard.server;

import android.content.Context;
import com.remotekeyboard.RemoteInputMethodService;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public class RemoteWebServer extends NanoHTTPD {

    private final Context context;
    private final RemoteInputMethodService imeService;

    public RemoteWebServer(Context context, RemoteInputMethodService imeService, int port) {
        super(port);
        this.context = context;
        this.imeService = imeService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (Method.OPTIONS.equals(method)) {
            return Response.newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
        }

        // Endpoint Ping
        if ("/api/ping".equals(uri)) {
            return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
        }

        // Endpoint Type Text
        if ("/api/type".equals(uri) && Method.POST.equals(method)) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String postData = files.get("postData");
                if (postData != null) {
                    // Extract text value from JSON {"text":"..."}
                    String text = extractJsonValue(postData, "text");
                    if (text != null && imeService != null) {
                        imeService.typeText(text);
                    }
                }
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // Endpoint Key Event
        if ("/api/key".equals(uri) && Method.POST.equals(method)) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String postData = files.get("postData");
                if (postData != null) {
                    String key = extractJsonValue(postData, "key");
                    if (key != null && imeService != null) {
                        imeService.sendSpecialKey(key);
                    }
                }
                return Response.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                return Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
        }

        // Serve HTML Assets
        try {
            InputStream is = context.getAssets().open("web/index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
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

    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return null;
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            String val = json.substring(start, end);
            return val.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception e) {
            return null;
        }
    }
}
