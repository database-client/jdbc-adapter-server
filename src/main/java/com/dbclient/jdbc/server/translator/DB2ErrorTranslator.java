package com.dbclient.jdbc.server.translator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DB2ErrorTranslator {

    private static final Map<String, String> errorMap = new HashMap<>();

    static {
        // data too long for column
        errorMap.put("-302", "The value is too large for the column.");
        errorMap.put("22001", "The value is too large for the column.");
        // data type conversion error
        errorMap.put("-420", "The value cannot be converted to the target data type.");
        errorMap.put("22018", "The value cannot be converted to the target data type.");
        // other errors
        errorMap.put("-104", "Syntax error: Check the SQL statement syntax.");
        errorMap.put("42601", "Syntax error: Check the SQL statement syntax.");
        errorMap.put("-407", "Null value error: A column that does not allow nulls was assigned a null value.");
        errorMap.put("23502", "Null value error: A column that does not allow nulls was assigned a null value.");
        errorMap.put("-530", "Foreign key violation: The insert or update value is not present in the parent table.");
        errorMap.put("23503", "Foreign key violation: The insert or update value is not present in the parent table.");
        errorMap.put("-803", "Unique constraint violation: Duplicate value in a column with a unique constraint.");
        errorMap.put("23505", "Unique constraint violation: Duplicate value in a column with a unique constraint.");
        errorMap.put("-551", "Authorization error: The user does not have the necessary privileges.");
        errorMap.put("42501", "Authorization error: The user does not have the necessary privileges.");
        errorMap.put("-911", "Deadlock or timeout: The current transaction has been rolled back due to a deadlock or timeout.");
        errorMap.put("40001", "Deadlock or timeout: The current transaction has been rolled back due to a deadlock or timeout.");
        errorMap.put("-913", "Deadlock or timeout: The current transaction has been rolled back due to a deadlock or timeout.");
        errorMap.put("57033", "Deadlock or timeout: The current transaction has been rolled back due to a deadlock or timeout.");
        errorMap.put("-805", "Package not found: The package specified in the request was not found.");
        errorMap.put("51002", "Package not found: The package specified in the request was not found.");
        errorMap.put("-818", "Timestamp mismatch: The timestamp of the program and the package do not match.");
        errorMap.put("51003", "Timestamp mismatch: The timestamp of the program and the package do not match.");
        // 可以根据需要继续添加更多错误代码和信息
    }

    public static String getErrorMessage(int sqlCode, String sqlState) {
        String message = errorMap.get(String.valueOf(sqlCode));
        if (message == null) {
            message = errorMap.get(sqlState);
        }
        String suffix = " SQLCODE=" + sqlCode + ", SQLSTATE=" + sqlState;
        return message != null ? message + suffix : "Unknown error:" + suffix;
    }

    public static void main(String[] args) {
        // 示例用法
        int sqlCode = -302;
        String sqlState = "22001";

        String errorMessage = getErrorMessage(sqlCode, sqlState);
        System.out.println("Error Message: " + errorMessage);
    }

    public String doTranslate(String errorMessage) {
        if (errorMessage == null || !errorMessage.contains("SQLCODE")) {
            return errorMessage;
        }

        Pattern pattern = Pattern.compile("SQLCODE=(-?\\d+), SQLSTATE=(\\w+)");
        Matcher matcher = pattern.matcher(errorMessage);
        int sqlCode = 0;
        String sqlState = null;
        if (matcher.find()) {
            sqlCode = Integer.parseInt(matcher.group(1));
            sqlState = matcher.group(2);
        }
        return getErrorMessage(sqlCode, sqlState);
    }
}