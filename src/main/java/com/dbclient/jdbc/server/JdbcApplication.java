package com.dbclient.jdbc.server;

import io.vertx.core.Vertx;
import lombok.SneakyThrows;

public class JdbcApplication {

    @SneakyThrows
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(JdbcExecutorServer.class.getName());
    }

}
