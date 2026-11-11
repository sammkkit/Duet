package com.samkit.duet.core.camera

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal val DirectExecutor = Executor { it.run() }

/** Suspends until this future finishes, instead of registering a callback. Cancelling the coroutine cancels the future. */
internal suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (e: ExecutionException) {
                    continuation.resumeWithException(e.cause ?: e)
                }
            },
            DirectExecutor,
        )
        continuation.invokeOnCancellation { cancel(false) }
    }
