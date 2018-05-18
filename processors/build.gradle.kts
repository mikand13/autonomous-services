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
import com.wiredforcode.gradle.spawn.KillProcessTask
import com.wiredforcode.gradle.spawn.SpawnProcessTask
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*
import org.gradle.script.lang.kotlin.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KaptAnnotationProcessorOptions
import org.jetbrains.kotlin.gradle.plugin.KaptJavacOptionsDelegate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.net.ServerSocket

val mainClass = "org.mikand.autonomous.services.processors.ProcessorsLauncher"
val mainVerticleName = "org.mikand.autonomous.services.processors.ProcessorsDeploymentVerticle"

val watchForChange = "src/**/*"
val confFile = "src/main/resources/app-conf.json"
var doOnChange : String
val projectName = project.name
val projectVersion = project.version
val nameOfArchive = "$projectName-$projectVersion-fat.jar"
val dockerImageName = "autonomous_services/$projectName"
val dockerFileLocation = "src/main/docker/Dockerfile"

val vertxPort = findFreePort()

doOnChange = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    "..\\gradlew :processors:classes"
} else {
    "../gradlew :processors:classes"
}

val kotlin_version: String by project
val vertx_version: String by project
val hazelcast_version: String by project
val log4j_version: String by project
val com_lmax_version: String by project
val junit_version: String by project
val rest_assured_version: String by project
val logger_factory_version: String by project
val nannoq_tools_version: String by project

buildscript {
    var kotlin_version: String by extra
    var dokka_version: String by extra
    kotlin_version = "1.2.41"
    dokka_version = "0.9.16"

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "http://dl.bintray.com/vermeulen-mp/gradle-plugins")
    }

    dependencies {
        classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:0.13.0")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.3")
        classpath("com.wiredforcode:gradle-spawn-plugin:0.8.0")
        classpath(kotlin("gradle-plugin", kotlin_version))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

plugins {
    id("java")
    id("kotlin")
    id("application")
    id("com.craigburke.karma") version("1.4.4")
    id("com.wiredforcode.spawn") version("0.8.0")

    @Suppress("RemoveRedundantBackticks")
    `maven-publish`
    signing
}

project.setProperty("mainClassName", mainClass)

apply {
    plugin("java")
    plugin("kotlin")
    plugin("application")
    plugin("com.github.johnrengelman.shadow")
    plugin("org.jetbrains.dokka")
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
    compile("com.nannoq:repository:$nannoq_tools_version")

    // Vert.x
    compile("io.vertx:vertx-health-check:$vertx_version")
    compile("io.vertx:vertx-lang-kotlin-coroutines:$vertx_version")

    // AS
    compile(project(":core"))

    // Kapt
    kapt("io.vertx:vertx-codegen:$vertx_version:processor")
    kapt("io.vertx:vertx-service-proxy:$vertx_version:processor")
    kaptTest("io.vertx:vertx-codegen:$vertx_version:processor")
    kaptTest("io.vertx:vertx-service-proxy:$vertx_version:processor")

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
            "vertx3-min@^3.4.2",
            "karma-browserify@^5.2.0",
            "browserify@^16.0.0"
    ))

    files = listOf("test/resources/js/karma/**/*_test.js")

    browsers = listOf("PhantomJS")
    frameworks = listOf("browserify", "jasmine")
    reporters = listOf("progress")

    preprocessors = mapOf(Pair("test/resources/js/karma/**/*_test.js", listOf("browserify")))
    propertyMissing("logLevel", "WARN")
    propertyMissing("port", findFreePort())
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/docs"
    jdkVersion = 8
}

