package com.greendome.adhkar.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal object FirebaseContentStorage {
    private val storage get() = FirebaseStorage.getInstance()
    private val root get() = storage.reference.child(RemoteContentConfig.STORAGE_ROOT)
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun downloadText(relativePath: String): String {
        ensureAnonymousReadIfNeeded()
        val sdk = runCatching {
            root.child(relativePath.trimStart('/')).getBytes(MAX_BYTES).await()
                .toString(Charsets.UTF_8)
        }
        if (sdk.isSuccess) return sdk.getOrThrow().withoutBom()
        val http = runCatching { downloadViaPublicUrl(relativePath) }.getOrNull()
        if (!http.isNullOrBlank()) return http.withoutBom()
        throw sdk.exceptionOrNull()
            ?: IllegalStateException("تعذّر جلب الملف من السحابة")
    }

    suspend fun upload(relativePath: String, bytes: ByteArray, contentType: String) {
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        root.child(relativePath.trimStart('/')).putBytes(bytes, metadata).await()
    }

    suspend fun ensureAdminSignedIn(email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.email?.equals(email, ignoreCase = true) == true) return
        auth.signOut()
        auth.signInWithEmailAndPassword(email, password).await()
    }

    private suspend fun ensureAnonymousReadIfNeeded() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) return
        runCatching { auth.signInAnonymously().await() }
    }

    private fun downloadViaPublicUrl(relativePath: String): String {
        val url = RemoteContentConfig.publicDownloadUrl(relativePath)
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("تعذّر جلب الملف (${response.code})")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun String.withoutBom(): String =
        if (isNotEmpty() && this[0] == '\uFEFF') substring(1) else this

    private const val MAX_BYTES = 32L * 1024 * 1024
}
