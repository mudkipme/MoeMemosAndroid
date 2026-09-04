package me.mudkip.moememos.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coalesces pushes for a memo identifier: every [schedule] restarts a delay and only the last
 * one runs [push]. [flush] pushes immediately, [cancel] drops the pending push.
 */
class DeferredPushScheduler(
    private val scope: CoroutineScope,
    private val delayMillis: Long,
    private val push: suspend (String) -> Unit
) {
    private val jobs = mutableMapOf<String, Job>()
    private val lock = Any()

    fun schedule(identifier: String) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            delay(delayMillis)
            push(identifier)
        }
        synchronized(lock) {
            jobs.put(identifier, job)?.cancel()
        }
        job.invokeOnCompletion {
            synchronized(lock) {
                if (jobs[identifier] === job) {
                    jobs.remove(identifier)
                }
            }
        }
        job.start()
    }

    fun cancel(identifier: String) {
        synchronized(lock) {
            jobs.remove(identifier)
        }?.cancel()
    }

    suspend fun flush(identifier: String) {
        cancel(identifier)
        push(identifier)
    }
}
