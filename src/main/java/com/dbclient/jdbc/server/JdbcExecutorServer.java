package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.driver.LibraryUtils;
import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.execute.ExecuteDTO;
import com.dbclient.jdbc.server.response.AliveCheckResponse;
import com.dbclient.jdbc.server.response.ConnectResponse;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import com.dbclient.jdbc.server.translator.DB2ErrorTranslator;
import com.dbclient.jdbc.server.util.JSON;
import com.dbclient.jdbc.server.util.ServerUtil;
import com.dbclient.jdbc.server.util.StringUtils;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Slf4j
public class JdbcExecutorServer {

    public static final Map<String, JdbcExecutor> executorMap = new HashMap<>();

    private static final DB2ErrorTranslator translator = new DB2ErrorTranslator();

    @SneakyThrows
    public static void main(String[] args) {

        long start = new Date().getTime();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7823), 0);
        server.createContext("/test", exchange -> {
            ServerUtil.writeJSONResponse(exchange, "hello");
        });
        server.createContext("/connect", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            log.info("connect, params: {}", JSON.toJSON(connectDTO));
            String errorMessage = null;
            String fullErrorMessage = null;
            try {
                String id = connectDTO.getId();
                log.info("Create connection for " + id);
                JdbcExecutor jdbcExecutor = executorMap.get(id);
                if (jdbcExecutor == null) {
                    jdbcExecutor = new JdbcExecutor(connectDTO);
                    jdbcExecutor.testAlive();
                    executorMap.put(id, jdbcExecutor);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String errMsg = e.getMessage();
                if (StringUtils.isNotEmpty(errMsg)) {
                    fullErrorMessage = e.getClass().getName() + ": " + e.getMessage();
                    errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                } else {
                    errorMessage = e.getClass().getName();
                    fullErrorMessage = errorMessage;
                }
                e.printStackTrace();
            } finally {
                ServerUtil.writeJSONResponse(exchange, ConnectResponse.builder()
                        .success(errorMessage == null)
                        .err(errorMessage)
                        .fullErr(fullErrorMessage)
                        .build());
            }
        });
        server.createContext("/execute", exchange -> {
            ExecuteDTO executeDTO = ServerUtil.read(exchange, ExecuteDTO.class);
            String id = executeDTO.getId();
            log.info("Execute SQL for {}, SQL: {}", id, executeDTO.getSql());
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            ExecuteResponse executeResponse = null;
            if (jdbcExecutor == null) {
                ServerUtil.writeJSONResponse(exchange, ExecuteResponse.builder().err("Connection " + id + " not found!").build());
                return;
            }

            try {
                String[] sqlList = executeDTO.getSqlList();
                if (sqlList != null) {
                    ServerUtil.writeJSONResponse(exchange, jdbcExecutor.executeBatch(sqlList, executeDTO));
                } else {
                    executeResponse = jdbcExecutor.execute(executeDTO.getSql(), executeDTO);
                }
            } catch (Error | Exception e) {
                String errorMessage = e instanceof Error ? e.toString() : e.getMessage();
                errorMessage = translator.doTranslate(errorMessage);
                if (StringUtils.isEmpty(errorMessage)) errorMessage = e.getClass().getName();
                executeResponse = ExecuteResponse.builder().err(errorMessage).build();
                log.error(e.getMessage(), e);
            } finally {
                ServerUtil.writeJSONResponse(exchange, executeResponse);
            }
        });
        server.createContext("/close", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            log.info("Close connection for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            try {
                if (jdbcExecutor != null) {
                    jdbcExecutor.close();
                }
            } catch (Exception ignored) {
                log.error("Close connection fail", ignored);
            } finally {
                executorMap.remove(connectDTO.getJdbcUrl());
                ServerUtil.writeJSONResponse(exchange, "");
            }
        });
        server.createContext("/cancel", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            log.info("Cancel execute for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            if (jdbcExecutor != null) {
                jdbcExecutor.cancel();
            }
        });
        server.createContext("/alive", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            log.info("Check connection {} alive..", id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            AliveCheckResponse aliveCheckResponse = null;
            try {
                if (jdbcExecutor != null) {
                    boolean alive = !jdbcExecutor.getConnection().isClosed();
                    if (alive) {
                        jdbcExecutor.testAlive();
                    }
                    aliveCheckResponse = new AliveCheckResponse(alive);
                }
            } catch (Exception e) {
                aliveCheckResponse = new AliveCheckResponse(false);
                e.printStackTrace();
            } finally {
                ServerUtil.writeJSONResponse(exchange, aliveCheckResponse);
            }
        });
        server.createContext("/", exchange -> {
            String filePath = exchange.getRequestURI().getPath();
            if (filePath.equals("/")) filePath = "/index.html";

            // 获取文件扩展名
            String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            String contentType = getContentType(extension);

            java.io.InputStream inputStream = JdbcExecutorServer.class.getClassLoader()
                    .getResourceAsStream("static" + filePath);
            if (inputStream == null) {
                log.error("Resource not found: " + filePath);
                ServerUtil.notFound(exchange);
            } else {
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();
                ServerUtil.writeResponse(exchange, bytes, contentType);
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        log.info("HTTP server started on port {}, Cost time: {} ms", server.getAddress().getPort(), new Date().getTime() - start);
        log.info("JAVA library path: {}", LibraryUtils.getJavaLibraryPath());
    }

    private static String getContentType(String extension) {
        switch (extension) {
            case "html":
            case "htm":
                return "text/html; charset=UTF-8";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            default:
                return "application/octet-stream";
        }
    }

}
