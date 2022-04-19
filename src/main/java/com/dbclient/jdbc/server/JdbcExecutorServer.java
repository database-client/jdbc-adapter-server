package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.ExecuteDTO;
import com.dbclient.jdbc.server.response.AliveCheckResponse;
import com.dbclient.jdbc.server.response.ConnectResponse;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import com.dbclient.jdbc.server.util.ServerUtil;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class JdbcExecutorServer {

    public static final Map<String, JdbcExecutor> executorMap = new HashMap<>();

    @SneakyThrows
    public static void main(String[] args) {

        HttpServer server = HttpServer.create(new InetSocketAddress(7823), 0);
        server.createContext("/test", exchange -> {
            ServerUtil.writeResponse(exchange, "hello");
        });
        server.createContext("/connect", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String errorMessage = null;
            try {
                String id = connectDTO.getId();
                System.out.println("Create connection for " + id);
                JdbcExecutor jdbcExecutor = executorMap.get(id);
                if (jdbcExecutor == null) {
                    jdbcExecutor = JdbcExecutor.create(connectDTO);
                    executorMap.put(id, jdbcExecutor);
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                e.printStackTrace();
            } finally {
                ServerUtil.writeResponse(exchange, ConnectResponse.builder()
                        .success(errorMessage == null)
                        .err(errorMessage)
                        .build());
            }
        });
        server.createContext("/execute", exchange -> {
            ExecuteDTO executeDTO = ServerUtil.read(exchange, ExecuteDTO.class);
            String id = executeDTO.getId();
            System.out.println("Execute SQL for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            ExecuteResponse executeResponse = null;
            try {
                if (jdbcExecutor != null) {
                    executeResponse = jdbcExecutor.execute(executeDTO.getSql());
                }
            } catch (Exception e) {
                executeResponse = ExecuteResponse.builder().err(e.getMessage()).build();
                e.printStackTrace();
            } finally {
                System.out.println(id);
                ServerUtil.writeResponse(exchange, executeResponse);
            }
        });
        server.createContext("/close", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            System.out.println("Close connection for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            try {
                if (jdbcExecutor != null) {
                    jdbcExecutor.close();
                }
            } catch (Exception ignored) {
            } finally {
                executorMap.remove(connectDTO.getJdbcUrl());
                ServerUtil.writeResponse(exchange, "");
            }
        });
        server.createContext("/cancel", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            System.out.println("Cancel execute for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            if (jdbcExecutor != null) {
                jdbcExecutor.cancel();
            }
        });
        server.createContext("/alive", exchange -> {
            ConnectDTO connectDTO = ServerUtil.read(exchange, ConnectDTO.class);
            String id = connectDTO.getId();
            System.out.println("Check alive for " + id);
            JdbcExecutor jdbcExecutor = executorMap.get(id);
            AliveCheckResponse aliveCheckResponse = null;
            try {
                if (jdbcExecutor != null) {
                    aliveCheckResponse = new AliveCheckResponse(!jdbcExecutor.getConnection().isClosed());
                }
            } catch (Exception e) {
                aliveCheckResponse = new AliveCheckResponse(false);
                e.printStackTrace();
            } finally {
                ServerUtil.writeResponse(exchange, aliveCheckResponse);
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP server started on port " + server.getAddress().getPort());
    }

}
