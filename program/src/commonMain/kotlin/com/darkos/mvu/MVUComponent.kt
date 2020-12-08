package com.darkos.mvu

import com.darkos.mvu.model.MVUState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class MVUComponent<T : MVUState>(
    private val effectHandler: EffectHandler,
    private val reducer: Reducer<T>
) : Component<T> {

    private val _state by lazy { MutableStateFlow(createInitialState()) }
    val state: StateFlow<T>
        get() = _state

    protected val program: Program<T> by lazy {
        Program(
            initialState = createInitialState(),
            component = this,
            effectHandler = effectHandler,
            reducer = reducer
        )
    }

    abstract fun createInitialState(): T

    override fun render(state: T) {
        _state.value = state
    }

    fun start() {
        program.start()
    }

    fun clear() {
        program.clear()
    }
}