package com.davidmedenjak.redditsample.auth.api.model;

import com.squareup.moshi.Json;

public class User {
    @Json(name = "name")
    public String name;

    @Json(name = "created")
    public long createdAt;

    @Json(name = "comment_karma")
    public long commentKarma;

    @Json(name = "link_karma")
    public long linkKarma;
}
