import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        final var server = new Server(9999);

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            // Логика обработки GET-запроса
            try {
                responseStream.write("HTTP/1.1 200 OK\r\nContent-Length: 12\r\nConnection: close\r\n\r\nGET messages".getBytes());
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            // Логика обработки POST-запроса
            try {
                responseStream.write("HTTP/1.1 200 OK\r\nContent-Length: 13\r\nConnection: close\r\n\r\nPOST messages".getBytes());
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.listen(9999);
    }
}