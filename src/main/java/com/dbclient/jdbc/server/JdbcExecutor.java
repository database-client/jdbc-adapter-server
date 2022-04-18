package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ColumnMeta;
import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.QueryBO;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcExecutor {
    PreparedStatement ps;
    private final Connection connection;

    @SneakyThrows
    public JdbcExecutor(String driver, String jdbcUrl, String username, String password) {
        Class.forName(driver);
        connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static JdbcExecutor create(ConnectDTO connectDTO) {
        String jdbcUrl = connectDTO.getJdbcUrl();
        return new JdbcExecutor(connectDTO.getDriver(), jdbcUrl, connectDTO.getUsername(), connectDTO.getPassword());
    }

    @SneakyThrows
    public ExecuteResponse execute(String sql) {
        if (sql.toLowerCase().startsWith("select")) {
            QueryBO queryBO = this.executeQuery(sql);
            return ExecuteResponse.builder()
                    .rows(queryBO.getRows())
                    .columns(queryBO.getColumns())
                    .build();
        }
        connection.prepareStatement(sql).execute();
        return ExecuteResponse.builder().build();
    }

    @SneakyThrows
    public QueryBO executeQuery(String sql) {
        ResultSet rs = connection.prepareStatement(sql).executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 生成列信息
        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            int i1 = i + 1;
            columns.add(new ColumnMeta(metaData.getColumnName(i1), metaData.getColumnTypeName(i1), metaData.getTableName(i1)));
        }
        // 生成二维数组数据
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }
        return new QueryBO(rows, columns);
    }

    public void close() throws Exception {
        //关闭连接和释放资源
        if (connection != null) {
            connection.close();
        }
        if (ps != null) {
            ps.close();
        }
    }

}
