package com.hms.shared.dao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public enum Database {
    INSTANCE;

    // Database folder
    private static final String DATA_PATH = "database";

    public static Connection getConnection(String databasePath) throws GetConnectionException, DBFileNotFoundException {
        try {
            // Check if db file exists
            Path dbFilePath = Path.of(DATA_PATH, databasePath);
            Files.createDirectories(dbFilePath.getParent());

            File dbFile = new File(DATA_PATH + "/" + databasePath);
            if (!dbFile.exists()) {
                if (!dbFile.createNewFile()) {
                    throw new DBFileNotFoundException("Failed to create database file: " + databasePath);
                }
            }

            // Load the SQLite JDBC driver (you must have the .jar file in your classpath)
            Class.forName("org.sqlite.JDBC");
            // Establish a connection to the SQLite database
            String url = "jdbc:sqlite:" + DATA_PATH + "/" + databasePath; // Change this to your database path
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new GetConnectionException("Failed to get database connection for: " + databasePath, e);
        }
    }

    public static boolean databaseExists(String databasePath) {
        File dbFile = new File(DATA_PATH + "/" + databasePath);
        return dbFile.exists();
    }

    public static boolean createDatabase(String databasePath) {
        try (Connection conn = getConnection(databasePath)) {
            return true; // Database created successfully
        } catch (DBFileNotFoundException e) {
            File dbFile = new File(DATA_PATH + "/" + databasePath);
            try {
                dbFile.getParentFile().mkdirs(); // Ensure the parent directory exists
                return dbFile.createNewFile(); // Database created successfully
            } catch (Exception ex) {
                ex.printStackTrace();
                return false; // Failed to create database
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Failed to create database
        }
    }
}
