package com.darkos.mvu

import com.darkos.mvu.model.MVUState
import com.darkos.mvu.model.Message
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class MVUComponent<T : MVUState>(
    private val effectHandler: EffectHandler,
    private val reducer: Reducer<T>
) : Component<T> {

    var processState: ((T)->Unit)? = null

    private var isStarted = false

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
        if(isStarted.not()){
            isStarted = true
            program.start()
        }
    }

    fun clear() {
        program.clear()
        processState = null
    }

    fun accept(message: Message) {
        program.accept(message)
    }
}