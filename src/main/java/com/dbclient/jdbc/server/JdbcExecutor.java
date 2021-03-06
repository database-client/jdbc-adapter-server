package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ColumnMeta;
import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.QueryBO;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.TIMESTAMP;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JdbcExecutor {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Statement statement;
    private final Connection connection;
    private final Map<String, URLClassLoader> loaderMap = new HashMap<>();

    @SneakyThrows
    public JdbcExecutor(ConnectDTO connectDTO) {
        this.checkClass(connectDTO);
        connection = connectDTO.getUsername() == null ? DriverManager.getConnection(connectDTO.getJdbcUrl()) :
                DriverManager.getConnection(connectDTO.getJdbcUrl(), connectDTO.getUsername(), connectDTO.getPassword());
        if (connectDTO.isReadonly()) {
            connection.setReadOnly(true);
        }
    }

    @SneakyThrows
    private void checkClass(ConnectDTO connectDTO) {
        if (connectDTO.getDriverPath() != null) {
            if (!loaderMap.containsKey(connectDTO.getDriverPath())) {
                this.loadDriver(connectDTO);
            }
        } else {
            Class.forName(connectDTO.getDriver());
        }
    }

    @SneakyThrows
    private void loadDriver(ConnectDTO connectDTO) {
        String driverPath = connectDTO.getDriverPath();
        String driver = connectDTO.getDriver();
        URL u = new URL("jar:file:" + driverPath + "!/");
        URLClassLoader ucl = new URLClassLoader(new URL[]{u});
        Driver d = (Driver) Class.forName(driver, true, ucl).newInstance();
        DriverManager.registerDriver(new DriverShim(d));
        loaderMap.put(driverPath, ucl);
    }

    /**
     * Batch execute sql
     *
     * @param sqlList sql array
     */
    @SneakyThrows
    public List<ExecuteResponse> executeBatch(String[] sqlList) {
        return Arrays.stream(sqlList).map(this::execute).collect(Collectors.toList());
    }

    @SneakyThrows
    public synchronized ExecuteResponse execute(String sql) {
        log.info("Executing SQL: {}", sql);
        String lowerSQL = sql.toLowerCase();
        if (lowerSQL.startsWith("select")) {
            QueryBO queryBO = this.executeQuery(sql);
            return ExecuteResponse.builder()
                    .rows(queryBO.getRows())
                    .columns(queryBO.getColumns())
                    .build();
        }
        this.statement = connection.createStatement();
        if (lowerSQL.startsWith("update") || lowerSQL.startsWith("delete")) {
            int affectedRows = this.statement.executeUpdate(sql);
            this.statement = null;
            return ExecuteResponse.builder()
                    .affectedRows(affectedRows)
                    .build();
        }
        this.statement.execute(sql);
        this.statement.close();
        this.statement = null;
        return ExecuteResponse.builder().build();
    }

    @SneakyThrows
    public QueryBO executeQuery(String sql) {
        this.statement = connection.createStatement();
        ResultSet rs = this.statement.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // ???????????????
        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            int i1 = i + 1;
            columns.add(new ColumnMeta(metaData.getColumnLabel(i1), metaData.getColumnTypeName(i1), metaData.getTableName(i1)));
        }
        // ????????????????????????
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(getColumnValue(rs, i));
            }
            rows.add(row);
        }
        this.statement.close();
        this.statement = null;
        return new QueryBO(rows, columns);
    }

    private Object getColumnValue(ResultSet rs, int i) throws SQLException {
        Object object = rs.getObject(i);
        object = parseClob(object);
        if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime().format(dateTimeFormatter);
        }
        if (object instanceof TIMESTAMP) {
            return ((TIMESTAMP) object).toLocalDateTime().format(dateTimeFormatter);
        }
        return object;
    }

    private Object parseClob(Object object) throws SQLException {
        if (object instanceof Clob) {
            Clob clob = (Clob) object;
            return clob.getSubString(1, (int) clob.length());
        }
        return object;
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
        //???????????????????????????
        if (statement != null) {
            statement.cancel();
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
