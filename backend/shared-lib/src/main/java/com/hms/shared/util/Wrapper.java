package com.hms.shared.util;

public class Wrapper<T> {
    private T value;

    public Wrapper(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    /**
     * Applies the given function to the wrapped value and updates the wrapped value with the result.
     * @param function the function to apply to the wrapped value
     */
    public Wrapper<T> apply(WrapperFunction<T> function) {
        set(function.apply(get()));
        return this;
    }

    @FunctionalInterface
    public static interface WrapperFunction<T> {
        T apply(T value);
    }
}