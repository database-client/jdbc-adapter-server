package com.dbclient.jdbc.server.driver;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DriverShim implements Driver {
    private final Driver driver;

    DriverShim(Driver d) {
        this.driver = d;
    }

    public boolean acceptsURL(String u) throws SQLException {
        return this.driver.acceptsURL(u);
    }

    /**
     * The driver should return "null" if it realizes it is the wrong kind
     * of driver to connect to the given URL.
     * <P>The driver should throw an <code>SQLException</code> if it is the right
     * driver to connect to the given URL but has trouble connecting to
     * the database.
     */
    public Connection connect(String url, Properties p) throws SQLException {
        return this.driver.connect(url, p);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return this.driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return this.driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return this.driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return this.driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.driver.getParentLogger();
    }
}
