// package com.hms.shared.dao;

// import java.sql.SQLException;

// public interface SQLiteRecord {
//     public String getDbPath();

//     public String getTableName();

//     public String toCreateTableStatement();

//     public String toInsertStatement();

//     public void insert() throws DBFileNotFoundException, GetConnectionException, SQLException;

//     public void update() throws DBFileNotFoundException, GetConnectionException, SQLException;

//     public default void ensureTableExists() throws DBFileNotFoundException, GetConnectionException, SQLException {
//         try (var conn = Database.getConnection(getDbPath());) {
//             try (var stmt = conn.createStatement()) {
//                 stmt.execute(toCreateTableStatement());
//             }
//         }
//     }
// }
