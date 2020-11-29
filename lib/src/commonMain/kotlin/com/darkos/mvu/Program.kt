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
    private val msgQueue = ArrayDeque<Message>()

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

    private val channel = Channel<StateMessageData<T>>()
    private var state: T = initialState

    init {
        component.render(initialState)
    }

    private fun loop() {
        if (msgQueue.isNotEmpty()) {
            processMessage()
        }
    }

    private fun processMessage() {
        StateMessageData(
            state = state,
            message = msgQueue.first()
        ).let {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                channel.send(it)
            }
        }
    }

    private val job = CoroutineScope(Dispatchers.Main.immediate).launch {
        channel.consumeAsFlow().collect {
            component.render(it.state)

            state = it.state

            msgQueue.let { messages ->
                if (messages.isNotEmpty()) {
                    messages.removeFirst()
                }
            }

            reducer.update(it.state, it.message).takeIf {
                it.effect !is None
            }?.let {
                runEffect(it)
            }
        }
    }

    private fun runEffect(stateCmdData: StateCmdData<T>) {
        CoroutineScope(Background).launch {
            when (val effect = stateCmdData.effect) {
                is FlowEffect -> {
                    effectHandler.call(effect).collect {
                        processMessage(it)
                        if (it.isFinal) {
                            cancel()
                        }
                    }
                }
                else -> {
                    processMessage(effectHandler.call(effect))
                }
            }
        }.let { job ->
            stateCmdData.effect.let {
                if (it is ScopedEffect) {
                    effectJobPool.addScoped(it.scope, job)
                } else {
                    effectJobPool.add(job)
                }
            }
        }
    }

    private fun processMessage(message: Message) {
        when (message) {
            is Idle -> Unit
            else -> msgQueue.addLast(message)
        }
        loop()
    }

    fun start() {
        accept(ComponentInitialized)
    }

    fun clear() {
        effectJobPool.clear()
        job.cancel()
        channel.cancel()
        msgQueue.clear()
    }

    fun accept(message: Message) {
        msgQueue.addLast(message)
        if (msgQueue.isNotEmpty()) {
            processMessage()
        }
    }
}