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
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class OAuthAuthenticatorTest {
    private static final Account account = new Account("test", "test");
    private static final String tokenType = "bearer";

    private AccountManager am;

    private OAuthAuthenticator authenticator;
    private AuthCallback authCallback;
    private AccountAuthenticatorResponse response;

    @Before
    public void setUp() {
        am = AccountManager.get(RuntimeEnvironment.application);

        response = mock(AccountAuthenticatorResponse.class);
        authCallback = mock(AuthCallback.class);

        authenticator = new OAuthAuthenticator(RuntimeEnvironment.application, authCallback);
    }

    @Test
    public void accessTokenReturnedImmediately() {
        am.addAccountExplicitly(account, null, null);
        final String accessToken = "access1";
        am.setAuthToken(account, tokenType, accessToken);

        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNotNull(result);
        assertEquals(accessToken, result.getString(AccountManager.KEY_AUTHTOKEN));
    }

    @Test
    public void errorOnInvalidRefreshToken() throws IOException, TokenRefreshError {
        am.addAccountExplicitly(account, null, null);
        am.setPassword(account, "invalid");

        withServiceResponse(
                callback -> {
                    throw new UnknownHostException();
                });

        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNull(result);
        verify(response).onError(eq(AccountManager.ERROR_CODE_NETWORK_ERROR), any());
    }

    @Test
    public void errorOnNullPointerException() throws IOException, TokenRefreshError {
        am.addAccountExplicitly(account, null, null);
        am.setPassword(account, "invalid");

        Mockito.doAnswer(
                        invocation -> {
                            String refreshToken = (String) invocation.getArguments()[0];
                            throw new NullPointerException();
                        })
                .when(authCallback)
                .authenticate(anyString());
        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNull(result);
        verify(response).onError(eq(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION), any());
    }

    @Test
    public void noLoginIntentProvided() throws NetworkErrorException {
        Mockito.doAnswer(invocation -> null).when(authCallback).getLoginIntent();

        Bundle result = authenticator.addAccount(response, account.type, tokenType, null, null);
    }

    @Test
    public void accessTokenReturnedAfterRefresh()
            throws AuthenticatorException, OperationCanceledException, IOException,
                    TokenRefreshError {
        am.addAccountExplicitly(account, null, null);
        final String accessToken = "access1";
        am.setPassword(account, "refresh1");

        TokenPair response = new TokenPair(accessToken, "refresh2");
        withServiceResponse(callback -> response);

        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNull(result);
        assertEquals(accessToken, am.blockingGetAuthToken(account, "bearer", true));
    }

    @Test
    public void multipleRequestsTriggerASingleRefresh() throws IOException, TokenRefreshError {
        am.addAccountExplicitly(account, null, null);
        final String accessToken = "access1";
        am.setPassword(account, "refresh1");

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
                    return authResponse;
                });

        // when
        Bundle result = getAuthTokenWithResponse(response);

        // then
        assertNull(result);
        verify(authCallback, times(1)).authenticate(anyString());
        verify(response).onResult(argThat(new AuthResponseMatcher(accessToken)));
        verify(secondResponse).onResult(argThat(new AuthResponseMatcher(accessToken)));
    }

    @Test
    public void multipleUserRequestsTriggerRunConcurrently() throws IOException, TokenRefreshError {

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
            am.addAccountExplicitly(users[i], null, null);
            am.setPassword(users[i], refreshTokens[i]);
        }

        // when the callback is called we wait for 4 requests to be made before returning any result
        withServiceResponse(
                (refreshToken) -> {
                    int idx = refreshToken.equals(refreshTokens[0]) ? 0 : 1;

                    // request seconds for every account
                    getAuthTokenWithResponse(users[idx], secondResponses[idx]);

                    // return result
                    return new TokenPair(accessTokens[idx], refreshTokens[idx]);
                });

        Bundle[] results = new Bundle[2];
        for (int i = 0; i < 2; i++) {
            results[i] = getAuthTokenWithResponse(users[i], firstResponses[i]);
        }

        // there should be 2 api calls (2 accounts) for all 4 requests
        verify(authCallback, times(2)).authenticate(anyString());

        for (int i = 0; i < 2; i++) {
            // should all wait asynchronously, thus the result be null
            assertNull(results[i]);

            // each response should be called once with the right token
            verify(firstResponses[i]).onResult(argThat(new AuthResponseMatcher(accessTokens[i])));
            verify(secondResponses[i]).onResult(argThat(new AuthResponseMatcher(accessTokens[i])));
        }
    }

    @Test
    public void returnCustomError() throws IOException, TokenRefreshError {
        am.addAccountExplicitly(account, null, null);
        am.setPassword(account, "invalid");

        final int errCode = AccountManager.ERROR_CODE_BAD_AUTHENTICATION;
        final String errMessage = "unauthorized";

        withServiceResponse(
                callback -> {
                    throw new TokenRefreshError(errCode, errMessage);
                });

        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNull(result);
        verify(response).onError(errCode, errMessage);
    }

    @Test
    public void cancelWithNullRefreshToken() throws IOException, TokenRefreshError {
        am.addAccountExplicitly(account, null, null);

        // `null` password / refresh token
        am.setPassword(account, null);

        final int errCode = AccountManager.ERROR_CODE_CANCELED;

        withServiceResponse(
                callback -> {
                    throw new IllegalStateException("should not run");
                });

        // when
        Bundle result = getAuthTokenWithResponse();

        // then
        assertNull(result);
        verify(response).onError(eq(errCode), anyString());
    }

    private void withServiceResponse(Function0<TokenPair> action)
            throws TokenRefreshError, IOException {
        withServiceResponse((obj1) -> action.run());
    }

    private void withServiceResponse(Function1<String, TokenPair> action)
            throws TokenRefreshError, IOException {
        Mockito.doAnswer(
                        invocation -> {
                            String refreshToken = (String) invocation.getArguments()[0];
                            return action.run(refreshToken);
                        })
                .when(authCallback)
                .authenticate(anyString());
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
