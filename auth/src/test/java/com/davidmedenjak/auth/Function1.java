package com.davidmedenjak.auth;

import java.io.IOException;

@FunctionalInterface
public interface Function1<T, R> {
    R run(T object) throws IOException, TokenRefreshError;
}
