/*
 * MIT License
 *
 * Copyright (c) 2017 Anders Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.craigburke.gradle.KarmaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.docker.DockerComponent
import com.palantir.gradle.docker.DockerExtension
import com.palantir.gradle.docker.DockerRunExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*
import org.gradle.script.lang.kotlin.*
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KaptAnnotationProcessorOptions
import org.jetbrains.kotlin.gradle.plugin.KaptJavacOptionsDelegate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mainClass = "org.mikand.autonomous.services.gateway.VertxLauncher"
val mainVerticleName = "org.mikand.autonomous.services.gateway.GatewayDeploymentVerticle"

val watchForChange = "src/**/*"
val confFile = "src/main/resources/app-conf.json"
var doOnChange : String
val projectName = project.name
val projectVersion = project.version
val nameOfArchive = "$projectName-$projectVersion-fat.jar"
val dockerImageName = "autonomous_services/$projectName"
val dockerFileLocation = "src/main/docker/Dockerfile"

doOnChange = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    "..\\gradlew :gateway:classes"
} else {
    "../gradlew :gateway:classes"
}

val kotlin_version by project
val vertx_version by project
val hazelcast_version by project
val log4j_version by project
val com_lmax_version by project
val junit_version by project
val rest_assured_version by project
val logger_factory_version by project
val nannoq_tools_version by project

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.21"

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:0.13.0")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.2")
        classpath(kotlin("gradle-plugin", kotlin_version))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

plugins {
    id("java")
    id("kotlin")
    id("application")
    id("com.craigburke.karma") version("1.4.4")
}

apply {
    plugin("java")
    plugin("kotlin")
    plugin("application")
    plugin("com.github.johnrengelman.shadow")
    plugin("com.palantir.docker")
    plugin("com.palantir.docker-run")
    plugin("com.palantir.docker-compose")
    plugin("kotlin-kapt")
    plugin("idea")
}

dependencies {
    // Kotlin
    compile(kotlin("stdlib", kotlin_version.toString()))
    compile(kotlin("stdlib-jdk8", kotlin_version.toString()))
    compile("org.jetbrains.kotlin:kotlin-reflect")

    // Nannoq
    compile("com.nannoq:tools:$nannoq_tools_version")
    compile("com.nannoq:cluster:$nannoq_tools_version")

    // Vert.x
    compile("io.vertx:vertx-health-check:$vertx_version")
    compile("io.vertx:vertx-lang-kotlin-coroutines:$vertx_version")

    // Kapt
    kapt("io.vertx:vertx-codegen:$vertx_version:processor")
    kapt("io.vertx:vertx-service-proxy:$vertx_version:processor")

    // Log4j2
    compile(group = "org.apache.logging.log4j", name = "log4j-api", version = log4j_version.toString())
    compile(group = "org.apache.logging.log4j", name = "log4j-core", version = log4j_version.toString())
    compile(group = "com.lmax", name = "disruptor", version = com_lmax_version.toString())

    // Test
    testCompile("junit:junit:$junit_version")
    testCompile("org.jetbrains.kotlin:kotlin-test")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit")
    testCompile("io.vertx:vertx-config:$vertx_version")
    testCompile("io.vertx:vertx-unit:$vertx_version")
    testCompile("io.rest-assured:rest-assured:$rest_assured_version")
}

configure<ApplicationPluginConvention> {
    mainClassName = mainClass
}

configure<KotlinProjectExtension> {
    experimental.coroutines = Coroutines.ENABLE
}

configure<DockerExtension> {
    name = dockerImageName
    val now = System.currentTimeMillis()
    tags("latest", "" + now)

    buildArgs(mapOf("jarName" to nameOfArchive))
    setDockerfile(file(dockerFileLocation))
    files("build/libs/$nameOfArchive")
    pull(true)
}

configure<DockerRunExtension> {
    name = "gateway-test"
    image = dockerImageName
    daemonize = false
    clean = true
}

karma {
    basePath = "../src"
    colors = true

    dependencies(listOf(
            "sockjs-client@^1.1.4",
            "vertx3-eventbus-client@^3.4.2",
            "vertx3-min@^3.4.2"
    ))

    files = listOf("test/resources/js/**/*_test.js")

    browsers = listOf("PhantomJS")
    frameworks = listOf("jasmine")
    reporters = listOf("junit")
}

tasks {
    "run"(JavaExec::class) {
        jvmArgs("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005")

        args("run", mainVerticleName,
                "--redeploy=$watchForChange",
                "--launcher-class=$mainClass",
                "--on-redeploy=$doOnChange",
                "-conf $confFile")
    }

    "shadowJar"(ShadowJar::class) {
        classifier = "fat"
        archiveName = nameOfArchive

        manifest {
            attributes(mapOf(
                    "Main-Class" to mainClass,
                    "Main-Verticle" to mainVerticleName)
            )
        }

        files("/src/main")

        mergeServiceFiles {
            include("META-INF/services/io.vertx.core.spi.VerticleFactory")
        }
    }

    "build" {
        dependsOn("shadowJar")
    }

    "docker" {
        dependsOn("build")
    }

    "dockerRun" {
        dependsOn("docker")
    }

    "copyJsServiceProxies"(Copy::class) {
        delete("${projectDir}/src/test/resources/js/extractedProxies")

        configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts.forEach({
            if (isExtract(it.id.componentIdentifier.displayName)) {
                println("Copying JS/TS proxies from: ${it.name}")

                copy {
                    from(zipTree(it.file))
                    into(file("${buildDir}/nannoq-artifacts/${it.name}"))
                }

                copy {
                    from("${buildDir}/nannoq-artifacts/${it.name}/nannoqHeartbeatService-js")
                    into("${projectDir}/src/test/resources/js/extractedProxies/js")
                }

                copy {
                    from("${buildDir}/nannoq-artifacts/${it.name}/nannoqHeartbeatService-ts")
                    into("${projectDir}/src/test/resources/js/extractedProxies/ts")
                }
            }
        })
    }

    "processResources" {
        dependsOn("copyJsServiceProxies")
    }

    withType<Test> {
        System.setProperty("vertx.logger-delegate-factory-class-name", logger_factory_version.toString())
    }
}

fun isExtract(componentIdentifier: String) : Boolean {
    return componentIdentifier.contains("com.nannoq:cluster")
}