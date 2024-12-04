package com.dbclient.jdbc.server;

import com.dbclient.jdbc.server.dto.ColumnMeta;
import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.dto.QueryBO;
import com.dbclient.jdbc.server.dto.execute.ExecuteDTO;
import com.dbclient.jdbc.server.dto.execute.SQLParam;
import com.dbclient.jdbc.server.response.ExecuteResponse;
import com.dbclient.jdbc.server.util.PatternUtils;
import com.dbclient.jdbc.server.util.TypeChecker;
import com.dbclient.jdbc.server.util.ValueUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.REF;
import oracle.sql.TIMESTAMP;
import oracle.sql.TIMESTAMPTZ;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JdbcExecutor {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Set<Statement> statements = new HashSet<>();
    private String aliveSQL;
    private final ConnectDTO option;
    private final Connection connection;
    private final Map<String, URLClassLoader> loaderMap = new HashMap<>();

    @SneakyThrows
    public JdbcExecutor(ConnectDTO connectDTO) {
        this.option = connectDTO;
        this.initAliveSQL();
        this.checkClass(connectDTO);
        Properties properties = getConnectProperties(connectDTO);
        connection = DriverManager.getConnection(connectDTO.getJdbcUrl(), properties);
        if (connectDTO.isReadonly()) {
            connection.setReadOnly(true);
        }
    }

    private static Properties getConnectProperties(ConnectDTO connectDTO) {
        Properties properties = new Properties();
        if (!ValueUtils.isEmpty(connectDTO.getUsername())) {
            properties.put("user", connectDTO.getUsername());
        }
        if (!ValueUtils.isEmpty(connectDTO.getPassword())) {
            properties.put("password", connectDTO.getPassword());
        }
        Map<String, Object> paramProperties = connectDTO.getProperties();
        if (paramProperties != null) {
            properties.putAll(paramProperties);
        }
        return properties;
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
     * @param sqlList    sql array
     * @param executeDTO
     */
    @SneakyThrows
    public List<ExecuteResponse> executeBatch(String[] sqlList, ExecuteDTO executeDTO) {
        executeDTO.setParams(null);
        return Arrays.stream(sqlList)
                .map(s -> execute(s, executeDTO))
                .collect(Collectors.toList());
    }

    public boolean isOracle() {
        return this.option.getDriver().contains("Oracle");
    }

    public void testAlive() {
        try {
            execute(aliveSQL, new ExecuteDTO());
        } catch (Exception e) {
            if (e instanceof SQLSyntaxErrorException) {
                log.error(e.getMessage(), e);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    private void initAliveSQL() {
        aliveSQL = option.getAliveSQL();
        if (ValueUtils.isEmpty(aliveSQL)) {
            aliveSQL = "SELECT 1";
            if (isOracle()) aliveSQL += " FROM DUAL";
        }
    }

    @SneakyThrows
    public synchronized ExecuteResponse execute(String sql, ExecuteDTO executeDTO) {
        log.info("Executing SQL: {}", sql);
        String lowerSQL = sql.toLowerCase();
        if (!PatternUtils.match(lowerSQL, "^\\s*(select|show|desc|describe|with)")) {
            PreparedStatement statement = connection.prepareStatement(sql);
            fillParams(statement, executeDTO.getParams());
            int affectedRows = statement.executeUpdate();
            closeStatement(statement);
            return ExecuteResponse.builder()
                    .affectedRows(affectedRows)
                    .build();
        }
        QueryBO queryBO = this.executeQuery(sql, executeDTO);
        return ExecuteResponse.builder()
                .rows(queryBO.getRows())
                .columns(queryBO.getColumns())
                .build();
    }

    @SneakyThrows
    private void fillParams(PreparedStatement statement, SQLParam[] params) {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            SQLParam param = params[i];
            if (param == null) continue;
            switch (param.getType()) {
                case HEX:
                    statement.setBytes(i + 1, ValueUtils.hexToBytes(param.getValue()));
                    break;
                default:
                    statement.setObject(i + 1, param.getValue());
            }
        }
    }


    @SneakyThrows
    private Statement newStatement() {
        Statement statement = !isOracle() ? connection.createStatement() :
                connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_UPDATABLE);
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
    public QueryBO executeQuery(String sql, ExecuteDTO executeDTO) {
        Statement statement = newStatement();
        Integer skipRows = executeDTO.getSkipRows();
        Integer fetchSize = executeDTO.getFetchSize();
        if (fetchSize != null && skipRows == null) {
            statement.setMaxRows(fetchSize);
            statement.setFetchSize(fetchSize);
        }
        ResultSet rs = statement.executeQuery(sql);
        log.info("执行结束, 开始获取数据...");
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 生成列信息
        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            int i1 = i + 1;
            columns.add(new ColumnMeta(metaData.getColumnLabel(i1), metaData.getColumnTypeName(i1), getTableName(metaData, i1)));
        }
        // 生成二维数组数据
        if (skipRows != null) {
            if (this.option.isNotSupportForward()) {
                for (int i = 0; i < skipRows; i++) rs.next();
            } else {
                if (fetchSize != null) rs.setFetchSize(fetchSize);
                rs.absolute(skipRows);
            }
        }
        int fetchedCount = 0;
        List<List<Object>> rows = new ArrayList<>();
        try {
            while (rs.next() && (fetchSize == null || fetchedCount < fetchSize)) {
                List<Object> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(getColumnValue(rs, i));
                }
                rows.add(row);
                fetchedCount++;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            closeStatement(statement);
        }
        log.info("获取数据完成!");
        return new QueryBO(rows, columns);
    }

    private String getTableName(ResultSetMetaData metaData, int i1) throws SQLException {
        if (this.option.getDriver().contains("Hive") || option.isNoTableName()) return null;
        try {
            return metaData.getTableName(i1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private Object getColumnValue(ResultSet rs, int i) throws SQLException {
        Object object = rs.getObject(i);
        return convertValue(object);
    }

    @SneakyThrows
    private static Object convertValue(Object object) {
        if (object == null) return null;
        if (TypeChecker.isPrimary(object.getClass())) return object;
        if (object instanceof ByteBuffer) {
            return ValueUtils.bytesToHex(((ByteBuffer) object).array());
        } else if (object instanceof Clob) {
            Clob clob = (Clob) object;
            return clob.getSubString(1, (int) clob.length());
        } else if (object instanceof Blob) {
            byte[] bytes = ((Blob) object).getBytes(1, (int) ((Blob) object).length());
            return ValueUtils.bytesToHex(bytes);
        } else if (object instanceof java.sql.Date || object instanceof Time) {
            return object.toString();
        } else if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime().format(dateTimeFormatter);
        }
        else if (object instanceof TIMESTAMP) { // Oracle内置的两个类型
            return ((TIMESTAMP) object).toLocalDateTime().format(dateTimeFormatter);
        } else if (object instanceof TIMESTAMPTZ) {
            return ((TIMESTAMPTZ) object).toLocalDateTime().format(dateTimeFormatter);
        } else if (object instanceof Date) {
            return dateFormat.format(object);
        } else if (object instanceof Array) {
            return Arrays.stream((Object[]) ((Array) object).getArray())
                    .map(JdbcExecutor::convertValue)
                    .toArray(Object[]::new);
        } else if (object.getClass() == byte[].class) {
            return ValueUtils.bytesToHex((byte[]) object);
        } else if (object.getClass() == Object[].class) {
            return Arrays.stream((Object[]) object)
                    .map(JdbcExecutor::convertValue)
                    .toArray(Object[]::new);
        } else if (object instanceof SQLXML) {
            return ((SQLXML) object).getString();
        } else if (object instanceof Struct) {
            return convertValue(((Struct) object).getAttributes());
        } else if (object instanceof Ref) {
            return ((Ref) object).getBaseTypeName() + " -> {" +
                    Arrays.stream((Object[]) convertValue(((Struct) ((REF) object).getObject()).getAttributes()))
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "))
                    + "}";
        }
        return object.toString();
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
