package dev.ostfalia.iotcam.network.oauth

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import com.auth0.android.jwt.JWT
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import net.openid.appauth.browser.BrowserWhitelist
import net.openid.appauth.browser.VersionedBrowserMatcher
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Authenticator @Inject constructor(@ApplicationContext var appContext: Context) {

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse(ConfigData.AUTH_URL), // authorization endpoint
        Uri.parse(ConfigData.ACCESS_TOKEN_URL) // token endpoint
    )

    private val authConfig = AppAuthConfiguration.Builder().setBrowserMatcher(
        BrowserWhitelist(VersionedBrowserMatcher.CHROME_CUSTOM_TAB)
    ).build();

    private val authState: AuthState = readAuthState()
    private var jwt: JWT? = null

    fun openAuthIntent(authorizationLauncher: ActivityResultLauncher<Intent>) {
        var authIntent = this.getAuthIntent()
        authorizationLauncher.launch(authIntent);
    }

    private fun getAuthorizationRequest(): AuthorizationRequest {
        val redirectUri = Uri.parse("dev.ostfalia.iotcam:/oauth2redirect")
        val builder = AuthorizationRequest.Builder(
            serviceConfig,
            ConfigData.CLIENT_ID,
            ResponseTypeValues.CODE,
            redirectUri
        )
        builder.setScopes("openid")

        return builder.build();
    }

    fun getAuthIntent(): Intent {
        val authRequest = getAuthorizationRequest();
        val authService = AuthorizationService(appContext);


        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun authorizationRefreshRequest()  {
        val authService = AuthorizationService(appContext);

        val tokenRequest = TokenRequest.Builder(serviceConfig, ConfigData.CLIENT_ID)
            .setRefreshToken(authState.refreshToken)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setScope("openid")
            .build()

        val tokenResponseCallback =
            TokenResponseCallback { resp, ex ->
                if (resp != null) {
                    print("REFRESH SUCCESS: $resp")

                    authState.update(resp, ex)
                    writeAuthState(authState)
                } else {
                    print("REFRESH ERROR: $ex")
                    //failed
                }
            }

        authService.performTokenRequest(tokenRequest, tokenResponseCallback)
    }

    fun handleAuthorizationResponse(intent: Intent) {
        val authorizationResponse: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val tokenExchangeRequest = authorizationResponse!!.createTokenExchangeRequest()

        AuthorizationService(appContext).performTokenRequest(tokenExchangeRequest) { response, exception ->
            authState.update(response, exception);
            writeAuthState(authState)
            if (exception != null) {
                var authState = AuthState()
            } else {
                if (response != null) {
                    Log.v("Tag", response.toString())
                }
            }
            println("ERROR AUTH: ${error?.message.toString()}")

            if (authState.idToken != null) {
                jwt = JWT(authState.idToken!!)
                jwt!!.getClaim("email")?.let { Log.v("JWT", it.asString()!!) }
            }
        }
    }

    fun handleAuthorizationResponse(intent: Intent, welcometextbox : TextView?) {
        val authorizationResponse: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val tokenExchangeRequest = authorizationResponse!!.createTokenExchangeRequest()

        AuthorizationService(appContext).performTokenRequest(tokenExchangeRequest) { response, exception ->
            authState.update(response, exception);
            writeAuthState(authState)
            if (exception != null) {
                var authState = AuthState()
            } else {
                if (response != null) {
                    Log.v("Tag", response.toString())
                }
            }
            println("ERROR AUTH: ${error?.message.toString()}")

            if (authState.idToken != null) {
                jwt = JWT(authState.idToken!!)
                jwt!!.getClaim("email")?.let {
                    welcometextbox?.text = it.asString()
                    Log.v("JWT", it.asString()!!)
                }
            }
        }
    }

    private fun readAuthState(): AuthState {
        val authPrefs: SharedPreferences = appContext.getSharedPreferences("auth", MODE_PRIVATE)
        val stateJson = authPrefs.getString("stateJson", null)
        if (stateJson != null) {
            var authState = AuthState.jsonDeserialize(stateJson)
            if (authState.idToken != null) {
                jwt = JWT(authState.idToken!!)
            }
            return authState
        } else {
            return AuthState()
        }
    }

    private fun writeAuthState(state: AuthState) {
        val authPrefs: SharedPreferences = appContext.getSharedPreferences("auth", MODE_PRIVATE)
        authPrefs.edit()
            .putString("stateJson", state.jsonSerializeString())
            .apply()
    }

    fun isAuthorized(): Boolean {
        return authState.isAuthorized;
    }

    fun getUserName(): String? {
        if (jwt?.getClaim("email") ?: null != null) {
            return jwt!!.getClaim("email").asString()
        } else {
            return null;
        }
    }

    fun isTokenExpired(): Boolean {
        val unixTime = System.currentTimeMillis()
        val tokenTime = authState.accessTokenExpirationTime!!
        var isExpired = true

        if (unixTime < tokenTime) isExpired = false
        println("TOKEN EXPIRATION: ${(tokenTime - unixTime)  / 1000 / 60}")

        return isExpired
    }

    fun getToken(): String? {
        return authState.accessToken;
    }
}