val packageJavadoc by tasks.creating(Jar::class) {
    dependsOn("dokka")
    classifier = "javadoc"
    from(dokka.outputDirectory)
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = "fat"
    archiveName = nameOfArchive

    manifest {
        attributes(mapOf(
                "Main-Class" to mainClass,
                "Main-Verticle" to mainVerticleName)
        )
    }

    files(listOf("/src/main/java", "/src/main/kotlin"))

    mergeServiceFiles {
        include("META-INF/services/io.vertx.core.spi.VerticleFactory")
    }
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

    "dockerRun" {
        dependsOn("docker")
    }

    "startServer"(SpawnProcessTask::class) {
        dependsOn("shadowJar")
        doFirst({
            command = "java -jar $projectDir/build/libs/$nameOfArchive -conf ${writeCustomConfToConf(vertxPort)}"
        })

        ready = "running"
        directory = "processors/build/tmp"
        pidLockFileName = ".processors.pid.lock"
    }

    "stopServer"(KillProcessTask::class) {
        directory = "processors/build/tmp"
        pidLockFileName = ".processors.pid.lock"
    }

    "karmaRun" {
        dependsOn("startServer")
        delete("$buildDir/karma.conf.js")
        finalizedBy("stopServer")
    }

    "test"(Test::class) {
        maxParallelForks = 4
        systemProperties = mapOf(Pair("vertx.logger-delegate-factory-class-name", logger_factory_version.toString()))
    }

    "karmaRun" {
        dependsOn("karmaGenerateConfig")
    }

    "dockerPrepare" {
        dependsOn("shadowJar")
    }

    "docker" {
        mustRunAfter("test")
        doLast({
            println("Built image for $nameOfArchive!")
        })
    }

    "verify" {
        dependsOn(listOf("test"))
    }

    "publish" {
        dependsOn(listOf("signSourcesJar", "signPackageJavadoc", "signShadowJar"))
        mustRunAfter(listOf("signSourcesJar", "signPackageJavadoc", "signShadowJar"))
    }

    "install" {
        dependsOn(listOf("verify", "docker", "publish"))
        mustRunAfter("clean")

        doLast({
            println("$nameOfArchive installed!")
        })
    }
}

signing {
    useGpgCmd()
    sign(sourcesJar)
    sign(packageJavadoc)
    sign(shadowJar)

    sign(publishing.publications)
}

publishing {
    repositories {
        mavenLocal()

        if (projectVersion.toString().contains("-SNAPSHOT") && project.hasProperty("central")) {
            maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASS")
                }
            }
        } else if (project.hasProperty("central")) {
            maven(url = "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASS")
                }
            }
        }
    }

    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])

            artifact(sourcesJar) {
                classifier = "sources"
            }

            artifact(packageJavadoc) {
                classifier = "javadoc"
            }

            pom.withXml {
                asNode().appendNode("name", "AS Processors")
                asNode().appendNode("description", "Processors of Gateway Layer in AS")
                asNode().appendNode("url", "https://github.com/mikand13/autonomous-services")

                val scmNode = asNode().appendNode("scm")

                scmNode.appendNode("url", "https://github.com/mikand13/autonomous-services")
                scmNode.appendNode("connection", "scm:git:git://github.com/mikand13/autonomous-services")
                scmNode.appendNode("developerConnection", "scm:git:ssh:git@github.com/mikand13/autonomous-services")

                val licenses = asNode().appendNode("licenses")
                val license = licenses.appendNode("license")
                license.appendNode("name", "MIT License")
                license.appendNode("url", "http://www.opensource.org/licenses/mit-license.php")
                license.appendNode("distribution", "repo")

                val developers = asNode().appendNode("developers")
                val developer = developers.appendNode("developer")
                developer.appendNode("id", "mikand13")
                developer.appendNode("name", "Anders Mikkelsen")
                developer.appendNode("email", "mikkelsen.anders@gmail.com")
            }
        }
    }
}

fun findFreePort() = ServerSocket(0).use {
    it.localPort
}

fun writeCustomConfToConf(vertxPort: Int): String {
    val config = JsonSlurper().parseText(File("$projectDir/src/test/resources/app-conf.json").readText())
    val outPutConfig = file("$buildDir/tmp/app-conf-test.json")
    outPutConfig.createNewFile()

    val builder = JsonBuilder(config)
    val openJson = builder.toPrettyString().removeSuffix("}")
    val newConfig = JsonBuilder(JsonSlurper().parseText("$openJson, \"gateway\":{\"bridgePort\":$vertxPort}}")).toPrettyString()

    outPutConfig.bufferedWriter().use { out ->
        out.write(newConfig)
        out.flush()
    }

    return outPutConfig.absolutePath
}