package com.dbclient.jdbc.server.driver;

import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.util.StringUtils;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.*;

public abstract class DriverLoader {

    private static final Map<String, Boolean> loaderMap = new HashMap<>();
    private static final Map<String, Boolean> driverMap = new HashMap<>();


    @SneakyThrows
    public static void loadDriverByDTO(ConnectDTO connectDTO) {
        String path = connectDTO.getDriverPath();
        if (StringUtils.isEmpty(path)) return;
        loaderMap.computeIfAbsent(path, (k) -> {
            DriverLoader.loadDriver(connectDTO.getDriverPath(), connectDTO.getDriver());
            return true;
        });
    }


    @SneakyThrows
    private static void loadDriver(String driverPath, String driverClassName) {
        File driverFile = new File(driverPath);

        if (!driverFile.exists()) {
            throw new RuntimeException("Driver path " + driverPath + " not exists!");
        }

        URLClassLoader driverLoader = getUrlClassLoader(driverFile, driverPath);

        // Auto load driver
        ServiceLoader<Driver> serviceLoader = ServiceLoader.load(Driver.class, driverLoader);
        for (Driver driver : serviceLoader) {
            String name = driver.getClass().getName();
            if (driverMap.containsKey(name)) continue;
            DriverManager.registerDriver(new DriverShim(driver));
            driverMap.put(name, Boolean.TRUE);
        }

        // Custom driver
        if (StringUtils.isNotEmpty(driverClassName) && !driverMap.containsKey(driverClassName)) {
            Driver customDriver = (Driver) Class.forName(driverClassName, true, driverLoader).newInstance();
            DriverManager.registerDriver(new DriverShim(customDriver));
            driverMap.put(driverClassName, Boolean.TRUE);
        }
    }

    private static URLClassLoader getUrlClassLoader(File driverFile, String driverPath) throws IOException {
        List<URL> urls = new ArrayList<>();
        if (driverPath.endsWith(".jar")) {
            urls.add(driverFile.toURI().toURL());
        } else if (driverFile.isDirectory()) {
            addJarFilesRecursively(driverFile, urls);
        } else {
            throw new RuntimeException("Unsupported driver path format: " + driverPath);
        }

        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    private static void addJarFilesRecursively(File directory, List<URL> urls) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addJarFilesRecursively(file, urls);
                } else if (file.getName().endsWith(".jar") && !file.getName().matches(".*(sources|javadoc).*")) {
                    urls.add(file.toURI().toURL());
                }
            }
        }
    }

}
