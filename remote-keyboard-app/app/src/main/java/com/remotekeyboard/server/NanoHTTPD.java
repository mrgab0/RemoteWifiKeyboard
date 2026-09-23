package com.remotekeyboard.server;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * NanoHTTPD v2.3.1 (Embedded Light HTTP Server)
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
                do {
                    try {
                        final Socket finalAccept = myServerSocket.accept();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream inputStream = finalAccept.getInputStream();
                                    OutputStream outputStream = finalAccept.getOutputStream();
                                    HTTPSession session = new HTTPSession(inputStream, outputStream);
                                    session.execute();
                                } catch (Exception e) {
                                    // Ignore closed stream
                                }
                            }
                        }).start();
                    } catch (IOException e) {
                        break;
                    }
                } while (!myServerSocket.isClosed());
            }
        });
        myThread.setDaemon(true);
        myThread.setName("NanoHTTPD Main Listener");
        myThread.start();
    }

    public void stop() {
        try {
            if (myServerSocket != null) {
                myServerSocket.close();
            }
            if (myThread != null) {
                myThread.join();
            }
        } catch (Exception e) {
            // Ignored
        }
    }

    public abstract Response serve(IHTTPSession session);

    public interface IHTTPSession {
        String getUri();
        Method getMethod();
        Map<String, String> getHeaders();
        InputStream getInputStream();
        Map<String, String> getParms();
        void parseBody(Map<String, String> files) throws IOException;
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

        private Status status;
        private String mimeType;
        private InputStream data;

        public Response(Status status, String mimeType, InputStream data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        public Response(Status status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            try {
                this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
            } catch (UnsupportedEncodingException uee) {
                this.data = null;
            }
        }

        public static Response newFixedLengthResponse(Status status, String mimeType, String message) {
            return new Response(status, mimeType, message);
        }

        public static Response newFixedLengthResponse(String message) {
            return newFixedLengthResponse(Status.OK, "text/html; charset=UTF-8", message);
        }

        protected void send(OutputStream outputStream) {
            try {
                String mime = mimeType == null ? "text/plain" : mimeType;
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")), false);
                pw.append("HTTP/1.1 ").append(status.getDescription()).append(" \r\n");
                pw.append("Content-Type: ").append(mime).append("\r\n");
                pw.append("Connection: close\r\n");
                pw.append("Access-Control-Allow-Origin: *\r\n");
                pw.append("Access-Control-Allow-Headers: Content-Type\r\n");
                pw.append("\r\n");
                pw.flush();

                if (data != null) {
                    byte[] buff = new byte[8192];
                    int read;
                    while ((read = data.read(buff)) > 0) {
                        outputStream.write(buff, 0, read);
                    }
                    outputStream.flush();
                }
            } catch (IOException ioe) {
                // Closed
            }
        }
    }

    protected class HTTPSession implements IHTTPSession {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private String uri;
        private Method method;
        private Map<String, String> headers = new HashMap<String, String>();
        private Map<String, String> parms = new HashMap<String, String>();

        public HTTPSession(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public void execute() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = reader.readLine();
            if (line == null) return;

            StringTokenizer st = new StringTokenizer(line);
            if (!st.hasMoreTokens()) return;

            String methodStr = st.nextToken();
            try {
                this.method = Method.valueOf(methodStr);
            } catch (Exception e) {
                this.method = Method.GET;
            }

            if (!st.hasMoreTokens()) return;
            this.uri = st.nextToken();

            while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                }
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
        public InputStream getInputStream() { return inputStream; }
        @Override
        public Map<String, String> getParms() { return parms; }
        @Override
        public void parseBody(Map<String, String> files) throws IOException {
            String contentLengthStr = headers.get("content-length");
            if (contentLengthStr != null) {
                try {
                    int length = Integer.parseInt(contentLengthStr);
                    char[] buf = new char[length];
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    int total = 0;
                    while (total < length) {
                        int read = reader.read(buf, total, length - total);
                        if (read == -1) break;
                        total += read;
                    }
                    files.put("postData", new String(buf, 0, total));
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}
