package com.dbclient.jdbc.server.dto.execute;

import lombok.Data;

@Data
public class ExecuteDTO {
    private String sql;
    private SQLParam[] params;
    private String[] sqlList;
    private SQLParam[][] paramsList;
    private String id;
    private Integer skipRows;
    private Integer fetchSize;
}
