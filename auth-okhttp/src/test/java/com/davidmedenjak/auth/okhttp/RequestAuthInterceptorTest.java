package com.davidmedenjak.auth.okhttp;

import com.davidmedenjak.auth.AccountAuthenticator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestAuthInterceptorTest {

    private AccountAuthenticator authenticator;
    private Interceptor interceptor;
    private Interceptor.Chain chain;

    @Before
    public void before() throws IOException {
        authenticator = mock(AccountAuthenticator.class);
        interceptor = new RequestAuthInterceptor(authenticator);

        Request request = new Request.Builder().url("http://localhost/").build();

        chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any()))
                .then(
                        invocation ->
                                new Response.Builder()
                                        .request((Request) invocation.getArguments()[0])
                                        .protocol(Protocol.HTTP_2)
                                        .code(200)
                                        .message("hi")
                                        .build());
    }

    @Test
    public void authHeaderGetsAdded() throws Exception {
        String accessToken = "valid";
        when(authenticator.getAccessToken()).thenAnswer(invocation -> accessToken);

        Response response = interceptor.intercept(chain);

        Assert.assertEquals("Bearer " + accessToken, response.request().header("Authorization"));
    }

    @Test
    public void emptyAuthHeaderIgnored() throws Exception {
        String accessToken = "";
        when(authenticator.getAccessToken()).thenAnswer(invocation -> accessToken);

        Response response = interceptor.intercept(chain);

        Assert.assertNull(response.request().header("Authorization"));
    }
}
