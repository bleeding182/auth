package com.davidmedenjak.redditsample.auth.api;

import com.davidmedenjak.redditsample.auth.api.model.TokenResponse;
import com.davidmedenjak.redditsample.auth.api.model.User;

import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RedditAuthApi {

    @FormUrlEncoded
    @POST("v1/access_token")
    Single<TokenResponse> authenticate(
            @Header("Authorization") String basicAuth,
            @Field("grant_type") String grantType,
            @Field("code") String code,
            @Field("redirect_uri") String redirectUri);

    @FormUrlEncoded
    @POST("v1/access_token")
    Single<TokenResponse> authenticate(
            @Header("Authorization") String basicAuth,
            @Field("grant_type") String grantType,
            @Field("refresh_token") String refreshToken);

    @GET("https://oauth.reddit.com/api/v1/me")
    Single<User> fetchMe(@Header("Authorization") String basicAuth);
}
