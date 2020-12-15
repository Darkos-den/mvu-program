package com.darkos.mvu

import kotlinx.cinterop.*
import platform.posix.*

actual class Lock {

    private val arena = Arena()
    private val attr = arena.alloc<pthread_mutexattr_t>()
    private val mutex = arena.alloc<pthread_mutex_t>()

    init {
        pthread_mutexattr_init(attr.ptr)
        pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE)
        pthread_mutex_init(mutex.ptr, attr.ptr)
    }

    actual fun acquire() {
        pthread_mutex_lock(mutex.ptr)
    }

    actual fun release() {
        pthread_mutex_unlock(mutex.ptr)
    }

    actual fun destroy() {
        pthread_mutex_destroy(mutex.ptr)
        pthread_mutexattr_destroy(attr.ptr)
        arena.clear()
    }

    actual fun newCondition(): Condition = ConditionImpl(mutex.ptr)

    private class ConditionImpl(
        private val lockPtr: CPointer<pthread_mutex_t>
    ) : Condition {

        private val arena = Arena()
        private val cond = arena.alloc<pthread_cond_t>()

        init {
            pthread_cond_init(cond.ptr, null)
        }

        override fun await(timeoutNanos: Long) {
            if (timeoutNanos >= 0L) {
                memScoped {
                    // can't use monotonic time, pthread_condattr_setclock() nor clock_gettime(),
                    // iOS does not support it
                    // can't use NSRecursiveLock and NSCondition,
                    // it can't wait less then 1 second and lock can't create condition
                    val tv: timeval = alloc { gettimeofday(ptr, null) }
                    val ts: timespec = alloc()
                    ts.tv_sec = tv.tv_sec
                    ts.tv_nsec = (tv.tv_usec * NANOS_IN_MICRO).convert()
                    ts += timeoutNanos
                    pthread_cond_timedwait(cond.ptr, lockPtr, ts.ptr)
                }
            } else {
                pthread_cond_wait(cond.ptr, lockPtr)
            }
        }

        override fun signal() {
            pthread_cond_broadcast(cond.ptr)
        }

        override fun destroy() {
            pthread_cond_destroy(cond.ptr)
            arena.clear()
        }

        private companion object {
            private operator fun timespec.plusAssign(nanos: Long) {
                tv_sec += (nanos / NANOS_IN_SECOND).convert<__darwin_time_t>()
                tv_nsec += (nanos % NANOS_IN_SECOND).convert<__darwin_time_t>()
                if (tv_nsec >= NANOS_IN_SECOND) {
                    tv_sec += 1
                    tv_nsec -= NANOS_IN_SECOND.convert<__darwin_time_t>()
                }
            }
        }
    }
}