package com.dbclient.jdbc.server.util;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ServerUtil {

    public static final Gson gson = new Gson();

    @SneakyThrows
    public static <T> T read(HttpExchange exchange, Class<T> clazz) {
//        String s = IOUtils.toString(exchange.getRequestBody());
        String s = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                .lines().collect(Collectors.joining("\n"));
//        return JSON.parse(s, clazz);
//        return JSON.parseObject(s,clazz);
        return gson.fromJson(s, clazz);
    }

    public static void writeResponse(HttpExchange exchange, Object responseObj) throws IOException {
//        String response = responseObj instanceof String ? responseObj.toString() : JSON.toJSONString(responseObj);
        String response = responseObj instanceof String ? responseObj.toString() : gson.toJson(responseObj);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
