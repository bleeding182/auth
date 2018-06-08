package com.davidmedenjak.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Build;
import android.os.Bundle;

import com.davidmedenjak.auth.manager.AccountData;
import com.davidmedenjak.auth.manager.OAuthAccountManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OAuthAccountManagerTest {

    private final Account account = new Account("dummy", "user");
    private final String refreshToken = "refreshToken";
    private final String accessToken = "accessToken";
    private final TokenPair tokens = new TokenPair(accessToken, refreshToken);

    private AccountManager am;
    private OAuthAccountManager accountManager;

    @Before
    public void setUp() throws Exception {
        am = AccountManager.get(RuntimeEnvironment.application);
        accountManager = new OAuthAccountManager(account.type, am);
    }

    @Test
    public void existingAccountAutomaticallyLoggedIn() {
        am.addAccountExplicitly(account, refreshToken, Bundle.EMPTY);

        final OAuthAccountManager newManager = new OAuthAccountManager(account.type, am);

        assertTrue(newManager.isLoggedIn());
    }

    @Test
    public void noAccount_isLoggedOut() {
        assertFalse("No account added - should be logged out", accountManager.isLoggedIn());
    }

    @Test
    public void noAccount_logUserIn() {
        accountManager.login(account.name, tokens, AccountData.EMPTY);

        assertTrue(accountManager.isLoggedIn());
        assertEquals(account, accountManager.getAccount());
    }

    @Test
    public void storeUserData() {
        AccountData accountData = AccountData.with("name", "John");

        accountManager.login(account.name, tokens, accountData);

        assertEquals("John", accountManager.getAccountData("name"));
    }

    @Test
    public void updateUserData() {
        AccountData accountData = AccountData.with("name", "John");
        accountManager.login(account.name, tokens, accountData);

        accountManager.setAccountData("name", "Joan");

        assertEquals("Joan", accountManager.getAccountData("name"));
    }

    @Test
    public void updateUserData_withAccountData() {
        AccountData accountData = AccountData.with("name", "John");
        accountManager.login(account.name, tokens, accountData);

        AccountData newData = AccountData.with("name", "Joan");
        accountManager.setAccountData(newData);

        assertEquals("Joan", accountManager.getAccountData("name"));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void logout_preLollipop() {
        accountManager.login(account.name, tokens, AccountData.EMPTY);

        accountManager.logout();

        assertFalse(accountManager.isLoggedIn());
    }

    @Test
    @Ignore("Robolectric seems not to implement this correctly")
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void logout() {
        accountManager.login(account.name, tokens, AccountData.EMPTY);

        accountManager.logout();

        assertFalse(accountManager.isLoggedIn());
    }

    @Test
    public void provideAccessToken() throws IOException {
        accountManager.login(account.name, tokens, AccountData.EMPTY);

        String accessToken = accountManager.getAccessToken();

        assertEquals(this.accessToken, accessToken);
    }

    @Test
    public void refreshAccessToken() throws IOException {
        accountManager.login(account.name, tokens, AccountData.EMPTY);

        String newAccessToken = accountManager.getNewAccessToken(accessToken);

        assertNotEquals(this.accessToken, newAccessToken);
    }



}
