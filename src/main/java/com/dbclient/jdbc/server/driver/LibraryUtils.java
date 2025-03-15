package com.dbclient.jdbc.server.driver;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LibraryUtils {

    public static Path getLibraryPath(String libraryName) {
        String libraryPath = System.getProperty("java.library.path");
        String javaLibraryPath = libraryPath.split(isWindows() ? ";" : ":")[0];
        log.info("Library path: {}", javaLibraryPath);
        File libraryDir = new File(javaLibraryPath);
        if (!libraryDir.exists()) libraryDir.mkdirs();
        return Paths.get(javaLibraryPath, libraryName);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }


    public static boolean isLibraryFile(String fileName) {
        return fileName.endsWith(".dll") && isWindows();
    }

}

