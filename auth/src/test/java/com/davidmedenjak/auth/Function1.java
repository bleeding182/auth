package com.davidmedenjak.auth;

@FunctionalInterface
public interface Function1<T, R> {
    R run(T object);
}
