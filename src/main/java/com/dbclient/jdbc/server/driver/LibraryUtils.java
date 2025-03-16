package com.dbclient.jdbc.server.driver;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LibraryUtils {

    public static Path getLibraryPath(String libraryName) {
        String javaLibraryPath = getJavaLibraryPath();
        File libraryDir = new File(javaLibraryPath);
        if (!libraryDir.exists()) libraryDir.mkdirs();
        return Paths.get(javaLibraryPath, libraryName);
    }

    public static String getJavaLibraryPath() {
        String libraryPath = System.getProperty("java.library.path");
        return libraryPath.split(isWindows() ? ";" : ":")[0];
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }


    public static boolean isLibraryFile(String fileName) {
        return fileName.endsWith(".dll") && isWindows();
    }

}

