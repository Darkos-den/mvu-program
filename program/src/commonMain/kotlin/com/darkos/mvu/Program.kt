package com.darkos.mvu

import com.darkos.mvu.model.*
import com.darkos.mvu.model.flow.FinalMessage
import com.darkos.mvu.model.flow.FlowEffect
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow

class Program<T : MVUState>(
    private val reducer: Reducer<T>,
    private val effectHandler: EffectHandler,
    private val component: Component<T>,
    initialState: T
) {
    private val messages = Channel<Message>()

    private inner class EffectJobPool{
        private var jobs: List<Job> = emptyList()
        private var scopedJobs: HashMap<Any, Job> = hashMapOf()

        fun add(job: Job) {
            jobs = jobs + job
            job.invokeOnCompletion {
                jobs = jobs - job
            }
        }

        fun addScoped(key: Any, job: Job) {
            scopedJobs[key]?.cancel()
            scopedJobs[key] = job
            job.invokeOnCompletion {
                scopedJobs.remove(key)
            }
        }

        fun clear() {
            jobs.forEach {
                it.cancel()
            }
            scopedJobs.forEach {
                it.value.cancel()
            }
            jobs = emptyList()
            scopedJobs.clear()
        }
    }

    private val effectJobPool = EffectJobPool()
    private val acceptJob: Job = SupervisorJob()

    private var state: T = initialState

    init {
        component.render(initialState)
    }

    private fun runEffect(effect: Effect) {
        CoroutineScope(Background).launch {
            when (effect) {
                is FlowEffect -> {
                    effectHandler.callAsFlow(effect).collect {
                        accept(it)
                        if (it is FinalMessage) {
                            cancel()
                        }
                    }
                }
                else -> {
                    accept(effectHandler.call(effect))
                }
            }
        }.let { job ->
            if (effect is ScopedEffect) {
                effectJobPool.addScoped(effect.scope, job)
            } else {
                effectJobPool.add(job)
            }
        }
    }

    fun start() {
        accept(ComponentInitialized)
    }

    fun clear() {
        effectJobPool.clear()
        messages.cancel()
        acceptJob.cancel()
    }

    fun accept(message: Message) {
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