## Auth&mdash;Wrapper for the Authentication Framework

Provides a tested implementation for the Android Account Framework for you to get rid of SharedPreference based authentication.

### Why not SharedPreferences?

`SharedPreferences` work well and will be good enough for most projects, but there are 2 edge cases that don't always work as expected.

1. _Clear Data_ in the apps settings will remove any app data&mdash;including your OAuth tokens!
2. When triggering a token refresh after an access token has expired some APIs invalidate your refresh token (one time use). When refreshing the token at the same time from multiple threads you might receive 401 on your later requests, possibly logging out your user. Even if your API can handle multiple requests, this library will only ever send _one_ token refresh request at a time.

This library will help provide a stable user experience and may help you save time while testing since you can clean your app data without having to login again.

### Why a library?

Implementing the Account Manager Framework needs a lot of boilerplate and is a little confusing. To make it more accessible this library provides support for a basic OAuth use case.

Additionally this is intended as an example for you on how to implement your own Authenticator, as the internet is somewhat lacking on that.

### Features

As already mentioned above, this library implements (some of) the boilerplate needed to use the Authenticator Framework. The core of it is the `OAuthAuthenticator` that will be registered on the Android framework and supports single or multi-user applications.

For convenience this library includes a basic `OAuthAccountManager` that wraps the framework `AccountManager` and offers a simple single user experience (login, logout, isLoggedIn). This account manager when used with OkHttp also offers `RequestAuthInterceptor` and `RequestRetryAuthenticator` which will add the `Authorization` headers to your HTTP requests and refresh the access token when it becomes invalid.

There is currently no "wrapper" for multi-user support. If you need this make sure to check the above mentioned classes and continue from there!

### Usage / Setup

There is an example project in the `/app` folder that uses the Reddit API and shows how the library could be used. You have to add your own `CLIENT_ID` if you want to run the example! Take not of the _two_ Retrofit services used (one without authentication, the other one with auth headers) to prevent deadlocks when refreshing the token.

Sadly you will still need to add _some_ boilerplate as you can see next.

#### Gradle

The library is currently published on my bintray repository, so add the following to the end of your repositories in your root `build.gradle` file.

    repositories {
        maven {
            url "https://dl.bintray.com/bleeding182/bleeding182/"
        }
    }

Then include the packages

    implementation 'com.davidmedenjak.auth:auth:0.3.0'
    implementation 'com.davidmedenjak.auth:auth-okhttp:0.3.0'

_The library is currently [pre-release](https://semver.org/#spec-item-4). I will publish the artifacts on jcenter/maven central once I have some feedback and am happy with the API_

#### Basic Setup

You start by extending `AuthenticatorService` and return an implementation of `AuthCallback` that enables token refreshing. In your `AuthCallback` you should call your API and trade the refresh token for a new access token.

    public class RedditAuthenticatorService extends AuthenticatorService {
    
        private RedditAuthApi authApiService; // Retrofit service

        @Override
        public AuthCallback getAuthCallback() {
            return new RedditAuthCallback(this, authApiService);
        }
    }
    
Then you add the service to your manifest, registering the AccountAuthenticator.

    <service
        android:name=".auth.RedditAuthenticatorService"
        android:permission="android.permission.ACCOUNT_MANAGER">
        <intent-filter>
            <action android:name="android.accounts.AccountAuthenticator"/>
        </intent-filter>
        <meta-data
            android:name="android.accounts.AccountAuthenticator"
            android:resource="@xml/authenticator"/>
    </service>
    
Next you create the xml resource that contains your Authenticators configuration. An example for `res/xml/authenticator` can be seen here:

    <?xml version="1.0" encoding="utf-8"?>
    <account-authenticator
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:accountType="@string/account_type"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:smallIcon="@mipmap/ic_launcher"/>
        
If you want to use the `OAuthAccountManager` for convenience you should add your account type to your manifest as well. Alternatively you can supply it at runtime.

    <application>
        <meta-data android:name="oauth-account.type" android:value="@string/account_type" />
    </application>
        
And that's the basic setup! Be sure to check the example for more information.

#### OAuthAccountManager - OkHttp

The `auth-okhttp` package contains an interceptor and an authenticator for OkHttp that will add a `Authorization: Bearer {{accessToken}}` header to your api calls. To set it up you can use `OAuthAccountManager` that will fetch the token from the Account Authenticator, or alternatively implement the interface yourself.

    AccountAuthenticator authenticator = OAuthAccountManager.fromContext(this);
    OkHttpClient okHttpClient =
            new OkHttpClient.Builder()
                    .authenticator(new RequestRetryAuthenticator(authenticator))
                    .addInterceptor(new RequestAuthInterceptor(authenticator))
                    .build();
                    
### Contributing

This library will keep a `0.*` version until I am happy with the interface and can provide solid support for the most common OAuth use cases with multiple users. As such the current interfaces might change with any update.

Feedback about the usage and API is welcome. When you decide to add a feature request please think about whether this is a common use case that _should_ be handled by this library.

### License

MIT License applies, so please feel free to use what you need.
