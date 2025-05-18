import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Server {
    private int port;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;
    private volatile boolean isRunning = true;

    public Server(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать серверный сокет", e);
        }
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
            // Чтение строки запроса
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            // Разделение строки запроса на части
            String[] parts = requestLine.split("\\s");
            if (parts.length != 3) {
                return;
            }

            // Извлечение метода и пути
            String method = parts[0];
            String path = parts[1];

            // Чтение заголовков
            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            // Получение тела запроса
            InputStream body = socket.getInputStream();

            // Создание объекта Request
            Request request = new Request(method, path, headers, body);

            // Поиск обработчика
            Handler handler = handlers.getOrDefault(method, Collections.emptyMap()).get(path);

            // Вызов обработчика, если он найден
            if (handler != null) {
                try {
                    handler.handle(request, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Отправка ответа 404 Not Found, если обработчик не найден
                sendResponse(out, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
            }

            // Закрытие потока вывода
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(BufferedOutputStream out, String response) throws IOException {
        out.write(response.getBytes());
        out.flush();
    }

    public void listen(int port) {
        this.port = port;
        start();
    }
}