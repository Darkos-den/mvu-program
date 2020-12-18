package com.darkos.mvu

import com.badoo.reaktive.utils.lock.Lock
import com.badoo.reaktive.utils.lock.synchronized
import com.darkos.mvu.model.*
import com.darkos.mvu.model.flow.FinalMessage
import com.darkos.mvu.model.flow.FlowEffect
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

@InternalCoroutinesApi
class Program<T : MVUState>(
    private val reducer: Reducer<T>,
    private val effectHandler: EffectHandler,
    private val component: Component<T>,
    initialState: T
) {
    private val jobs = SupervisorJob()
    private val scope = CoroutineScope(Background + jobs)
    private val lock = Lock()

    private inner class EffectJobPool {
        private var scopedJobs: HashMap<Any, Job> = hashMapOf()

        fun addScoped(key: Any, jobBuilder: () -> Job) {
            scopedJobs[key]?.cancel()
            scopedJobs[key] = jobBuilder.invoke().also {
                it.invokeOnCompletion {
                    scopedJobs.remove(key)
                }
            }
        }

        fun clear() {
            scopedJobs.clear()
        }
    }

    private val effectJobPool = EffectJobPool()

    private var state: T = initialState

    init {
        component.render(initialState)
    }

    private fun runEffect(effect: Effect) {
        if (effect is ScopedEffect) {
            effectJobPool.addScoped(effect.scope) {
                effect.run()
            }
        } else {
            effect.run()
        }
    }

    private fun Effect.run(): Job {
        return scope.launch {
            when (val effect = this@run) {
                is FlowEffect -> {
                    effectHandler.callAsFlow(effect).collect {
                        acceptAsync(it)
                        if (it is FinalMessage) {
                            cancel()
                        }
                    }
                }
                else -> {
                    acceptAsync(effectHandler.call(effect))
                }
            }
        }
    }

    fun start() {
        accept(ComponentInitialized)
    }

    fun clear() {
        effectJobPool.clear()
        jobs.cancel()
        lock.destroy()
    }

    fun accept(message: Message) {
        lock.synchronized {
            runReducerProcessing(message)
        }
    }

    private suspend fun acceptAsync(message: Message){
        withContext(Ui) {
            lock.synchronized {
                runReducerProcessing(message)
            }
        }
    }

    private fun runReducerProcessing(message: Message) {
        reducer.update(state, message).also {
            state = it.state
        }.also {
            component.render(state)
        }.effect.takeIf {
            it !is None
        }?.let {
            runEffect(it)
        }
    }
}