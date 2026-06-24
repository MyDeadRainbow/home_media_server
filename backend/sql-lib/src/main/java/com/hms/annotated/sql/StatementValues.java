package com.hms.annotated.sql;

import java.util.List;

public record StatementValues(String sql, List<Object> parameters) {
}
