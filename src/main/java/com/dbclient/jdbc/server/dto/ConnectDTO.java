package com.dbclient.jdbc.server.dto;

import lombok.Data;

@Data
public class ConnectDTO {
    private String jdbcUrl;
    private String driver;
    private String username;
    private String password;
}
