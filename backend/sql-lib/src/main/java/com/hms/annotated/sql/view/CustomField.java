package com.hms.annotated.sql.view;

import java.lang.reflect.RecordComponent;

public @interface CustomField {
    String name();
    Class<?> type();
}
