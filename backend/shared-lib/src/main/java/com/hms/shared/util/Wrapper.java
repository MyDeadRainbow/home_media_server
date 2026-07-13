package com.hms.shared.util;

public class Wrapper<T> {
    private T value;

    public Wrapper(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Applies the given consumer to the wrapped value and updates the wrapped value with the result.
     * @param consumer the consumer to apply to the wrapped value
     */
    public void apply(WrapperFunction<T> consumer) {
        setValue(consumer.apply(getValue()));
    }

    @FunctionalInterface
    public static interface WrapperFunction<T> {
        T apply(T wrapper);
    }
}