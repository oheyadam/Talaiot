package com.talaiot.buildplugins

import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import java.io.File

fun Project.setUpJacoco() = this.run {
    tasks.withType<JacocoReport> {
        reports {
            xml.isEnabled = true
            csv.isEnabled = true
            html.isEnabled = true
            html.destination = File("${rootProject.buildDir}/coverage")
        }
    }
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.3"
    }
}