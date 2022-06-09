package io.github.cdsap.talaiot.publisher

import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.entities.TaskLength
import io.github.cdsap.talaiot.filter.BuildFilterProcessor
import io.github.cdsap.talaiot.filter.TaskFilterProcessor

/**
 * Implementation of TalaiotPublisher.
 * It will retrieve all the metrics trough the MetricsProvider and the Publishers defined in the configuration
 * trough the PublisherProvider.
 * At the publishing phase it will aggregate the data of in a TaskMeasurementAggregated to publish the result
 * on each publisher retrieved.
 * Before the publishing phase we will apply the TaskFilterProcessor. Filtering doesn't apply to
 * the TaskDependencyGraphPublisher
 */
class TalaiotPublisherImpl(
    private val executionReport: ExecutionReport,
    private val publisherProvider: List<Publisher>,
    private val taskFilterProcessor: TaskFilterProcessor,
    private val buildFilterProcessor: BuildFilterProcessor
) : TalaiotPublisher, java.io.Serializable {

    override fun publish(
        taskLengthList: MutableList<TaskLength>,
        start: Long,
        configuraionMs: Long?,
        end: Long,
        success: Boolean
    ) {
        executionReport.tasks = taskLengthList.filter { taskFilterProcessor.taskLengthFilter(it) }
        executionReport.unfilteredTasks = taskLengthList
        executionReport.beginMs = start.toString()
        executionReport.endMs = end.toString()
        executionReport.success = success

        executionReport.durationMs = (end - start).toString()

        executionReport.configurationDurationMs = when {
            configuraionMs != null -> (configuraionMs - start).toString()
            else -> "undefined"
        }

        if (buildFilterProcessor.shouldPublishBuild(executionReport)) {
            publisherProvider.forEach {
                it.publish(executionReport)
            }
        }
    }
}
