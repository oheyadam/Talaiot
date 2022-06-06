package io.github.cdsap.talaiot.plugin

import io.github.cdsap.talaiot.logger.LogTrackerImpl
import io.github.cdsap.talaiot.provider.PublisherConfigurationProvider
import io.github.cdsap.talaiot.publisher.JsonPublisher
import io.github.cdsap.talaiot.publisher.OutputPublisher
// import io.github.cdsap.talaiot.publisher.JsonPublisher
// import io.github.cdsap.talaiot.publisher.OutputPublisher
import io.github.cdsap.talaiot.publisher.Publisher
import io.github.cdsap.talaiot.publisher.elasticsearch.ElasticSearchPublisher
// import io.github.cdsap.talaiot.publisher.graph.GraphPublisherFactoryImpl
// import io.github.cdsap.talaiot.publisher.graph.TaskDependencyGraphPublisher
import io.github.cdsap.talaiot.publisher.hybrid.HybridPublisher
import io.github.cdsap.talaiot.publisher.influxdb.InfluxDbPublisher
import io.github.cdsap.talaiot.publisher.pushgateway.PushGatewayPublisher
import io.github.cdsap.talaiot.publisher.rethinkdb.RethinkDbPublisher
// import io.github.cdsap.talaiot.publisher.timeline.TimelinePublisher
import org.gradle.api.Project
import java.util.concurrent.Executors

class TalaiotConfigurationProvider(
    val project: Project
) : PublisherConfigurationProvider {
    override fun get(): List<Publisher> {
        val publishers = mutableListOf<Publisher>()
        val talaiotExtension = project.extensions.getByName("talaiot") as TalaiotPluginExtension
        val logger = LogTrackerImpl(talaiotExtension.logger)
        val executor = Executors.newSingleThreadExecutor()
        val heavyExecutor = Executors.newSingleThreadExecutor()

        println("3")
        println(talaiotExtension.publishers.toString())
        talaiotExtension.publishers?.apply {
            outputPublisher?.apply {
                publishers.add(OutputPublisher(this, logger))
            }

            influxDbPublisher?.apply {
                publishers.add(
                    InfluxDbPublisher(
                        this,
                        logger
                    )
                )
            }

            pushGatewayPublisher?.apply {
                publishers.add(
                    PushGatewayPublisher(
                        this,
                        logger
                    )
                )
            }
            println("kkskdskdskdskkdkddkskds")
            if (jsonPublisher) {
                println("adding this thing")
                publishers.add(JsonPublisher(project.gradle.rootProject.buildDir))
            }

            elasticSearchPublisher?.apply {
                publishers.add(
                    ElasticSearchPublisher(
                        this,
                        logger
                    )
                )
            }

            hybridPublisher?.apply {
                publishers.add(
                    HybridPublisher(
                        this,
                        logger
                    )
                )
            }

            rethinkDbPublisher?.apply {
                publishers.add(
                    RethinkDbPublisher(
                        this,
                        logger
                    )
                )
            }

            publishers.addAll(customPublishers)
        }
        return publishers
    }
}
