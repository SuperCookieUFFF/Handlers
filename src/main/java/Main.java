import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Server server = new Server(9999);

            // Регистрируем хендлер для GET-запросов к пути /messages
            server.addHandler("GET", "/messages", (request, responseStream) -> {
                try {
                    // Получаем параметр "last" из Query String
                    String last = request.getQueryParam("last");

                    // Проверяем, передан ли параметр
                    if (last != null) {
                        // Обработка параметра "last"
                        responseStream.write(("Received last: " + last).getBytes());
                    } else {
                        // Если параметра нет, отправляем сообщение
                        responseStream.write("No last parameter".getBytes());
                    }

                    // Отправляем ответ клиенту
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Запускаем сервер
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}