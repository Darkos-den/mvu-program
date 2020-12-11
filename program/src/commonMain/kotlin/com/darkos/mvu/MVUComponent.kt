package com.darkos.mvu

import com.darkos.mvu.model.MVUState
import com.darkos.mvu.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class MVUComponent<T : MVUState>(
    private val effectHandler: EffectHandler,
    private val reducer: Reducer<T>
) : Component<T> {

    var processState: ((T)->Unit)? = null

    private val program: Program<T> by lazy {
        Program(
            initialState = createInitialState(),
            component = this,
            effectHandler = effectHandler,
            reducer = reducer
        )
    }

    abstract fun createInitialState(): T

    override fun render(state: T) {
        processState?.invoke(state)
    }

    fun start() {
        program.start()
    }

    fun clear() {
        program.clear()
        processState = null
    }

    fun accept(message: Message) {
        program.accept(message)
    }
}