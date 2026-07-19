package com.hms.dao;

import java.sql.SQLException;

public interface Producer<T> {
    T get() throws SQLException;
}
