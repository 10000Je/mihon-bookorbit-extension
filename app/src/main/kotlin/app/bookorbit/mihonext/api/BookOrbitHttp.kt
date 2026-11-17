package app.bookorbit.mihonext.api

import app.bookorbit.mihonext.settings.Connection
import app.bookorbit.mihonext.settings.ServerUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Both API calls and host image requests pass through the same scoped client. */
class BookOrbitHttp(private val connection: () -> Connection) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val config = connection()
        val request = chain.request()
        val root = ServerUrl.parse(config.server)
        val url = request.url
        if (url.scheme != root.scheme || url.host != root.host || url.port != root.port ||
            !url.encodedPath.startsWith(root.encodedPath + "api/v1/") ||
            url.fragment != "bo=${config.binding}") {
            throw ApiFailure(FailureKind.CONFIGURATION, "이미지 또는 요청이 현재 슬롯 연결에 속하지 않습니다.")
        }
        if (config.apiKey.isBlank() || config.apiKey.any { it <= ' ' || it >= '\u007f' }) throw ApiFailure.configuration()
        val authenticated = request.newBuilder()
            .url(url.newBuilder().fragment(null).build())
            .removeHeader("Cookie").removeHeader("X-Api-Key")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("User-Agent", "BookOrbitIndependent/1.6.1")
            .build()
        var attempt = 0
        while (true) {
            if (chain.call().isCanceled()) throw IOException("요청이 취소되었습니다.")
            val response = try {
                chain.proceed(authenticated)
            } catch (_: SocketTimeoutException) {
                throw ApiFailure(FailureKind.TIMEOUT, "서버 응답 시간이 초과되었습니다.")
            } catch (e: ApiFailure) {
                throw e
            } catch (_: IOException) {
                throw ApiFailure(FailureKind.CONNECTION, "서버에 연결할 수 없습니다. 네트워크와 인증서를 확인하세요.")
            }
            if (response.isSuccessful || response.code == 304) return response
            val wait = retryDelay(response.code, response.header("Retry-After"), attempt)
            val code = response.code
            response.close()
            if (wait == null) throw ApiFailure.http(code)
            attempt++
            var remaining = wait
            while (remaining > 0) {
                if (chain.call().isCanceled()) throw IOException("요청이 취소되었습니다.")
                val step = minOf(remaining, 100L)
                try { Thread.sleep(step) } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("요청이 취소되었습니다.")
                }
                remaining -= step
            }
        }
    }

    companion object {
        private val dispatcher = Dispatcher().apply { maxRequests = 4; maxRequestsPerHost = 4 }

        fun client(connection: () -> Connection): OkHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .cookieJar(CookieJar.NO_COOKIES)
            .addInterceptor(BookOrbitHttp(connection)).build()

        fun retryDelay(code: Int, retryAfter: String?, attempt: Int, now: Long = System.currentTimeMillis()): Long? {
            if (code !in setOf(429, 502, 503, 504) || attempt >= 2) return null
            val header = retryAfter?.let { value ->
                value.toLongOrNull()?.takeIf { it >= 0 }?.let { if (it > 5) 6000L else it * 1000 }
                    ?: runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli() - now }.getOrNull()
            }
            if (header != null && header > 5000) return null
            return header?.coerceAtLeast(0) ?: (500L shl attempt)
        }
    }
}

suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val failure = if (e is ApiFailure) e else ApiFailure(FailureKind.CONNECTION, "요청이 완료되지 않았습니다.")
            if (continuation.isActive) continuation.resumeWithException(failure)
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

/** Keep cancellation attached until the entire JSON body has been consumed. */
suspend fun <T> Call.awaitParsed(parse: (Response) -> T): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e.safeFailure())
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                response.use {
                    if (!continuation.isActive) return
                    val result = parse(it)
                    if (continuation.isActive) continuation.resume(result)
                }
            } catch (e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e.safeFailure())
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resumeWithException(ApiFailure.protocol())
            }
        }
    })
}

private fun IOException.safeFailure(): ApiFailure = when (this) {
    is ApiFailure -> this
    is java.io.InterruptedIOException -> ApiFailure(FailureKind.TIMEOUT, "서버 응답 시간이 초과되었습니다.")
    else -> ApiFailure(FailureKind.CONNECTION, "서버 응답을 끝까지 받지 못했습니다.")
}
