package com.hms.dao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.sqlite.SQLiteConfig;

public enum Database {
    INSTANCE;

    // Database folder
    private static final String DATA_PATH = "database";
    private final SQLiteConfig config;
    private Connection connection = null;

    private Database() {
        this.config = new SQLiteConfig();

        // 1. Enable WAL mode to allow concurrent readers + 1 writer
        this.config.setJournalMode(SQLiteConfig.JournalMode.WAL);

        // 2. Set busy timeout (in milliseconds) so threads wait instead of throwing
        // immediate lock errors
        this.config.setBusyTimeout(5000);

        // 3. Optional: Enforce foreign keys if needed
        this.config.enforceForeignKeys(true);
    }

    private synchronized Connection createConnection(String databasePath)
            throws GetConnectionException, DBFileNotFoundException {
        try {
            // Check if db file exists
            Path dbFilePath = Path.of(DATA_PATH, databasePath);
            Files.createDirectories(dbFilePath.getParent());

            File dbFile = dbFilePath.toFile();
            if (!dbFile.exists()) {
                if (!dbFile.createNewFile()) {
                    throw new DBFileNotFoundException("Failed to create database file: " + databasePath);
                }
            }

            // Load the SQLite JDBC driver (you must have the .jar file in your classpath)
            Class.forName("org.sqlite.JDBC");
            // Establish a connection to the SQLite database
            String url = "jdbc:sqlite:" + DATA_PATH + "/" + databasePath; // Change this to your database path
            return new UnclosingConnectionWrapper(DriverManager.getConnection(url, config.toProperties()));
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new GetConnectionException("Failed to get database connection for: " + databasePath, e);
        }
    }

    public static synchronized Connection getConnection(String databasePath)
            throws GetConnectionException, DBFileNotFoundException {
        Database dbInstance = Database.INSTANCE;
        if (dbInstance.connection == null) {
            dbInstance.connection = dbInstance.createConnection(databasePath);
        }
        return dbInstance.connection;
    }

    public static synchronized Connection reader(String databasePath)
            throws GetConnectionException, DBFileNotFoundException {
        return getConnection(databasePath);
    }

    public static synchronized Connection writer(String databasePath)
            throws GetConnectionException, DBFileNotFoundException {
        return getConnection(databasePath);
    }

    public static boolean databaseExists(String databasePath) {
        File dbFile = new File(DATA_PATH + "/" + databasePath);
        return dbFile.exists();
    }

    public static boolean createDatabase(String databasePath) {
        try (Connection _ = getConnection(databasePath)) {
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