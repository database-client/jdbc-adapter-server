package com.dbclient.jdbc.server.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ConnectDTO {
    private String id;
    private String jdbcUrl;
    private String driver;
    private String driverPath;
    private String username;
    private String password;
    private boolean readonly;
    private String aliveSQL;
    private boolean noTableName;
    private boolean supportForward;
    private Map<String,Object> properties;
}
