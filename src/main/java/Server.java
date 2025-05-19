import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private final int port;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;
    private volatile boolean isRunning = true;

    public Server(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
        this.executorService = Executors.newFixedThreadPool(64);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }

    public void start() {
        System.out.println("Сервер запущен на порту " + port);
        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                if (isRunning) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        System.out.println("Сервер остановлен");
    }

    private void handleConnection(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String[] parts = requestLine.split("\\s");
            if (parts.length != 3) {
                return;
            }

            String method = parts[0];
            String fullPath = parts[1];

            int queryStart = fullPath.indexOf('?');
            String path = (queryStart != -1) ? fullPath.substring(0, queryStart) : fullPath;

            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            InputStream body = socket.getInputStream();
            Request request = new Request(method, fullPath, headers, body);

            Handler handler = handlers.getOrDefault(method, Collections.emptyMap()).get(path);
            if (handler != null) {
                try {
                    handler.handle(request, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                sendResponse(out, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
            }

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(BufferedOutputStream out, String response) throws IOException {
        out.write(response.getBytes());
        out.flush();
    }
}