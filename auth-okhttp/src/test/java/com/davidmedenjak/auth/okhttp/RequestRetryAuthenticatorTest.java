package com.davidmedenjak.auth.okhttp;

import com.davidmedenjak.auth.AccountAuthenticator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestRetryAuthenticatorTest {

    private String invalidAccessToken = "invalid";
    private String validAccessToken = "valid";

    private AccountAuthenticator accountAuthenticator;
    private RequestRetryAuthenticator requestRetryAuthenticator;
    private Response response;

    @Before
    public void before() throws IOException {
        accountAuthenticator = mock(AccountAuthenticator.class);
        requestRetryAuthenticator = new RequestRetryAuthenticator(accountAuthenticator);

        Request request = new Request.Builder().url("http://localhost/")
                .header("Authorization", "Bearer " + invalidAccessToken).build();
        response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("hi")
                .build();
    }

    @Test
    public void retryFailedRequestWithNewAuthToken() throws Exception {
        when(accountAuthenticator.getAccessToken()).thenAnswer(invocation -> invalidAccessToken);
        when(accountAuthenticator.getNewAccessToken(invalidAccessToken)).thenAnswer(invocation -> validAccessToken);

        Request request = requestRetryAuthenticator.authenticate(null, response);

        assertNotNull(request);
        verify(accountAuthenticator, times(1)).getNewAccessToken(invalidAccessToken);

        Assert.assertEquals("Bearer " + validAccessToken, request.header("Authorization"));
    }

    @Test
    public void stopRetryAfterFailedAttempt() throws Exception {
        when(accountAuthenticator.getAccessToken()).thenAnswer(invocation -> invalidAccessToken);
        when(accountAuthenticator.getNewAccessToken(invalidAccessToken)).thenAnswer(invocation -> validAccessToken);

        Response secondResponse = response.newBuilder()
                .priorResponse(response)
                .build();
        Request request = requestRetryAuthenticator.authenticate(null, secondResponse);

        assertNull(request);
    }

    @Test
    public void retryWithNoPriorAuth() throws Exception {
        when(accountAuthenticator.getAccessToken()).thenAnswer(invocation -> validAccessToken);

        Response unauthenticatedResponse = response.newBuilder()
                .request(response.request().newBuilder().removeHeader("Authorization").build())
                .build();
        Request request = requestRetryAuthenticator.authenticate(null, unauthenticatedResponse);

        assertNotNull(request);
        Assert.assertEquals("Bearer " + validAccessToken, request.header("Authorization"));
    }
}
