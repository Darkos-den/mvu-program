package com.darkos.mvu.component

import com.darkos.mvu.model.MVUState

interface ProgramComponent<T : MVUState> {
    fun createInitialState(): T
    fun applyStateListener(block: (T) -> Unit)
    fun start()
}