package com.dbclient.jdbc.server.dto.execute;

import lombok.Data;

@Data
public class SQLParam {
    private String value;
    private SQLParamType type;
}
