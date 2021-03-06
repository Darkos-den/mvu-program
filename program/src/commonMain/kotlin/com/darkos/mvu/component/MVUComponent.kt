package com.darkos.mvu.component

import com.darkos.mvu.Component
import com.darkos.mvu.EffectHandler
import com.darkos.mvu.Program
import com.darkos.mvu.Reducer
import com.darkos.mvu.model.MVUState
import com.darkos.mvu.model.Message
import com.darkos.mvu.model.RestoreState
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(InternalCoroutinesApi::class)
abstract class MVUComponent<T : MVUState>(
    private val effectHandler: EffectHandler,
    private val reducer: Reducer<T>
) : Component<T>, ProgramComponent<T> {

    private var processState: ((T) -> Unit)? = null
    private var isStarted = false
    private val program: Program<T> by lazy {
        Program(
            initialState = createInitialState(),
            component = this,
            effectHandler = effectHandler,
            reducer = reducer
        )
    }

    abstract override fun createInitialState(): T

    override fun render(state: T) {
        processState?.invoke(state)
    }

    override fun start() {
        if (isStarted.not()) {
            isStarted = true
            program.start()
        }
    }

    override fun restore(state: T) {
        accept(RestoreState(state))
    }

    override fun applyStateListener(block: (T) -> Unit) {
        processState = block
    }

    override fun clearStateListener() {
        processState = null
    }

    override fun clear() {
        clearStateListener()
        program.clear()
    }

    fun accept(message: Message) {
        program.accept(message)
    }
}