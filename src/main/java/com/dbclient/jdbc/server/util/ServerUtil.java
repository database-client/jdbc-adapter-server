package com.dbclient.jdbc.server.util;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ServerUtil {

//    public static final Gson gson = new Gson(); // Deprecated, Gson will parse int as double

    @SneakyThrows
    public static <T> T read(HttpExchange exchange, Class<T> clazz) {
        String s = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        return JSON.parse(s, clazz);
    }

    public static void writeJSONResponse(HttpExchange exchange, Object responseObj) throws IOException {
        writeResponse(exchange, responseObj, "application/json;charset=utf-8");
    }

    public static void writeResponse(HttpExchange exchange, Object responseObj, String contentType) throws IOException {
        byte[] bytes;
        if (responseObj instanceof byte[]) {
            bytes = (byte[]) responseObj;
        } else if (responseObj instanceof String) {
            bytes = ((String) responseObj).getBytes(StandardCharsets.UTF_8);
        } else {
            String response = JSON.toJSON(responseObj);
            bytes = response.getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public static void notFound(HttpExchange exchange) throws IOException {
        String response = "404 Not Found";
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

}
