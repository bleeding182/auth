package com.davidmedenjak.auth;

@FunctionalInterface
public interface Action2<S, T> {
    void run(S obj1, T obj2);
}
