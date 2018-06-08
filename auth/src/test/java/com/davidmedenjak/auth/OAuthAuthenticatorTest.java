package com.davidmedenjak.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class OAuthAuthenticatorTest {
    private static final Account account = new Account("test", "test");
    private static final String tokenType = "bearer";

    private AccountManager am;

    private OAuthAuthenticator authenticator;
    private AuthService authService;
    private AccountAuthenticatorResponse response;

    @Before
    public void setUp() throws Exception {
        am = AccountManager.get(RuntimeEnvironment.application);

        response = mock(AccountAuthenticatorResponse.class);
        authService = mock(AuthService.class);

        authenticator = new OAuthAuthenticator(RuntimeEnvironment.application, authService);
    }

    @Test
    public void accessTokenReturnedImmediately()
            throws NetworkErrorException, AuthenticatorException, OperationCanceledException,
                    IOException {
        shadowOf(am).addAccount(account);
        final String accessToken = "access1";
        shadowOf(am).setAuthToken(account, tokenType, accessToken);

        // when
        Bundle result = getAuthTokenResponse();

        // then
        assertNotNull(result);
        assertEquals(accessToken, result.getString(AccountManager.KEY_AUTHTOKEN));
    }

    @Test
    public void errorOnInvalidRefreshToken()
            throws NetworkErrorException, AuthenticatorException, OperationCanceledException,
                    IOException {
        shadowOf(am).addAccount(account);
        shadowOf(am).setPassword(account, "invalid");

        withServiceResponse(callback -> callback.onError(new Throwable()));

        // when
        Bundle result = getAuthTokenResponse();

        // then
        assertNull(result);
        verify(response).onError(anyInt(), any());
    }

    @Test
    public void accessTokenReturnedAfterRefresh()
            throws NetworkErrorException, AuthenticatorException, OperationCanceledException,
                    IOException {
        shadowOf(am).addAccount(account);
        final String accessToken = "access1";
        shadowOf(am).setPassword(account, "refresh1");

        TokenPair response = new TokenPair(accessToken, "refresh2");
        withServiceResponse(callback -> callback.onAuthenticated(response));

        // when
        Bundle result = getAuthTokenResponse();

        // then
        assertNull(result);
        assertEquals(accessToken, am.blockingGetAuthToken(account, "bearer", true));
    }

    @Test
    public void multipleRequestsTriggerASingleRefresh()
            throws NetworkErrorException, AuthenticatorException, OperationCanceledException,
                    IOException {
        shadowOf(am).addAccount(account);
        final String accessToken = "access1";
        shadowOf(am).setPassword(account, "refresh1");

        AccountAuthenticatorResponse secondResponse = mock(AccountAuthenticatorResponse.class);

        TokenPair authResponse = new TokenPair(accessToken, "refresh2");

        final boolean[] firedSecond = {false};
        withServiceResponse(
                cb -> {
                    if (!firedSecond[0]) {
                        firedSecond[0] = true;
                        // second request "before api call finishes"
                        Bundle result = getAuthTokenResponse(secondResponse);
                    }

                    // return result
                    cb.onAuthenticated(authResponse);
                });

        // when
        Bundle result = getAuthTokenResponse(response);

        // then
        assertNull(result);
        verify(authService, times(1)).authenticate(anyString(), any());
        verify(response).onResult(argThat(new AuthResponseMatcher(accessToken)));
        verify(secondResponse).onResult(argThat(new AuthResponseMatcher(accessToken)));
    }

    private void withServiceResponse(Action1<AuthService.Callback> action) {
        Mockito.doAnswer(
                        invocation -> {
                            AuthService.Callback callback =
                                    (AuthService.Callback) invocation.getArguments()[1];
                            action.run(callback);
                            return null;
                        })
                .when(authService)
                .authenticate(anyString(), any(AuthService.Callback.class));
    }

    private Bundle getAuthTokenResponse() {
        return getAuthTokenResponse(response);
    }

    private Bundle getAuthTokenResponse(AccountAuthenticatorResponse response) {
        try {
            return authenticator.getAuthToken(response, account, "bearer", null);
        } catch (NetworkErrorException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
