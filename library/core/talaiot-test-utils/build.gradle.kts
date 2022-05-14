plugins {
    id("kotlinLib")
}

talaiotLib {
    artifact = "talaiot-test-utils"
    group = io.github.cdsap.talaiot.buildplugins.Constants.DEFAULT_GROUP_LIBRARY
    version = io.github.cdsap.talaiot.buildplugins.Constants.TALAIOT_VERSION
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
    implementation("io.kotest:kotest-runner-junit5-jvm:5.3.0")
    implementation(project(":library:core:talaiot-logger"))
    implementation("org.testcontainers:testcontainers:1.11.3")
    api("org.testcontainers:influxdb:1.15.2")
    api("org.testcontainers:elasticsearch:1.17.1")
}
