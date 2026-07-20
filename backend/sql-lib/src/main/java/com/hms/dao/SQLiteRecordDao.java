package com.hms.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteUpdateListener;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class SQLiteRecordDao<T extends SQLiteRecord> {

    public abstract String getDbPath();

    public abstract String getTableName();

    public abstract String toCreateTableStatement();

    public abstract PreparedStatementValue toInsertStatement(T record);

    public abstract PreparedStatementValue toUpdateStatement(T record);

    public abstract PreparedStatementValue toDeleteStatement(T record);

    public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(getTableName());
        if (conditions != null && !conditions.isEmpty()) {
            sb.append(" WHERE ");
            boolean first = true;
            for (String field : conditions.keySet()) {
                if (!first) {
                    sb.append(" AND ");
                }
                sb.append(field).append(" = ?");
                first = false;
            }
        }
        sb.append(";");
        return new PreparedStatementValue(sb.toString(), conditions.values().toArray());
    }

    public abstract T mapResultSetToRecord(ResultSet rs) throws SQLException;

    public abstract String getPrimaryKeyField();

    public abstract Object getPrimaryKeyValue(T record);

    public List<SQLiteRecordDao<?>> getDependecies() {
        return List.of();
    }

    public T get(Object primaryKeyValue)
            throws SQLException {
        List<T> results = select(Map.of(getPrimaryKeyField(), primaryKeyValue));
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    protected T getByRowId(long rowId) throws SQLException {
        List<T> results = select(Map.of("rowid", rowId));
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    public List<T> select(Map<String, Object> conditions)
            throws SQLException {
        ensureTableExists();
        try (var conn = Database.getConnection(getDbPath())) {
            PreparedStatementValue psValue = toSelectStatement(conditions);
            try (var stmt = conn.prepareStatement(psValue.statement())) {
                for (int i = 0; i < psValue.values().length; i++) {
                    stmt.setObject(i + 1, psValue.values()[i]);
                }
                try (var rs = stmt.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        T rowInstance = mapResultSetToRecord(rs);
                        results.add(rowInstance);
                    }
                    return results;
                }
            }
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void insert(T record) throws SQLException {
        ensureTableExists();
        if (get(getPrimaryKeyValue(record)) != null) {
            update(record);
            return;
        }
        PreparedStatementValue psValue = toInsertStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void update(T record) throws SQLException {
        ensureTableExists();
        if (get(getPrimaryKeyValue(record)) == null) {
            insert(record);
            return;
        }
        PreparedStatementValue psValue = toUpdateStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }

        
    }

    public void delete(T record) throws SQLException {
        ensureTableExists();
        PreparedStatementValue psValue = toDeleteStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    private Observable<T> observable = null;

    public Observable<T> listen() {
        if (observable == null) {
            observable = createObservable();
        }
        return observable;
    }

    private Observable<T> createObservable() {
        return Observable.<Producer<T>>create(emitter -> {
            SQLiteUpdateListener listener = new SQLiteUpdateListener() {

                @Override
                public void onUpdate(Type type, String database, String table, long rowId) {
                    if (!table.equals(getTableName())) {
                        return;
                    }
                    Maybe<T> completable = Maybe.fromCallable(() -> {
                        return getByRowId(rowId);

                    }).observeOn(Schedulers.io());

                    switch (type) {
                        case INSERT:
                        case UPDATE:
                            emitter.onNext(completable::blockingGet);
                            break;
                        case DELETE:
                            // Handle delete if necessary
                            break;
                    }
                }

            };

            AutoCloseable listenerRegistration = new SQLiteUpdateListenerCloseable(listener, getDbPath());
            emitter.setCancellable(listenerRegistration::close);
        }).observeOn(Schedulers.computation()).subscribeOn(Schedulers.computation()).map(p -> p.get());
    }

    public void ensureTableExists() throws SQLException {
        try (var conn = Database.getConnection(getDbPath());) {
            for (SQLiteRecordDao<?> dao : getDependecies()) {
                dao.ensureTableExists();
            }
            try (var stmt = conn.createStatement()) {
                stmt.execute(toCreateTableStatement());
            }
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }
}

class SQLiteUpdateListenerCloseable implements AutoCloseable {
    private final String dbPath;
    private final Connection connection;
    private final SQLiteUpdateListener listener;

    public SQLiteUpdateListenerCloseable(SQLiteUpdateListener listener, String dbPath) {
        this.listener = listener;
        this.dbPath = dbPath;
        try {
            this.connection = Database.getConnection(dbPath);
            connection.unwrap(SQLiteConnection.class).addUpdateListener(listener);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add update listener", e);
        }
    }

    @Override
    public void close() {
        try (Connection _ = connection) {
            connection.unwrap(SQLiteConnection.class).removeUpdateListener(listener);
        } catch (Exception e) {
            throw new RuntimeException("Failed to close connection", e);
        }
    }
}