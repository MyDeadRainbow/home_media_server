package com.hms.shared.util;

import java.util.ArrayList;
import java.util.List;

public abstract class Node<T, R> {
    private final T value;
    private final List<R> children;

    public Node(T value) {
        this(value, new ArrayList<>());
    }

    public Node(T value, List<R> children) {
        this.value = value;
        this.children = children;
    }

    public T getValue() {
        return value;
    }

    public List<R> getChildren() {
        return children;
    }

    public void addChild(R child) {
        this.children.add(child);
    }

    public void removeChild(R child) {
        this.children.remove(child);
    }

    public void addChildren(List<R> children) {
        this.children.addAll(children);
    }

    public void removeChildren(List<R> children) {
        this.children.removeAll(children);
    }
}
