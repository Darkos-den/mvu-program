package com.darkos.mvu

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Background: CoroutineDispatcher
    get() = Dispatchers.IO