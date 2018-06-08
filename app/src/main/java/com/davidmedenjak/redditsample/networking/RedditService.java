package com.davidmedenjak.redditsample.networking;

import com.davidmedenjak.redditsample.networking.model.Comment;
import com.davidmedenjak.redditsample.networking.model.Content;
import com.davidmedenjak.redditsample.networking.model.Listing;
import com.davidmedenjak.redditsample.networking.model.Response;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RedditService {

    @GET("/user/{username}/comments")
    Observable<Response<Listing<Content<Comment>>>> fetchComments(
            @Path("username") String username);
}
