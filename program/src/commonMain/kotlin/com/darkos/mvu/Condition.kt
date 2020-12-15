package com.darkos.mvu

interface Condition {

    fun await(timeoutNanos: Long = -1L)

    fun signal()

    fun destroy()
}