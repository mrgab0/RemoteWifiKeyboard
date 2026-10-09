package com.remotekeyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.remotekeyboard.server.RemoteWebServer;
import com.remotekeyboard.server.RemoteWebServerManager;

import org.json.JSONObject;

public class ShareToPcActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            RemoteWebServer server = RemoteWebServerManager.getServerInstance();
            if (server != null) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "clipboard_sync");

                    if ("text/plain".equals(type)) {
                        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                        if (sharedText != null) {
                            json.put("contentType", "text");
                            json.put("content", sharedText);
                            server.broadcastWebSocket(json.toString());
                            Toast.makeText(this, "Enviado a la PC: " + sharedText, Toast.LENGTH_SHORT).show();
                        }
                    } else if (type.startsWith("image/")) {
                        // For images, we would read the URI, compress to Base64, and send.
                        // Implemented async to avoid blocking UI.
                        android.net.Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (imageUri != null) {
                            new Thread(() -> {
                                try {
                                    java.io.InputStream is = getContentResolver().openInputStream(imageUri);
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                                    if (bitmap != null) {
                                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
                                        byte[] bytes = baos.toByteArray();
                                        String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                                        String dataUrl = "data:image/png;base64," + base64;
                                        
                                        json.put("contentType", "image");
                                        json.put("content", dataUrl);
                                        server.broadcastWebSocket(json.toString());
                                        
                                        runOnUiThread(() -> Toast.makeText(ShareToPcActivity.this, "Imagen enviada a la PC", Toast.LENGTH_SHORT).show());
                                    }
                                } catch (Exception e) {}
                            }).start();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error enviando a la PC", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "PC no conectada. Abre la app Remote Keyboard", Toast.LENGTH_LONG).show();
            }
        }
        
        finish(); // Cierra la actividad transparente de inmediato
    }
}
