package com.davidmedenjak.redditsample.auth.api.model;

import com.squareup.moshi.Json;

public class TokenResponse {

    @Json(name = "access_token")
    public String accessToken;

    @Json(name = "token_type")
    public String tokenType;

    @Json(name = "expires_in")
    public long expiresIn;

    @Json(name = "refresh_token")
    public String refreshToken;

    @Json(name = "scope")
    public String scope;
}
