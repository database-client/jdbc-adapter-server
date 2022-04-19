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
    private Statement statement;
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
        this.statement = connection.createStatement();
        this.statement.execute(sql);
        this.statement = null;
        return ExecuteResponse.builder().build();
    }

    @SneakyThrows
    public QueryBO executeQuery(String sql) {
        this.statement = connection.createStatement();
        ResultSet rs = this.statement.executeQuery(sql);
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
        this.statement = null;
        return new QueryBO(rows, columns);
    }

    public void cancel() {
        if (this.statement != null) {
            try {
                this.statement.cancel();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                this.statement = null;
            }
        }
    }

    public void close() throws Exception {
        //关闭连接和释放资源
        if (statement != null) {
            statement.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}