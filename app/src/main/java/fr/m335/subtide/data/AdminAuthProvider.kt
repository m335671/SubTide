package fr.m335.subtide.data

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Attaches admin credentials to a request bound for the admin `dj` endpoints. Confirmed against a
 * real deployment (`WWW-Authenticate: Basic realm="SUB/WAVE admin"` on `/stats`/`/listeners`) as
 * standard HTTP Basic Auth — the upstream default, not the custom query-param scheme
 * CLAUDE_CODE_PROMPT.md warned some instances might use. Kept behind this one interface so a
 * deployment that *does* need a different scheme is a single-file change.
 */
fun interface AdminAuthProvider {
    fun authenticate(request: Request): Request
}

class BasicAuthProvider(private val username: String, private val password: String) : AdminAuthProvider {
    override fun authenticate(request: Request): Request =
        request.newBuilder().header("Authorization", Credentials.basic(username, password)).build()
}

class AdminAuthInterceptor(private val provider: AdminAuthProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(provider.authenticate(chain.request()))
}
