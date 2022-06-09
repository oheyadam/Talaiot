package io.github.cdsap.talaiot.publisher.influxdb

import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.logger.LogTracker
import io.github.cdsap.talaiot.metrics.DefaultBuildMetricsProvider
import io.github.cdsap.talaiot.metrics.DefaultTaskDataProvider
import io.github.cdsap.talaiot.publisher.Publisher
import okhttp3.OkHttpClient
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBException
import org.influxdb.InfluxDBFactory
import org.influxdb.InfluxDBIOException
import org.influxdb.dto.BatchPoints
import org.influxdb.dto.Point
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val TIMEOUT_SEC = 10L

/**
 * Publisher using InfluxDb and LineProtocol format to send the metrics
 */
class InfluxDbPublisher(
    /**
     * General configuration for the publisher
     */
    private val influxDbPublisherConfiguration: InfluxDbPublisherConfiguration,
    /**
     * LogTracker to print in console depending on the Mode
     */
    private val logTracker: LogTracker
) : Publisher, java.io.Serializable {

    private val TAG = "InfluxDbPublisher"

    override fun publish(report: ExecutionReport) {
        //  val executor = Executors.newSingleThreadExecutor()
        if (influxDbPublisherConfiguration.url.isEmpty() ||
            influxDbPublisherConfiguration.dbName.isEmpty() ||
            influxDbPublisherConfiguration.taskMetricName.isEmpty() ||
            influxDbPublisherConfiguration.buildMetricName.isEmpty()
        ) {
            logTracker.error(
                "InfluxDbPublisher not executed. Configuration requires url, dbName, taskMetricName and buildMetricName: \n" +
                    "influxDbPublisher {\n" +
                    "            dbName = \"tracking\"\n" +
                    "            url = \"http://localhost:8086\"\n" +
                    "            buildMetricName = \"build\"\n" +
                    "            taskMetricName = \"task\"\n" +
                    "}\n" +
                    "Please update your configuration"
            )
            return
        }

        try {
            val executor = Executors.newSingleThreadExecutor()
            val pointsBuilder = BatchPoints.builder()
                // See https://github.com/influxdata/influxdb-java/issues/373
                .retentionPolicy(influxDbPublisherConfiguration.retentionPolicyConfiguration.name)

            if (influxDbPublisherConfiguration.publishTaskMetrics) {
                val measurements = createTaskPoints(report)
                if (!measurements.isNullOrEmpty()) {
                    pointsBuilder.points(measurements)
                }
            }

            if (influxDbPublisherConfiguration.publishBuildMetrics) {
                val buildMeasurement = createBuildPoint(report)
                pointsBuilder.point(buildMeasurement)
            }
            //    executor.execute {
            logTracker.log(TAG, "================")
            logTracker.log(TAG, "InfluxDbPublisher")
            logTracker.log(
                TAG,
                "publishBuildMetrics: ${influxDbPublisherConfiguration.publishBuildMetrics}"
            )
            logTracker.log(
                TAG,
                "publishTaskMetrics: ${influxDbPublisherConfiguration.publishTaskMetrics}"
            )
            logTracker.log(TAG, "================")

            try {
                val _db = createDb()
                val points = pointsBuilder.build()
                logTracker.log(TAG, "Sending points to InfluxDb server $points")
                _db.write(points)
            } catch (e: Exception) {
                logTracker.log(TAG, "InfluxDbPublisher-Error-Executor Runnable: ${e.message}")
            }
            //       }
        } catch (e: Exception) {
            println("inakii     ${e.message}")
            logTracker.log(TAG, "InfluxDbPublisher-Error ${e.stackTrace}")
            when (e) {
                is InfluxDBIOException -> {
                    logTracker.log(TAG, "InfluxDbPublisher-Error-InfluxDBIOException: ${e.message}")
                }
                is InfluxDBException -> {
                    logTracker.log(TAG, "InfluxDbPublisher-Error-InfluxDBException: ${e.message}")
                }
                else -> {
                    logTracker.log(TAG, "InfluxDbPublisher-Error-Exception: ${e.message}")
                }
            }
        }
    }

    private fun createTaskPoints(report: ExecutionReport): List<Point>? {
        return report.tasks?.map { task ->
            val tagFieldProvider = TagFieldProvider(
                influxDbPublisherConfiguration.taskTags,
                DefaultTaskDataProvider(task, report),
                report.customProperties.taskProperties
            )
            Point.measurement(influxDbPublisherConfiguration.taskMetricName)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag(tagFieldProvider.tags())
                .fields(tagFieldProvider.fields())
                .build()
        }
    }

    private fun createBuildPoint(report: ExecutionReport): Point {
        val tagFieldProvider = TagFieldProvider(
            influxDbPublisherConfiguration.buildTags,
            DefaultBuildMetricsProvider(report),
            report.customProperties.buildProperties
        )
        return Point.measurement(influxDbPublisherConfiguration.buildMetricName)
            .time(report.endMs?.toLong() ?: System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .tag(tagFieldProvider.tags())
            .fields(tagFieldProvider.fields())
            .build()
    }

    private fun createDb(): InfluxDB {
        println("ianaki3")
        val okHttpBuilder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        val user = influxDbPublisherConfiguration.username
        println("ianaki4")
        val password = influxDbPublisherConfiguration.password
        println("ianaki5")

        val url = influxDbPublisherConfiguration.url
        val dbName = influxDbPublisherConfiguration.dbName
        val retentionPolicyConfiguration =
            influxDbPublisherConfiguration.retentionPolicyConfiguration
        println("ianaki6")

        val influxDb = if (user.isNotEmpty() && password.isNotEmpty()) {
            InfluxDBFactory.connect(url, user, password, okHttpBuilder)
        } else {
            InfluxDBFactory.connect(url, okHttpBuilder)
        }
        influxDb.setLogLevel(InfluxDB.LogLevel.BASIC)

        val rpName = retentionPolicyConfiguration.name
        println("ianaki7")

        if (!influxDb.databaseExists(dbName)) {
            logTracker.log(TAG, "Creating db $dbName")
            println("ianaki8a")
            println(dbName)
            try {
                influxDb.createDatabase(dbName)
            } catch (e: Exception) {
                println("xx")
                println(e.message)
            }
            println("ianaki8")

            val duration = retentionPolicyConfiguration.duration
            val shardDuration = retentionPolicyConfiguration.shardDuration
            val replicationFactor = retentionPolicyConfiguration.replicationFactor
            val isDefault = retentionPolicyConfiguration.isDefault

            influxDb.createRetentionPolicy(
                rpName,
                dbName,
                duration,
                shardDuration,
                replicationFactor,
                isDefault
            )
        }

        influxDb.setDatabase(dbName)
        influxDb.setRetentionPolicy(rpName)
        influxDb.enableBatch()
        influxDb.enableGzip()
        println("ianaki4")
        return influxDb
    }
}
