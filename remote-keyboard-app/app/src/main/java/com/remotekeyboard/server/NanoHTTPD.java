package com.remotekeyboard.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * NanoHTTPD Ultra-Light & Robust Embedded HTTP Server
 */
public abstract class NanoHTTPD {

    private final String hostname;
    private final int myPort;
    private ServerSocket myServerSocket;
    private Thread myThread;

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
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream is = socket.getInputStream();
                                    OutputStream os = socket.getOutputStream();
                                    HTTPSession session = new HTTPSession(is, os);
                                    session.execute();
                                } catch (Exception ignored) {
                                } finally {
                                    try {
                                        socket.close();
                                    } catch (Exception ignored) {}
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
            if (myThread != null) {
                myThread.interrupt();
            }
        } catch (Exception ignored) {}
    }

    public abstract Response serve(IHTTPSession session);

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
            return newFixedLengthResponse(Status.OK, "text/html; charset=UTF-8", message);
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

    protected class HTTPSession implements IHTTPSession {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private String uri = "/";
        private Method method = Method.GET;
        private final Map<String, String> headers = new HashMap<String, String>();
        private String body = "";

        public HTTPSession(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public void execute() throws IOException {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int b;
            int stage = 0;

            // Leer byte por byte hasta encontrar exactamente \r\n\r\n
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

            if (stage != 4) return;

            String headerText = new String(headerBuffer.toByteArray(), StandardCharsets.UTF_8);
            String[] lines = headerText.split("\r\n");
            if (lines.length == 0) return;

            String[] requestLine = lines[0].split("\\s+");
            if (requestLine.length < 2) return;

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
                return;
            }

            // Leer exactamente Content-Length bytes del body
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
