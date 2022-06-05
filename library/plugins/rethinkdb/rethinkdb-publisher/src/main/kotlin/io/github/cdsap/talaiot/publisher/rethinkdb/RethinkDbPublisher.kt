package io.github.cdsap.talaiot.publisher.rethinkdb

import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.logger.LogTracker
import io.github.cdsap.talaiot.metrics.DefaultBuildMetricsProvider
import io.github.cdsap.talaiot.metrics.DefaultTaskDataProvider
import io.github.cdsap.talaiot.publisher.Publisher
import java.net.URL
import java.util.concurrent.Executors

/**
 * Publisher using RethinkDb format to send the metrics
 */
class RethinkDbPublisher(
    /**
     * General configuration for the publisher
     */
    private val rethinkDbPublisherConfiguration: RethinkDbPublisherConfiguration,
    /**
     * LogTracker to print in console depending on the Mode
     */
    private val logTracker: LogTracker
) : Publisher, java.io.Serializable {

    private val TAG = "RethinkDbPublisher"


    override fun publish(report: ExecutionReport) {
        println("122112122")
        if (rethinkDbPublisherConfiguration.url.isEmpty() ||
            rethinkDbPublisherConfiguration.dbName.isEmpty() ||
            rethinkDbPublisherConfiguration.taskTableName.isEmpty() ||
            rethinkDbPublisherConfiguration.buildTableName.isEmpty()
        ) {
            error(
                "RethinkDbPublisher not executed. Configuration requires url, dbName, taskTableName and buildTableName: \n" +
                        "rethinkDbPublisher {\n" +
                        "            dbName = \"tracking\"\n" +
                        "            url = \"http://localhost:8086\"\n" +
                        "            buildTableName = \"build\"\n" +
                        "            taskTableName = \"task\"\n" +
                        "}\n" +
                        "Please update your configuration"
            )
        }
        println("23323232233232")
        val r = RethinkDB.r

        val executor = Executors.newSingleThreadExecutor()
        // executor.execute {
        println("11111")
        logTracker.log(TAG, "================")
        logTracker.log(TAG, "RethinkDbPublisher")
        logTracker.log(TAG, "publishBuildMetrics: ${rethinkDbPublisherConfiguration.publishBuildMetrics}")
        logTracker.log(TAG, "publishTaskMetrics: ${rethinkDbPublisherConfiguration.publishTaskMetrics}")
        logTracker.log(TAG, "================")
        println("2222")

        try {
            val url = URL(rethinkDbPublisherConfiguration.url)
            println("3333")

            val conn: Connection = if (rethinkDbPublisherConfiguration.username.isBlank() &&
                rethinkDbPublisherConfiguration.password.isBlank()
            ) {
                println("444444")

                r.connection()
                    .hostname(url.host)
                    .port(url.port)
                    .connect()
            } else {
                r.connection()
                    .hostname(url.host)
                    .port(url.port)
                    .user(rethinkDbPublisherConfiguration.username, rethinkDbPublisherConfiguration.password)
                    .connect()
            }
            println("555511111")

            checkDb(conn, rethinkDbPublisherConfiguration.dbName, r)
            println("66666511111")
            if (rethinkDbPublisherConfiguration.publishTaskMetrics) {
                println("77777555511111")
                val entries = createTaskEntries(report)
                println("899")
                if (entries.isNotEmpty()) {
                    println("1o000")
                    checkTable(
                        conn,
                        rethinkDbPublisherConfiguration.dbName,
                        rethinkDbPublisherConfiguration.taskTableName,
                        r
                    )
                    println("11111")
                    insertEntries(
                        conn,
                        rethinkDbPublisherConfiguration.dbName,
                        rethinkDbPublisherConfiguration.taskTableName,
                        entries,
                        r
                    )
                    println("33333323239")
                }
            }

            println("xxxxxx")
            println(rethinkDbPublisherConfiguration.publishBuildMetrics)
            if (rethinkDbPublisherConfiguration.publishBuildMetrics) {
                val entries = DefaultBuildMetricsProvider(report).get()
                println("12")
                if (entries != null && entries.isNotEmpty()) {
                    println("13")
                    checkTable(
                        conn,
                        rethinkDbPublisherConfiguration.dbName,
                        rethinkDbPublisherConfiguration.buildTableName,
                        r
                    )
                    println("14")
                    insertEntries(
                        conn,
                        rethinkDbPublisherConfiguration.dbName,
                        rethinkDbPublisherConfiguration.buildTableName,
                        entries,
                        r
                    )
                    println("15")
                }
            }
        } catch (e: Exception) {
            logTracker.error("RethinkDbPublisher- Error executing the Runnable: ${e.message}")
        }
        //   }
    }

    private fun insertEntries(
        conn: Connection,
        db: String,
        table: String,
        entries: Map<String, Any>?,
        r: RethinkDB,
    ) {
        r.db(db).table(table).insert(entries).run<Any>(conn)
    }

    private fun checkDb(conn: Connection, db: String, r: RethinkDB) {
        val exist = r.dbList().contains(db).run<Boolean>(conn)
        if (!exist) {
            r.dbCreate(db).run<Any>(conn)
        }
    }

    private fun checkTable(conn: Connection, db: String, table: String, r: RethinkDB) {
        println("xxx")
        val exist = r.db(db).tableList().contains(table).run<Boolean>(conn)
        println("22222")

        if (!exist) {
            println("4444x")
            try {
                println("x")
                val a = r.db(db).tableCreate(table).run<Any>(conn)
                println(a)

                println("3")
            } catch (e: Exception) {
                println(e.message)
            }
        }
        println("$444x")

    }

    private fun createTaskEntries(report: ExecutionReport): Map<String, Any> {
        val list = mutableMapOf<String, Any>()
        report.tasks?.forEach { task ->
            list.putAll(DefaultTaskDataProvider(task, report).get())
        }
        return list
    }
}
