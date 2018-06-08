## Auth&mdash;Wrapper for the Authentication Framework

Provides a tested implementation for the Android Account Framework for you to get rid of SharedPreference based authentication.

### Why not SharedPreferences?

`SharedPreferences` work well and will be good enough for most projects, but there are 2 edge cases that don't always work as expected.

1. _Clear Data_ will remove any app data&mdash;including your OAuth tokens!
2. When triggering a token refresh after an access token has expired some APIs invalidate your refresh token. When refreshing the token at the same time from multiple threads you might receive 401 on your later requests, possibly logging out your user. Even if your API can handle multiple requests, this library will only send _one_ request at a time.

This library will help provide a stable user experience and may help you save time while testing since you can clean your app data without having to login again.

### Why a library?

Implementing the Account Manager Framework needs a lot of boilerplate and is a little confusing. To make it more accessible this library provides support for a basic OAuth use case.

Additionally this should be an example for you on how to implement your own Authenticator, as the internet is somewhat lacking on that.

### Features

As already hinted above, this library implements (some of) the boilerplate needed to use the Authenticator Framework.
The library includes a basic `OAuthAccountManager` that can be used as a convenience for a single-user application.

_Note: Currently there is only support for single users but support for multiple is planned._

Further, when using OkHttp, you can use `RequestAuthInterceptor` and `RequestRetryAuthenticator` to authenticate your HTTP requests.

### Usage / Setup

There is an example project in the `/app` folder that uses the Reddit API that shows how this could be used. You have to add your own `CLIENT_ID` if you want to run the example!

Sadly there is still some boilerplate to include as you can see next.

#### Basic Setup

You start by extending `AuthenticatorService` and return an implementation of `AuthService` that enables token refreshing. In your `AuthService` you call your API and trade a refresh token for a new access token.

    public class RedditAuthenticatorService extends AuthenticatorService {
    
        @Override
        public AuthService getAuthenticatorService() {
            return new RedditAuthService(this, getApiService());
        }
    }
    
Then you add the service to your manifest.

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
    
After which you have to create a config file to set up your Authenticator. An example for `res/xml/authenticator` can be seen here:

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
        
And that's the basic setup!

#### OkHttp

The `auth-okhttp` package contains an interceptor and an authenticator for OkHttp that will add a `Authorization: Bearer {{accessToken}}` header to your api calls. To set it up you can use `OAuthAccountManager` that will fetch the token from the Account Authenticator!

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