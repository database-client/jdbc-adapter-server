# Jdbc Adapter Server

This is a project to open jdbc as http service.

## Usage

1. Download the jar file from [release](https://github.com/database-client/jdbc-adapter-server/releases).
2. Run the jar file: `java -jar dbclient-jdbc-1.4.0.jar`
3. After execute, http-api will be exposed on port 7823. 

## Http-Api

The http service will provide the following Api.

| Api                 | Desc                             |
|---------------------|----------------------------------|
| [connect](#connect) | Connect to database by jdbc url. |
| [alive](#alive)     | Check connection is alive.       |
| [execute](#execute) | Execute SQL by connection.       |
| [cancel](#cancel)   | Cancel executing statement.      |
| [close](#close)     | Close jdbc connection.           |

## Request Example

### connect

```http request
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

#### Parameter

| Parameter  | Required | Type    | Description                                  |
|------------|----------|---------|----------------------------------------------|
| id         | ✅       | String  | The ID of the connection. |
| jdbcUrl    | ✅       | String  | The JDBC URL of the database.                |
| driverPath | ✅       | String  | The path to the JDBC driver. It can be a JAR file, a directory, or a compressed archive. |
| driver     | ❌       | String  | The class name of the JDBC driver.           |
| username   | ❌       | String  | The username for the database.               |
| password   | ❌       | String  | The password for the database.               |
| readonly   | ❌       | Boolean | Whether to connect to the database in read-only mode. |

### alive

```http request
POST http://127.0.0.1:7823/connect
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

### execute

```http request
POST http://127.0.0.1:7823/execute
Content-Type: application/json

{
  "id": "mysql-connection",
  "sql": "select * from mysql.user where user=?",
  "params": [{"value": "root"}],
  "sqlList": ["select * from mysql.user","select * from mysql.user"]
}
```

Parameter:
- sql: The SQL you want to execute.
- params: The parameters of the SQL, the format is like this: [{"value": "value1"}, {"value": "value2"}].
- sqlList: the SQL list you want to batch execute, When parameter sqlList is not empty, parameter sql will be ignored.

### cancel

```http request
POST http://127.0.0.1:7823/cancel
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

### close

```http request
POST http://127.0.0.1:7823/close
Content-Type: application/json

{
  "id": "mysql-connection"
}
```

##

build: gradle fatJar
