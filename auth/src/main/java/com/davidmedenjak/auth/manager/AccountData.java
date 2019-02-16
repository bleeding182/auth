package com.davidmedenjak.auth.manager;

import android.os.Bundle;

/**
 * Used to store key value pairs with a users {@link android.accounts.Account}.
 *
 * <pre><code>
 * AccountData.with("key", "value")
 *     .and("otherKey", "text");
 * </code></pre>
 */
public final class AccountData {
    public static final AccountData EMPTY = new AccountData();

    final Bundle bundle = new Bundle();

    private AccountData() {}

    /**
     * Create a new AccountData object with
     *
     * @param key the key to use
     * @param value the value to store
     * @return the AccountData object
     */
    public static AccountData with(String key, String value) {
        return new AccountData().and(key, value);
    }

    /**
     * Add a new entry to the object
     *
     * @param key the key to add
     * @param value the value to add
     * @return the AccountData object
     */
    public AccountData and(String key, String value) {
        bundle.putString(key, value);
        return this;
    }
}
