import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, String> queryParams;

    public Request(String method, String path, Map<String, String> headers, InputStream body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;

        // Парсинг Query String
        this.queryParams = parseQueryParams(path);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    private Map<String, String> parseQueryParams(String path) {
        Map<String, String> params = new HashMap<>();

        // Разделяем путь и Query String
        int queryStart = path.indexOf('?');
        if (queryStart != -1) {
            String queryString = path.substring(queryStart + 1);
            try {
                // Используем URLEncodedUtils для парсинга Query String
                List<NameValuePair> pairs = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
                for (NameValuePair pair : pairs) {
                    params.put(pair.getName(), pair.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return params;
    }
}