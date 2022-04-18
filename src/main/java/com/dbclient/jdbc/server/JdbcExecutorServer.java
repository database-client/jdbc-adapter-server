package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.ExecuteDTO;
import com.dbclient.jdbc.server.response.AliveCheckResponse;
import com.dbclient.jdbc.server.response.ConnectResponse;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;

public class JdbcExecutorServer extends AbstractVerticle {

    public static final Map<String, JdbcExecutor> executorMap = new HashMap<>();

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/connect").handler(context -> {
            ConnectDTO connectDTO = context.getBodyAsJson().mapTo(ConnectDTO.class);
            String errorMessage = null;
            try {
                JdbcExecutor jdbcExecutor = executorMap.get(connectDTO.getId());
                if (jdbcExecutor == null) {
                    jdbcExecutor = JdbcExecutor.create(connectDTO);
                    executorMap.put(connectDTO.getId(), jdbcExecutor);
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                e.printStackTrace();
            } finally {
                context.json(ConnectResponse.builder()
                        .success(errorMessage == null)
                        .err(errorMessage)
                        .build());
            }
        });
        router.post("/execute").handler(context -> {
            ExecuteDTO executeDTO = context.getBodyAsJson().mapTo(ExecuteDTO.class);
            JdbcExecutor jdbcExecutor = executorMap.get(executeDTO.getId());
            ExecuteResponse executeResponse = null;
            try {
                if (jdbcExecutor != null) {
                    executeResponse = jdbcExecutor.execute(executeDTO.getSql());
                }
            } catch (Exception e) {
                executeResponse = ExecuteResponse.builder().err(e.getMessage()).build();
                e.printStackTrace();
            } finally {
                context.json(executeResponse);
            }
        });
        router.post("/close").handler(context -> {
            ConnectDTO connectDTO = context.getBodyAsJson().mapTo(ConnectDTO.class);
            JdbcExecutor jdbcExecutor = executorMap.get(connectDTO.getId());
            try {
                if (jdbcExecutor != null) {
                    jdbcExecutor.close();
                }
            } catch (Exception ignored) {
            } finally {
                executorMap.remove(connectDTO.getId());
                context.end("");
            }
        });
        router.post("/alive").handler(context -> {
            ConnectDTO connectDTO = context.getBodyAsJson().mapTo(ConnectDTO.class);
            JdbcExecutor jdbcExecutor = executorMap.get(connectDTO.getId());
            AliveCheckResponse aliveCheckResponse = null;
            try {
                if (jdbcExecutor != null) {
                    aliveCheckResponse = new AliveCheckResponse(jdbcExecutor.getConnection().isClosed());
                }
            } catch (Exception e) {
                aliveCheckResponse = new AliveCheckResponse(false);
                e.printStackTrace();
            } finally {
                context.json(aliveCheckResponse);
            }
        });
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(7823)
                .onSuccess(server ->
                        System.out.println("HTTP server started on port " + server.actualPort())
                );
    }
}
