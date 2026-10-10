import re

file_path = 'app/src/main/java/com/remotekeyboard/server/RemoteWebServer.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

logcat_code = '''
    private java.util.Timer logcatTimer;
    private final java.util.List<WebSocketSession> activeSessions = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void startLogcatStreamer() {
        if (logcatTimer != null) return;
        logcatTimer = new java.util.Timer();
        logcatTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (activeSessions.isEmpty()) return;
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d -v time -t 500");
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    StringBuilder log = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.append(line).append("\\n");
                    }
                    if (log.length() > 0) {
                        JSONObject json = new JSONObject();
                        json.put("type", "logcat");
                        json.put("logs", log.toString());
                        String jsonLog = json.toString();
                        for (WebSocketSession session : activeSessions) {
                            if (session.isOpen()) {
                                session.sendText(jsonLog);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading logcat", e);
                }
            }
        }, 5000, 50000); // 50 seconds interval
    }

    public void stopLogcatStreamer() {
        if (logcatTimer != null) {
            logcatTimer.cancel();
            logcatTimer = null;
        }
    }
'''

if 'startLogcatStreamer' not in content:
    content = content.replace('public class RemoteWebServer extends NanoHTTPD {', 'public class RemoteWebServer extends NanoHTTPD {' + logcat_code)
    content = content.replace('public void onWebSocketOpen(WebSocketSession session) {', 'public void onWebSocketOpen(WebSocketSession session) {\n        activeSessions.add(session);')
    content = content.replace('public void onWebSocketClose(WebSocketSession session) {', 'public void onWebSocketClose(WebSocketSession session) {\n        activeSessions.remove(session);')
    content = content.replace('this.context = context.getApplicationContext();', 'this.context = context.getApplicationContext();\n        startLogcatStreamer();')

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('RemoteWebServer updated with Logcat Streamer')
else:
    print('Logcat Streamer already injected')
