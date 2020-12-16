package com.darkos.mvu

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

actual val Background: CoroutineDispatcher
    get() = Dispatchers.IO

actual val Ui: CoroutineDispatcher
    get() = Dispatchers.Main