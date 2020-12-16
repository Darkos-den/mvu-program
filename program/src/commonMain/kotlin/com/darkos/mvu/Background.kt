package com.darkos.mvu

import kotlinx.coroutines.CoroutineDispatcher

expect val Background: CoroutineDispatcher
expect val Ui: CoroutineDispatcher