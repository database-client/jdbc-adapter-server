package com.dbclient.jdbc.server.dto;

import lombok.Data;

@Data
public class ExecuteDTO {
    private String sql;
    private String[] sqlList;
    private String id;
    private Integer skipRows;
    private Integer fetchSize;
}
