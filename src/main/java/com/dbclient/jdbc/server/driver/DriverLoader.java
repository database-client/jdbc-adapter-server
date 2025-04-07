package com.dbclient.jdbc.server.driver;

import com.dbclient.jdbc.server.dto.ConnectDTO;
import com.dbclient.jdbc.server.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.*;

@Slf4j
public abstract class DriverLoader {

    private static final Map<String, Boolean> loaderMap = new HashMap<>();
    private static final Map<String, Boolean> driverMap = new HashMap<>();


    @SneakyThrows
    public static void loadDriverByDTO(ConnectDTO connectDTO) {
        String path = connectDTO.getDriverPath();
        String driver = connectDTO.getDriver();
        if (StringUtils.isNotEmpty(path)) {
            loaderMap.computeIfAbsent(path, (k) -> {
                DriverLoader.loadDriver(connectDTO.getDriverPath(), driver);
                return true;
            });
        }
        try {
            if (StringUtils.isNotEmpty(driver)) {
                Class.forName(driver);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
            try {
                String fileName = FilenameUtils.getBaseName(driverFile.getName());
                File tempDir = new File(System.getProperty("user.home"), ".dbclient/drivers/decompress/" + fileName);
                if (!tempDir.exists())
                    tempDir.mkdirs();
                decompressArchive(driverFile, tempDir);
                addJarFilesRecursively(tempDir, urls);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException("Failed to load driver. Error: " + e.getMessage());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    @SneakyThrows
    private static void decompressArchive(File archiveFile, File destDir) {
        InputStream fi = Files.newInputStream(archiveFile.toPath());
        BufferedInputStream bi = new BufferedInputStream(fi);
        ArchiveInputStream<ArchiveEntry> i = new ArchiveStreamFactory().createArchiveInputStream(bi);
        ArchiveEntry entry;
        while ((entry = i.getNextEntry()) != null) {
            File f = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
                if (!f.exists()) f.mkdirs();
            } else if (isValidJar(f.getName()) || LibraryUtils.isLibraryFile(f.getName())) {
                IOUtils.copy(i, Files.newOutputStream(Paths.get(destDir.getAbsolutePath(), f.getName())));
            } else {
                IOUtils.copy(i, Files.newOutputStream(f.toPath()));
            }
        }
    }

    private static void addJarFilesRecursively(File directory, List<URL> urls) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addJarFilesRecursively(file, urls);
                } else if (isValidJar(file.getName())) {
                    urls.add(file.toURI().toURL());
                } else if (LibraryUtils.isLibraryFile(file.getName())) {
                    Path targetPath = LibraryUtils.getLibraryPath(file.getName());
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean isValidJar(String fileName) {
        return fileName.endsWith(".jar") && !fileName.matches(".*(sources|javadoc).*");
    }

}
