package com.darkos.mvu

import com.darkos.mvu.model.*
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

    inner class EffectJobPool{
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
    private val scope = CoroutineScope(Dispatchers.Default + acceptJob)

    private var state: T = initialState

    init {
        component.render(initialState)
    }

    private val job = CoroutineScope(Dispatchers.Main.immediate).launch {
        messages.consumeAsFlow().collect {
            reducer.update(state, it).also {
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

    private fun runEffect(effect: Effect) {
        CoroutineScope(Background).launch {
            when (effect) {
                is FlowEffect -> {
                    effectHandler.call(effect).collect {
                        accept(it)
                        if (it.isFinal) {
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
        job.cancel()
        messages.cancel()
        acceptJob.cancel()
    }

    fun accept(message: Message) {
        scope.launch {
            messages.send(message)
        }
    }
}