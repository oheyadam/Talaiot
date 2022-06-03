package io.github.cdsap.talaiot.plugin

import io.github.cdsap.talaiot.Talaiot
import org.gradle.api.Plugin
import org.gradle.api.Project

class TalaiotPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        //     target.gradle.taskGraph.whenReady {
//        target.afterEvaluate {
//            Talaiot(
//                TalaiotPluginExtension::class.java,
//                TalaiotConfigurationProvider(
//                    (target.extensions.getByName("talaiot") as TalaiotPluginExtension).publishers!!
//                )
//            ).setUpPlugin(target)
//        }
        Talaiot(
            TalaiotPluginExtension::class.java,
            TalaiotConfigurationProvider(
                target
            )
        ).setUpPlugin(target)
        //   }
    }
}
