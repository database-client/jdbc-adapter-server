package com.dbclient.jdbc.server.dto;

import lombok.Data;

@Data
public class ExecuteDTO {
    private String sql;
    private String id;
}
