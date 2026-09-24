package com.remotekeyboard.server;

import android.util.Base64;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NanoHTTPD Ultra-Light Embedded HTTP & WebSocket Server (RFC 6455)
 * Optimized for ultra-low latency (<3ms) with TCP_NODELAY and persistent streaming.
 */
public abstract class NanoHTTPD {

    private static final String TAG = "NanoHTTPD";
    private final String hostname;
    private final int myPort;
    private ServerSocket myServerSocket;
    private Thread myThread;
    private final List<WebSocketSession> activeWebSockets = new CopyOnWriteArrayList<WebSocketSession>();

    public NanoHTTPD(int port) {
        this(null, port);
    }

    public NanoHTTPD(String hostname, int port) {
        this.hostname = hostname;
        this.myPort = port;
    }

    public void start() throws IOException {
        myServerSocket = new ServerSocket();
        myServerSocket.setReuseAddress(true);
        myServerSocket.bind(hostname != null ? new InetSocketAddress(hostname, myPort) : new InetSocketAddress(myPort));
        
        myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!myServerSocket.isClosed()) {
                    try {
                        final Socket socket = myServerSocket.accept();
                        // ⚡ Optimización crucial de latencia: Desactivar algoritmo de Nagle (Delayed ACK)
                        socket.setTcpNoDelay(true);
                        socket.setReceiveBufferSize(65536);
                        socket.setSendBufferSize(65536);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BufferedInputStream bis = new BufferedInputStream(socket.getInputStream(), 8192);
                                    OutputStream os = new BufferedOutputStream(socket.getOutputStream(), 8192);
                                    HTTPSession session = new HTTPSession(bis, os, socket);
                                    session.execute();
                                } catch (Exception ignored) {
                                }
                            }
                        }).start();
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        });
        myThread.setDaemon(true);
        myThread.setName("NanoHTTPD Listener");
        myThread.start();
    }

    public void stop() {
        try {
            if (myServerSocket != null && !myServerSocket.isClosed()) {
                myServerSocket.close();
            }
            for (WebSocketSession ws : activeWebSockets) {
                ws.close();
            }
            activeWebSockets.clear();
            if (myThread != null) {
                myThread.interrupt();
            }
        } catch (Exception ignored) {}
    }

    public int getActiveWebSocketCount() {
        return activeWebSockets.size();
    }

    public abstract Response serve(IHTTPSession session);

    // WebSocket Callbacks
    public void onWebSocketOpen(WebSocketSession session) {}
    public void onWebSocketMessage(WebSocketSession session, String message) {}
    public void onWebSocketClose(WebSocketSession session) {}

    public void broadcastWebSocket(String message) {
        for (WebSocketSession ws : activeWebSockets) {
            try {
                ws.sendText(message);
            } catch (Exception ignored) {}
        }
    }

    public interface IHTTPSession {
        String getUri();
        Method getMethod();
        Map<String, String> getHeaders();
        String getBody();
    }

    public enum Method {
        GET, PUT, POST, DELETE, HEAD, OPTIONS
    }

    public static class Response {
        public enum Status {
            OK(200, "OK"),
            SWITCHING_PROTOCOLS(101, "Switching Protocols"),
            NOT_FOUND(404, "Not Found"),
            INTERNAL_ERROR(500, "Internal Server Error");

            private final int requestStatus;
            private final String description;

            Status(int requestStatus, String description) {
                this.requestStatus = requestStatus;
                this.description = description;
            }

            public String getDescription() {
                return "" + this.requestStatus + " " + this.description;
            }
        }

        private final Status status;
        private final String mimeType;
        private final byte[] data;

        public Response(Status status, String mimeType, byte[] data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        public Response(Status status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = (txt != null) ? txt.getBytes(StandardCharsets.UTF_8) : new byte[0];
        }

        public static Response newFixedLengthResponse(Status status, String mimeType, String message) {
            return new Response(status, mimeType, message);
        }

        public static Response newFixedLengthResponse(String message) {
            return new Response(Status.OK, "text/html; charset=UTF-8", message);
        }

        public void send(OutputStream outputStream) {
            try {
                String mime = mimeType == null ? "text/plain; charset=UTF-8" : mimeType;
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                pw.append("HTTP/1.1 ").append(status.getDescription()).append("\r\n");
                pw.append("Content-Type: ").append(mime).append("\r\n");
                pw.append("Content-Length: ").append(String.valueOf(data != null ? data.length : 0)).append("\r\n");
                pw.append("Access-Control-Allow-Origin: *\r\n");
                pw.append("Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n");
                pw.append("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n");
                pw.append("Connection: close\r\n");
                pw.append("\r\n");
                pw.flush();

                if (data != null && data.length > 0) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            } catch (IOException ignored) {}
        }
    }

    public class WebSocketSession {
        private final Socket socket;
        private final BufferedInputStream is;
        private final OutputStream os;
        private boolean closed = false;

        public WebSocketSession(Socket socket, BufferedInputStream is, OutputStream os) {
            this.socket = socket;
            this.is = is;
            this.os = os;
        }

        public synchronized void sendText(String text) throws IOException {
            if (closed || text == null) return;
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            os.write(0x81); // FIN + Opcode 1 (Text)
            if (payload.length <= 125) {
                os.write(payload.length);
            } else if (payload.length <= 65535) {
                os.write(126);
                os.write((payload.length >> 8) & 0xFF);
                os.write(payload.length & 0xFF);
            } else {
                os.write(127);
                for (int i = 7; i >= 0; i--) {
                    os.write((int) ((payload.length >> (8 * i)) & 0xFF));
                }
            }
            os.write(payload);
            os.flush();
        }

        public synchronized void sendPong(byte[] payload) throws IOException {
            if (closed) return;
            os.write(0x8A); // FIN + Opcode 10 (Pong)
            int len = payload != null ? payload.length : 0;
            os.write(len & 0x7F);
            if (payload != null && len > 0) {
                os.write(payload);
            }
            os.flush();
        }

        public void handleFrames() {
            try {
                activeWebSockets.add(this);
                onWebSocketOpen(this);

                while (!closed && !socket.isClosed()) {
                    int b0 = is.read();
                    if (b0 == -1) break;
                    int opcode = b0 & 0x0F;

                    int b1 = is.read();
                    if (b1 == -1) break;
                    boolean masked = (b1 & 0x80) != 0;
                    long len = b1 & 0x7F;

                    if (len == 126) {
                        len = ((is.read() & 0xFF) << 8) | (is.read() & 0xFF);
                    } else if (len == 127) {
                        len = 0;
                        for (int i = 0; i < 8; i++) {
                            len = (len << 8) | (is.read() & 0xFF);
                        }
                    }

                    byte[] mask = new byte[4];
                    if (masked) {
                        for (int i = 0; i < 4; i++) {
                            mask[i] = (byte) is.read();
                        }
                    }

                    byte[] payload = new byte[(int) len];
                    int totalRead = 0;
                    while (totalRead < len) {
                        int r = is.read(payload, totalRead, (int) (len - totalRead));
                        if (r == -1) break;
                        totalRead += r;
                    }

                    if (masked) {
                        for (int i = 0; i < payload.length; i++) {
                            payload[i] = (byte) (payload[i] ^ mask[i % 4]);
                        }
                    }

                    if (opcode == 8) { // CLOSE
                        break;
                    } else if (opcode == 9) { // PING
                        sendPong(payload);
                    } else if (opcode == 1) { // TEXT
                        String text = new String(payload, StandardCharsets.UTF_8);
                        onWebSocketMessage(this, text);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }
        }

        public void close() {
            if (closed) return;
            closed = true;
            activeWebSockets.remove(this);
            try {
                onWebSocketClose(this);
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    protected class HTTPSession implements IHTTPSession {
        private final BufferedInputStream inputStream;
        private final OutputStream outputStream;
        private final Socket socket;
        private String uri = "/";
        private Method method = Method.GET;
        private final Map<String, String> headers = new HashMap<String, String>();
        private String body = "";

        public HTTPSession(BufferedInputStream inputStream, OutputStream outputStream, Socket socket) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.socket = socket;
        }

        public void execute() throws IOException {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int b;
            int stage = 0;

            // Leer cabeceras HTTP de forma optimizada
            while ((b = inputStream.read()) != -1) {
                headerBuffer.write(b);
                if (b == '\r' && (stage == 0 || stage == 2)) {
                    stage++;
                } else if (b == '\n' && (stage == 1 || stage == 3)) {
                    stage++;
                    if (stage == 4) break;
                } else {
                    stage = (b == '\r') ? 1 : 0;
                }
            }

            if (stage != 4) {
                socket.close();
                return;
            }

            String headerText = new String(headerBuffer.toByteArray(), StandardCharsets.UTF_8);
            String[] lines = headerText.split("\r\n");
            if (lines.length == 0) {
                socket.close();
                return;
            }

            String[] requestLine = lines[0].split("\\s+");
            if (requestLine.length < 2) {
                socket.close();
                return;
            }

            try {
                this.method = Method.valueOf(requestLine[0].toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                this.method = Method.GET;
            }
            this.uri = requestLine[1];

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                int p = line.indexOf(':');
                if (p > 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.ROOT), line.substring(p + 1).trim());
                }
            }

            // Manejo de pre-flight CORS OPTIONS inmediato
            if (Method.OPTIONS.equals(this.method)) {
                Response r = Response.newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
                r.send(outputStream);
                socket.close();
                return;
            }

            // Detección y Handshake de WebSocket RFC 6455
            String upgrade = headers.get("upgrade");
            String wsKey = headers.get("sec-websocket-key");
            if ("websocket".equalsIgnoreCase(upgrade) && wsKey != null) {
                handleWebSocketHandshake(wsKey);
                return;
            }

            // Leer exactamente Content-Length bytes del body para POST
            String contentLengthStr = headers.get("content-length");
            if (contentLengthStr != null) {
                try {
                    int length = Integer.parseInt(contentLengthStr.trim());
                    if (length > 0) {
                        byte[] bodyBytes = new byte[length];
                        int totalRead = 0;
                        while (totalRead < length) {
                            int read = inputStream.read(bodyBytes, totalRead, length - totalRead);
                            if (read == -1) break;
                            totalRead += read;
                        }
                        this.body = new String(bodyBytes, 0, totalRead, StandardCharsets.UTF_8);
                    }
                } catch (Exception ignored) {}
            }

            Response r = serve(this);
            if (r != null) {
                r.send(outputStream);
            }
            try {
                socket.close();
            } catch (Exception ignored) {}
        }

        private void handleWebSocketHandshake(String key) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                byte[] hash = md.digest(acceptSeed.getBytes(StandardCharsets.UTF_8));
                String acceptKey = Base64.encodeToString(hash, Base64.NO_WRAP);

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                pw.append("HTTP/1.1 101 Switching Protocols\r\n");
                pw.append("Upgrade: websocket\r\n");
                pw.append("Connection: Upgrade\r\n");
                pw.append("Sec-WebSocket-Accept: ").append(acceptKey).append("\r\n");
                pw.append("\r\n");
                pw.flush();

                WebSocketSession wsSession = new WebSocketSession(socket, inputStream, outputStream);
                wsSession.handleFrames();

            } catch (Exception e) {
                Log.e(TAG, "Error en handshake WebSocket: ", e);
                try {
                    socket.close();
                } catch (Exception ignored) {}
            }
        }

        @Override
        public String getUri() { return uri; }
        @Override
        public Method getMethod() { return method; }
        @Override
        public Map<String, String> getHeaders() { return headers; }
        @Override
        public String getBody() { return body; }
    }
}
