package com.hms.stream.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public enum Dao {
    INSTANCE;        

    private static final String DATA_PATH = "data"; // Change this to your database path

    // Add DAO methods here
    private static Connection tryConnection(String databasePath) throws Exception {

        //Check if db file exists
        File dbFile = new File(DATA_PATH + "/" + databasePath);
        if (!dbFile.exists()) {
            throw new DBFileNotFoundException("Database file not found: " + databasePath);
        }

        // Load the SQLite JDBC driver (you must have the .jar file in your classpath)
        Class.forName("org.sqlite.JDBC");
        // Establish a connection to the SQLite database
        String url = "jdbc:sqlite:" + DATA_PATH + "/" + databasePath; // Change this to your database path
        return DriverManager.getConnection(url);
    }

    public static boolean databaseExists(String databasePath) {
        File dbFile = new File(DATA_PATH + "/" + databasePath);
        return dbFile.exists();
    }

    public static Connection getConnection(String databasePath) {
        try {
            return tryConnection(databasePath); // Database created successfully
        } catch (DBFileNotFoundException e) {
            File dbFile = new File(DATA_PATH + "/" + databasePath);
            try {                
                dbFile.getParentFile().mkdirs(); // Ensure the parent directory exists
                return tryConnection(databasePath); // Database created successfully
            } catch (Exception ex) {
                ex.printStackTrace();
                return null; // Failed to create database
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Failed to create database
        }
    }
}
