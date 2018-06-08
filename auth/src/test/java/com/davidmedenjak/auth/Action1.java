package com.davidmedenjak.auth;

@FunctionalInterface
public interface Action1<T> {
    void run(T object);
}
