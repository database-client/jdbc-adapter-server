# Jdbc Adapter Server

This is a project to open jdbc as http service.

## Develop

1. run com.dbclient.jdbc.server.JdbcExecutorServer.main
2. After execute, http-api will be exposed on port 7823. 

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

Parameter:
id: The id of the connection, specified by the user.

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
