package io.github.cdsap.talaiot.plugin.elasticsearch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.cdsap.talaiot.utils.TemporaryFolder
import io.kotest.core.spec.style.BehaviorSpec
import org.apache.http.HttpHost
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.RestClient
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.testcontainers.elasticsearch.KElasticSearchContainer

class ElasticsearchPluginTest : BehaviorSpec() {
    private val container = KElasticSearchContainer()
    
    init {
        given("ElasticSearch Talaiot Plugin") {
            container.start()
            val testProjectDir = TemporaryFolder()
            `when`("Project includes the plugin") {
                testProjectDir.create()
                val buildFile = testProjectDir.newFile("build.gradle")
                buildFile.appendText(
                    """
                   plugins {
                      id 'java'
                      id 'io.github.cdsap.talaiot.plugin.elasticsearch'
                   }

                  talaiot {
                    publishers {
                      elasticSearchPublisher {
                           url = "http://${container.httpHostAddress}"
                      }
                    }
                  }
            """
                )

                val result = GradleRunner.create()
                    .withProjectDir(testProjectDir.getRoot())
                    .withArguments("assemble")
                    .withPluginClasspath()
                    .build()
                then("there are records in the ElasticSearch instance") {
                    assert(result.task(":assemble")?.outcome == TaskOutcome.SUCCESS)
                    Thread.sleep(10000)

                    val url = container.httpHostAddress.split(":")
                    val client =
                        RestClient.builder(HttpHost(url[0], url[1].toInt(), "http")).build()
                    val parser = JsonParser()

                    val responseBuild =
                        client.performRequest(
                            org.elasticsearch.client.Request(
                                "GET",
                                "/build/_search?"
                            )
                        )

                    val contentBuild = EntityUtils.toString(responseBuild.entity)
                    val hitsBuild =
                        (parser.parse(contentBuild.toString()) as JsonObject).get("hits").asJsonObject
                    val hitsContentBuild = (hitsBuild.get("hits").asJsonArray)[0].asJsonObject
                    val elementsBuild = hitsContentBuild.get("_source").asJsonObject

                    assert(hitsContentBuild.get("_index").asString == "build")
                    assert(elementsBuild.get("requestedTasks").asString == "assemble")
                }
                testProjectDir.delete()
                container.stop()
            }
        }
    }
}
