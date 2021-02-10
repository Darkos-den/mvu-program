package com.darkos.mvu.component

import com.darkos.mvu.model.MVUState

interface ProgramComponent<T : MVUState> {
    fun createInitialState(): T
    fun applyStateListener(block: (T) -> Unit)
    fun clearStateListener()
    fun start()
    fun clear()
    fun restore(state: T)
}