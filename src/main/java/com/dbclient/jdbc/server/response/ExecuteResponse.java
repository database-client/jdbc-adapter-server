package com.dbclient.jdbc.server.response;

import com.dbclient.jdbc.server.dto.ColumnMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteResponse {
    private Integer affectedRows;
    private String err;
    private List<List<Object>> rows;
    private List<ColumnMeta> columns;
}
