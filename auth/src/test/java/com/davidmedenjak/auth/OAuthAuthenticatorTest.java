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
        Bundle result = getAuthTokenWithResponse();

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
        Bundle result = getAuthTokenWithResponse();

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
        Bundle result = getAuthTokenWithResponse();

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
                        Bundle result = getAuthTokenWithResponse(secondResponse);
                    }

                    // return result
                    cb.onAuthenticated(authResponse);
                });

        // when
        Bundle result = getAuthTokenWithResponse(response);

        // then
        assertNull(result);
        verify(authService, times(1)).authenticate(anyString(), any());
        verify(response).onResult(argThat(new AuthResponseMatcher(accessToken)));
        verify(secondResponse).onResult(argThat(new AuthResponseMatcher(accessToken)));
    }

    @Test
    public void multipleUserRequestsTriggerRunConcurrently()
            throws NetworkErrorException, AuthenticatorException, OperationCanceledException,
                    IOException {

        // given some complicated setup... simulate "concurrency" :/
        Account[] users =
                new Account[] {new Account("test1", "test"), new Account("test2", "test")};
        String[] accessTokens = new String[] {"access1", "access2"};
        String[] refreshTokens = new String[] {"refresh1", "refresh2"};

        AccountAuthenticatorResponse[] firstResponses =
                new AccountAuthenticatorResponse[] {
                    mock(AccountAuthenticatorResponse.class),
                    mock(AccountAuthenticatorResponse.class)
                };
        AccountAuthenticatorResponse[] secondResponses =
                new AccountAuthenticatorResponse[] {
                    mock(AccountAuthenticatorResponse.class),
                    mock(AccountAuthenticatorResponse.class)
                };

        for (int i = 0; i < 2; i++) {
            shadowOf(am).addAccount(users[i]);
            shadowOf(am).setPassword(users[i], refreshTokens[i]);
        }

        // when the callback is called we wait for 4 requests to be made before returning any result
        final AuthService.Callback[] callbacks = new AuthService.Callback[2];
        withServiceResponse(
                (refreshToken, callback) -> {
                    if(refreshToken.equals(refreshTokens[0])) {
                        // save callback until we finished requesting all 4 tokens
                        callbacks[0] = callback;
                        return;
                    } else {
                        callbacks[1] = callback;
                    }

                    // request seconds for every account
                    for (int i = 0; i < 2; i++) {
                        getAuthTokenWithResponse(users[i], secondResponses[i]);
                    }

                    // return result
                    for (int i = 0; i < 2; i++) {
                        callbacks[i].onAuthenticated(new TokenPair(accessTokens[i], refreshTokens[i]));
                    }
                });

        Bundle[] results = new Bundle[2];
        for (int i = 0; i < 2; i++) {
            results[i] = getAuthTokenWithResponse(users[i], firstResponses[i]);
        }

        // there should be 2 api calls (2 accounts) for all 4 requests
        verify(authService, times(2)).authenticate(anyString(), any());

        for (int i = 0; i < 2; i++) {
            // should all wait asynchronously, thus the result be null
            assertNull(results[i]);

            // each response should be called once with the right token
            verify(firstResponses[i]).onResult(argThat(new AuthResponseMatcher(accessTokens[i])));
            verify(secondResponses[i]).onResult(argThat(new AuthResponseMatcher(accessTokens[i])));
        }
    }

    private void withServiceResponse(Action1<AuthService.Callback> action) {
        withServiceResponse((obj1, obj2) -> action.run(obj2));
    }

    private void withServiceResponse(Action2<String, AuthService.Callback> action) {
        Mockito.doAnswer(
                        invocation -> {
                            String refreshToken = (String) invocation.getArguments()[0];
                            AuthService.Callback callback =
                                    (AuthService.Callback) invocation.getArguments()[1];
                            action.run(refreshToken, callback);
                            return null;
                        })
                .when(authService)
                .authenticate(anyString(), any(AuthService.Callback.class));
    }

    private Bundle getAuthTokenWithResponse() {
        return getAuthTokenWithResponse(response);
    }

    private Bundle getAuthTokenWithResponse(AccountAuthenticatorResponse response) {
        return getAuthTokenWithResponse(account, response);
    }

    private Bundle getAuthTokenWithResponse(
            Account account, AccountAuthenticatorResponse response) {
        try {
            return authenticator.getAuthToken(response, account, "bearer", null);
        } catch (NetworkErrorException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
