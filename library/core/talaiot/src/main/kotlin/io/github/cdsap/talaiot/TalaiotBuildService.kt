package io.github.cdsap.talaiot

import io.github.cdsap.talaiot.entities.TaskLength
import io.github.cdsap.talaiot.entities.TaskMessageState
import io.github.cdsap.talaiot.publisher.TalaiotPublisher
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class TalaiotBuildService : BuildService<TalaiotBuildService.Params>, AutoCloseable,
    OperationCompletionListener {

    var start = 0L
    var configurationTime = 0L
    var configurationIsSet = false

    interface Params : BuildServiceParameters {
        val targeta: Property<TalaiotPublisher>
    }

    private val taskLengthList = mutableListOf<TaskLength>()

    init {
        start = System.currentTimeMillis()

    }

    override fun close() {

        parameters.targeta.get().publish(
            taskLengthList,
            start,
            configurationTime,
            System.currentTimeMillis(),
            taskLengthList.filter { it.state == TaskMessageState.FAILED }.isEmpty()
        )
    }


    override fun onFinish(p0: FinishEvent?) {

        println(p0.toString())
        if (!configurationIsSet) {
            configurationTime = System.currentTimeMillis() - start
            configurationIsSet = true
        }
        //   println("onfinish")
        val duration = p0?.result?.endTime!! - p0.result?.startTime!!
        val end = p0.result?.endTime!!
        val start = p0.result?.startTime!!
        val task = p0.descriptor?.name.toString()
        val state = p0.displayName.split(" ")[2]

        taskLengthList.add(
            taskLength(
                ms = duration, task = task, state = when (state) {
                    "UP-TO-DATE" -> TaskMessageState.UP_TO_DATE
                    "FROM-CACHE" -> TaskMessageState.FROM_CACHE
                    "NO-SOURCE" -> TaskMessageState.NO_SOURCE
                    "failed" -> TaskMessageState.FAILED
                    else -> TaskMessageState.EXECUTED
                }, rootNode = false, startMs = start, stopMs = end
            )
        )

    }
}

private fun taskLength(
    ms: Long, task: String, state: TaskMessageState, rootNode: Boolean, startMs: Long, stopMs: Long
): TaskLength = TaskLength(
    ms = ms,
    taskName = task,
    taskPath = task,
    state = state,
    rootNode = rootNode,
    module = getModule(task),
    startMs = startMs,
    stopMs = stopMs
)

private fun getModule(path: String): String {
    val module = path.split(":")

    return if (module.size > 2) module.toList().dropLast(1).joinToString(separator = ":")
    else "no_module"
}

