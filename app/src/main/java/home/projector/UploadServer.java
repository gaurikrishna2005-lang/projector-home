package home.projector;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * A tiny web page for sending photos from a phone on the same Wi-Fi.
 * It only runs while the Wallpaper screen is open.
 */
final class UploadServer {
    interface Handler {
        /** Called on the server thread with the uploaded bytes. Return false if it isn't a picture. */
        boolean onUpload(File raw);
    }

    private static final long MAX_BYTES = 40L * 1024 * 1024;

    private final byte[] page;
    private final File temp;
    private final Handler handler;
    private volatile ServerSocket socket;

    UploadServer(byte[] page, File temp, Handler handler) {
        this.page = page;
        this.temp = temp;
        this.handler = handler;
    }

    /** Starts listening and returns the port (8080 when it's free). */
    int start() throws IOException {
        stop();
        ServerSocket s = new ServerSocket();
        s.setReuseAddress(true);
        try {
            s.bind(new InetSocketAddress(8080));
        } catch (IOException e) {
            s.close();
            s = new ServerSocket(0);
        }
        socket = s;
        final ServerSocket ss = s;
        Thread t = new Thread(() -> loop(ss), "upload-server");
        t.setDaemon(true);
        t.start();
        return s.getLocalPort();
    }

    void stop() {
        ServerSocket s = socket;
        socket = null;
        if (s != null) {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private void loop(ServerSocket ss) {
        while (!ss.isClosed()) {
            Socket c;
            try {
                c = ss.accept();
            } catch (IOException e) {
                return;
            }
            try {
                handle(c);
            } catch (Exception ignored) {
                // A dropped connection shouldn't stop the page.
            } finally {
                try { c.close(); } catch (IOException ignored) { }
            }
        }
    }

    private void handle(Socket c) throws IOException {
        c.setSoTimeout(60000);
        InputStream in = new BufferedInputStream(c.getInputStream());
        OutputStream out = c.getOutputStream();

        String request = readLine(in);
        if (request == null) return;
        long length = -1;
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("content-length")) {
                try {
                    length = Long.parseLong(line.substring(colon + 1).trim());
                } catch (NumberFormatException ignored) { }
            }
        }

        String[] parts = request.split(" ");
        String method = parts.length > 0 ? parts[0] : "";
        String path = parts.length > 1 ? parts[1] : "/";

        if (method.equals("GET") && (path.equals("/") || path.startsWith("/?"))) {
            send(out, "200 OK", "text/html; charset=utf-8", page);
            return;
        }

        if (method.equals("POST") && path.equals("/upload")) {
            if (length <= 0 || length > MAX_BYTES) {
                send(out, "413 Payload Too Large", "text/plain", bytes("Too big"));
                return;
            }
            OutputStream f = new FileOutputStream(temp);
            try {
                byte[] buf = new byte[64 * 1024];
                long left = length;
                while (left > 0) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) throw new EOFException();
                    f.write(buf, 0, n);
                    left -= n;
                }
            } finally {
                f.close();
            }
            boolean ok = handler.onUpload(temp);
            send(out, ok ? "200 OK" : "415 Unsupported Media Type", "text/plain",
                    bytes(ok ? "ok" : "Not a picture"));
            return;
        }

        send(out, "404 Not Found", "text/plain", bytes("Not found"));
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') sb.append((char) b);
            if (sb.length() > 8192) throw new IOException("Line too long");
        }
        if (b == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static void send(OutputStream out, String status, String type, byte[] body) throws IOException {
        String head = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        out.write(bytes(head));
        out.write(body);
        out.flush();
    }

    /**
     * The projector's address on the home network, ready to put in a URL:
     * 192.168.1.23 when there is one, otherwise a bracketed IPv6 address
     * (a local fd.. address first, as it stays the same).
     */
    static String localIp() {
        String local6 = null, global6 = null;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        if (a.isSiteLocalAddress()) return a.getHostAddress();
                        continue;
                    }
                    if (a.isLinkLocalAddress() || a.isLoopbackAddress()) continue;
                    String s = a.getHostAddress();
                    int pct = s.indexOf('%');
                    if (pct >= 0) s = s.substring(0, pct);
                    byte first = a.getAddress()[0];
                    if ((first & 0xFE) == 0xFC) {
                        if (local6 == null) local6 = s;   // fc00::/7, e.g. fd06:...
                    } else if (global6 == null) {
                        global6 = s;
                    }
                }
            }
        } catch (Exception ignored) { }
        if (local6 != null) return "[" + local6 + "]";
        if (global6 != null) return "[" + global6 + "]";
        return null;
    }
}
