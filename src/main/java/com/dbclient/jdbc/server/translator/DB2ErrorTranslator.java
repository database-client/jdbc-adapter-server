package com.dbclient.jdbc.server.translator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DB2ErrorTranslator {

    private static final Map<String, String> errorMap = new HashMap<>();

    static {
        // data too long for column
        errorMap.put("-302", "Data is too large for the column.");
        errorMap.put("22001", "Data is too large for the column.");
        // data type conversion error
        errorMap.put("-420", "Data cannot be converted to the target data type '?'.");
        errorMap.put("22018", "Data cannot be converted to the target data type '?'.");
        // column not found
        errorMap.put("-206", "Unknown column '?' in 'field list'.");
        errorMap.put("42703", "Unknown column '?' in 'field list'.");
        // table or view not found
        errorMap.put("-204", "Table or view '?' not found.");
        errorMap.put("42704", "Table or view '?' not found.");
        // column cannot be null
        errorMap.put("-407", "Column '?' cannot be null.");
        errorMap.put("23502", "Column '?' cannot be null.");
        // other errors
        errorMap.put("-104", "SQL syntax error.");
        errorMap.put("42601", "SQL syntax error.");
        errorMap.put("-530", "Cannot add or update a child row: a foreign key constraint fails ('?' CONSTRAINT '?').");
        errorMap.put("23503", "Cannot add or update a child row: a foreign key constraint fails ('?' CONSTRAINT '?').");
        errorMap.put("-803", "Duplicate entry '?' for key '?'.");
        errorMap.put("23505", "Duplicate entry '?' for key '?'.");
        errorMap.put("-551", "Access denied for user '?'@'?' (using password: ?).");
        errorMap.put("42501", "Access denied for user '?'@'?' (using password: ?).");
        errorMap.put("-911", "Lock wait timeout exceeded; try restarting transaction.");
        errorMap.put("40001", "Lock wait timeout exceeded; try restarting transaction.");
        errorMap.put("-913", "Lock wait timeout exceeded; try restarting transaction.");
        errorMap.put("57033", "Lock wait timeout exceeded; try restarting transaction.");
        errorMap.put("-805", "Table '?' doesn't exist.");
        errorMap.put("51002", "Table '?' doesn't exist.");
        errorMap.put("-818", "Table '?' doesn't exist.");
        errorMap.put("51003", "Table '?' doesn't exist.");
    }

    public static String getErrorMessage(int sqlCode, String sqlState, String errorMessageCode) {
        String message = errorMap.get(String.valueOf(sqlCode));
        if (message == null) {
            message = errorMap.get(sqlState);
        }
        String suffix = " SQLCODE=" + sqlCode + ", SQLSTATE=" + sqlState;

        boolean hasErrorMessageCode = errorMessageCode != null && !"null".equals(errorMessageCode);
        if (message != null) {
            if (!hasErrorMessageCode) errorMessageCode = "Unknown";
            String[] errorMessageCodes = errorMessageCode.split(";");
            for (String code : errorMessageCodes) {
                message = message.replaceFirst("\\?", code);
            }
            return message + " \t " + suffix;
        }
        if (hasErrorMessageCode) suffix += ", errorMessageCode=" + errorMessageCode;
        return "Unknown error:" + suffix;
    }

    public String doTranslate(String errorMessage) {
        if (errorMessage == null || !errorMessage.contains("SQLCODE")) {
            return errorMessage;
        }

        Pattern pattern = Pattern.compile("SQLCODE=(-?\\d+), SQLSTATE=(\\w+), SQLERRMC=([^,]+)");
        Matcher matcher = pattern.matcher(errorMessage);
        int sqlCode = 0;
        String sqlState = null;
        String errorMessageCode = null;
        if (matcher.find()) {
            sqlCode = Integer.parseInt(matcher.group(1));
            sqlState = matcher.group(2);
            errorMessageCode = matcher.group(3);
        }
        return getErrorMessage(sqlCode, sqlState, errorMessageCode);
    }
}