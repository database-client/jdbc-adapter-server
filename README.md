# JDBC Adapter Server

A lightweight HTTP service that provides JDBC database access.

## Quick Start

1. Download the latest JAR file from [releases](https://github.com/database-client/jdbc-adapter-server/releases)
2. Run the JAR file: `java -jar jdbc-adapter-server-1.0-all.jar`
3. The HTTP API will be available on port 7823

## API Endpoints

| Endpoint         | Description                                    |
| ---------------- | ---------------------------------------------- |
| [connect](#connect) | Establish a database connection using JDBC URL |
| [alive](#alive)     | Verify connection status                       |
| [execute](#execute) | Execute SQL queries                            |
| [cancel](#cancel)   | Cancel ongoing SQL execution                   |
| [close](#close)     | Close database connection                      |

## API Documentation

### Connect

```http
POST http://127.0.0.1:7823/connect
Content-Type: application/json

{
  "jdbcUrl": "jdbc:mysql://localhost:3306/test",
  "driver": "com.mysql.cj.jdbc.Driver",
  "driverPath": "D:/mysql-connector-java-8.0.29.jar",
  "username": "root",
  "password": "root",
  "readonly": false,
  "id": "mysql-connection"
}
```

#### Parameters

| Parameter  | Required | Type    | Description                                               |
| ---------- | -------- | ------- | --------------------------------------------------------- |
| id         | ✅       | String  | Unique identifier for the connection                      |
| jdbcUrl    | ✅       | String  | JDBC connection URL                                       |
| driverPath | ✅       | String  | Path to JDBC driver (supports JAR, directory, or archive) |
| driver     | ❌       | String  | JDBC driver class name                                    |
| username   | ❌       | String  | Database username                                         |
| password   | ❌       | String  | Database password                                         |
| readonly   | ❌       | Boolean | Enable read-only mode                                     |

### Check Connection Status

```http
POST http://127.0.0.1:7823/alive
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

### Execute SQL

```http
POST http://127.0.0.1:7823/execute
Content-Type: application/json

{
  "id": "mysql-connection",
  "sql": "select * from mysql.user where user=?",
  "params": [{"value": "root"}],
  "sqlList": ["select * from mysql.user","select * from mysql.user"]
}
```

#### Parameters

- `sql`: SQL query to execute
- `params`: Query parameters in format `[{"value": "value1"}, {"value": "value2"}]`
- `sqlList`: Batch SQL queries to execute (takes precedence over `sql` parameter)

### Cancel Execution

```http
POST http://127.0.0.1:7823/cancel
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

### Close Connection

```http
POST http://127.0.0.1:7823/close
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

## Building

To build the project, run:

```bash
gradle fatJar
```
