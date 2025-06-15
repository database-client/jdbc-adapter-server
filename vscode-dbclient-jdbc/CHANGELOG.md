# Change Log

# 1.4.5-6 2025-6-15

- Fixed unable to retrieve the return value of the stored procedure.

# 1.4.4 2024-4-8

- implement the fault tolerance for driver loading failures.

# 1.4.3 2024-4-7

- Logging to a file.

# 1.4.2 2024-3-17

- Support configuration of supportForward.
- Fix Oracle driver not being automatically loaded.

# 1.4.1 2024-3-16

- Add Azure connection library.

# 1.4.0 2024-3-16

- Automatically load drivers through service discovery.
- Support loading drivers through folders and compressed files.

# 1.3.9 2024-12-6

- Support parsing date and time.

# 1.3.8 2024-12-2

- Support for inserting binary data.
- Support displaying more jdbc types.

# 1.3.7 2024-11-25

- Added parsing support for Ref, Struct, SQLXML and array types.

# 1.3.6 2024-9-4

- Fix cannot display desc results.

# 1.3.5 2024-5-2

- Fix parsing error when JSON key is a number(Using Jackson instead of FastJSON).

# 1.3.3 2023-12-7

- Better pagination support.
- Bind jdbc server to 127.0.0.1.
- Support configuring custom parameters.

# 1.3.2 2023-12-4

- Support config alive SQL.
- Fix failed to get hive table name.

# 1.3.0 2023-12-1

- Support parse blob and date.
- Support execute use statement.
- Update connection verification SQL.

# 1.2.6 2023-3-29

- Support skip rows.
- Support for limiting the size of rows when querying.

# 1.2.0 2022-6-6

- Support load jdbc drivers dynamically.
- Kill jdbc server after reload vscode.

# 1.1.3 2022-5-29

- Synchronous SQL execution.

# 1.1.1 2022-5-19

- Using multiple thread and support cancel connection.

# 1.1.0 2022-5-18

- Support set connection as readonly.
- Support show correct time value.
