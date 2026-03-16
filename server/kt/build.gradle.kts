import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.expediagroup.graphql)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotest)
    application
    jacoco
}

group = "com.cherba29.tally"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.content)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation(libs.ktor.server.content)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.statuspages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.graphql.kotlin.ktor.server)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest)
    testImplementation(libs.kotest.jvm)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.assertions.jvm)
    testImplementation(libs.kotest.assertions.ktor)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.junit5.jvm)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.test.jvm)

    testRuntimeOnly(libs.logback.classic)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks {
    test {
        testLogging {
            events("passed", "skipped", "failed")
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

graphql {
    schema {
        packages = listOf("com.cherba29.tally")
    }
}
