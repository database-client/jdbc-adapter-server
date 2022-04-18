package com.dbclient.jdbc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryBO {
    private List<List<Object>> rows;
    private List<ColumnMeta> columns;
}
