package com.davidmedenjak.auth.manager;

import android.os.Bundle;

public final class AccountData {
    public static final AccountData EMPTY = new AccountData();

    final Bundle bundle = new Bundle();

    private AccountData() {}

    public static AccountData with(String key, String value) {
        return new AccountData().and(key, value);
    }

    public AccountData and(String key, String value) {
        bundle.putString(key, value);
        return this;
    }
}
