package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ColumnMeta;
import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.QueryBO;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import com.dbclient.jdbc.server.util.PatternUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.BLOB;
import oracle.sql.TIMESTAMP;

import java.io.BufferedInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JdbcExecutor {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Set<Statement> statements = new HashSet<>();
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
        if (!new File(driverPath).exists()) {
            throw new RuntimeException("Driver " + driverPath + " not exists!");
        }
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
    public List<ExecuteResponse> executeBatch(String[] sqlList, Integer fetchCount) {
        return Arrays.stream(sqlList).map(s -> execute(s, fetchCount)).collect(Collectors.toList());
    }

    @SneakyThrows
    public synchronized ExecuteResponse execute(String sql, Integer fetchCount) {
        log.info("Executing SQL: {}", sql);
        String lowerSQL = sql.toLowerCase();
        if (PatternUtils.match(lowerSQL, "^\\s*(insert|update|delete)")) {
            Statement statement = newStatement();
            int affectedRows = statement.executeUpdate(sql);
            closeStatement(statement);
            return ExecuteResponse.builder()
                    .affectedRows(affectedRows)
                    .build();
        }
        QueryBO queryBO = this.executeQuery(sql, fetchCount);
        return ExecuteResponse.builder()
                .rows(queryBO.getRows())
                .columns(queryBO.getColumns())
                .build();
    }


    @SneakyThrows
    private Statement newStatement() {
        Statement statement = connection.createStatement();
        statements.add(statement);
        return statement;
    }

    private void closeStatement(Statement statement) throws SQLException {
        try {
            statement.close();
        } finally {
            statements.remove(statement);
        }
    }

    @SneakyThrows
    public QueryBO executeQuery(String sql, Integer fetchCount) {
        Statement statement = newStatement();
        if (fetchCount != null) {
            statement.setMaxRows(fetchCount);
            statement.setFetchSize(fetchCount);
        }
        ResultSet rs = statement.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 生成列信息
        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            int i1 = i + 1;
            columns.add(new ColumnMeta(metaData.getColumnLabel(i1), metaData.getColumnTypeName(i1), metaData.getTableName(i1)));
        }
        // 生成二维数组数据
        List<List<Object>> rows = new ArrayList<>();
        try {
            while (rs.next()) {
                List<Object> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(getColumnValue(rs, i));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeStatement(statement);
        }
        return new QueryBO(rows, columns);
    }

    private Object getColumnValue(ResultSet rs, int i) throws SQLException {
        Object object = rs.getObject(i);
        object = parseClob(object);
        if (object instanceof BLOB) {
            object = "Not Support Blob";
        }
        if (object instanceof BigDecimal) {
            object = object.toString();
        }
        if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime().format(dateTimeFormatter);
        }
        if (object instanceof TIMESTAMP) {
            return ((TIMESTAMP) object).toLocalDateTime().format(dateTimeFormatter);
        }
        return object;
    }

    @SneakyThrows
    private static String blobToString(BLOB blob) {
        if (blob == null) return null;
        byte[] data = new byte[(int) blob.length()];
        try (BufferedInputStream instream = new BufferedInputStream(blob.getBinaryStream())) {
            instream.read(data);
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
        return new String(data);
    }

    private Object parseClob(Object object) throws SQLException {
        if (object instanceof Clob) {
            Clob clob = (Clob) object;
            return clob.getSubString(1, (int) clob.length());
        }
        return object;
    }

    @SneakyThrows
    public void cancel() {
        for (Statement statement : statements) {
            try {
                statement.cancel();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            } finally {
                statement.close();
            }
        }
    }

    public void close() throws Exception {
        for (Statement statement : statements) {
            try {
                statement.cancel();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            } finally {
                statement.close();
            }
        }
        if (connection != null) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
