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
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KaptAnnotationProcessorOptions
import org.jetbrains.kotlin.gradle.plugin.KaptJavacOptionsDelegate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.net.ServerSocket;

val mainClass = "org.mikand.autonomous.services.storage.StorageLauncher"
val mainVerticleName = "org.mikand.autonomous.services.storage.StorageDeploymentVerticle"

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
    "..\\gradlew :storage:classes"
} else {
    "../gradlew :storage:classes"
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
val sqlLiteVersion = "1.0.392"

buildscript {
    var kotlin_version: String by extra
    var dokka_version: String by extra
    kotlin_version = "1.2.21"
    dokka_version = "0.9.16"

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "http://dl.bintray.com/vermeulen-mp/gradle-plugins")
    }

    dependencies {
        classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:0.13.0")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.2")
        classpath("com.wiredforcode:gradle-spawn-plugin:0.8.0")
        classpath(kotlin("gradle-plugin", kotlin_version))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "http://dynamodb-local.s3-website-us-west-2.amazonaws.com/release")
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
    compile("com.nannoq:web:$nannoq_tools_version")

    // Vert.x
    compile("io.vertx:vertx-health-check:$vertx_version")
    compile("io.vertx:vertx-lang-kotlin-coroutines:$vertx_version")

    // Kapt
    kapt("io.vertx:vertx-codegen:$vertx_version:processor")
    kapt("io.vertx:vertx-service-proxy:$vertx_version:processor")
    kaptTest("io.vertx:vertx-codegen:$vertx_version:processor")
    kaptTest("io.vertx:vertx-service-proxy:$vertx_version:processor")

    // Log4j2
    compile(group = "org.apache.logging.log4j", name = "log4j-api", version = log4j_version.toString())
    compile(group = "org.apache.logging.log4j", name = "log4j-core", version = log4j_version.toString())
    compile(group = "com.lmax", name = "disruptor", version = com_lmax_version.toString())

    // AWS DynamoDB
    //testCompile("com.amazonaws:DynamoDBLocal:9.5.0")

    // Test
    testCompile("junit:junit:$junit_version")
    testCompile("org.jetbrains.kotlin:kotlin-test")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit")
    testCompile("io.vertx:vertx-config:$vertx_version")
    testCompile("io.vertx:vertx-unit:$vertx_version")
    testCompile("io.rest-assured:rest-assured:$rest_assured_version")

    // DynamoDB Test
    testCompile("com.amazonaws:DynamoDBLocal:[1.11.2,2.0]")
    testCompile("com.almworks.sqlite4java:sqlite4java:$sqlLiteVersion")
    testCompile("com.almworks.sqlite4java:sqlite4java-win32-x86:$sqlLiteVersion")
    testCompile("com.almworks.sqlite4java:sqlite4java-win32-x64:$sqlLiteVersion")
    testCompile("com.almworks.sqlite4java:libsqlite4java-osx:$sqlLiteVersion")
    testCompile("com.almworks.sqlite4java:libsqlite4java-linux-i386:$sqlLiteVersion")
    testCompile("com.almworks.sqlite4java:libsqlite4java-linux-amd64:$sqlLiteVersion")
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

    "copyJsServiceProxies"(Copy::class) {
        delete("$projectDir/src/test/resources/js/extractedProxies")

        configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts.forEach({
            if (isExtract(it.id.componentIdentifier.displayName)) {
                copy {
                    from(zipTree(it.file))
                    into(file("$buildDir/nannoq-artifacts/${it.name}"))
                }

                copy {
                    from("$buildDir/nannoq-artifacts/${it.name}/nannoqRepositoryService-js") {
                        include("*-proxy.js")
                    }

                    into("$projectDir/src/test/resources/js/karma/extractedProxies")
                }
            }
        })
    }

    "copyDynamoDBLibs"(Copy::class) {
        delete("$projectDir/build/dynamodb-libs")

        configurations.getByName("testCompile").resolvedConfiguration.resolvedArtifacts.forEach({
            if (isSqlite(it.id.componentIdentifier.displayName)) {
                copy {
                    from(it.file)
                    into(file("$buildDir/sqlite/${it.name}"))
                }

                copy {
                    from("$buildDir/sqlite/${it.name}") {
                        include(listOf("*.so", "*.dll", "*.dylib"))
                    }

                    into("$projectDir/build/dynamodb-libs")
                }
            }
        })
    }

    "processResources" {
        dependsOn(listOf("copyJsServiceProxies", "copyDynamoDBLibs"))
    }

    "startServer"(SpawnProcessTask::class) {
        dependsOn("shadowJar")
        doFirst({
            command = "java -jar $projectDir/build/libs/$nameOfArchive -conf ${writeCustomConfToConf(vertxPort)}"
        })

        ready = "running"
        directory = "storage/build/tmp"
        pidLockFileName = ".storage.pid.lock"
    }

    "stopServer"(KillProcessTask::class) {
        directory = "storage/build/tmp"
        pidLockFileName = ".storage.pid.lock"
    }

    "karmaRun" {
        dependsOn("startServer")
        delete("$buildDir/karma.conf.js")
        finalizedBy("stopServer")
    }

    "test"(Test::class) {
        dependsOn("processResources")
        mustRunAfter("processResources")

        maxParallelForks = 4
        systemProperties = mapOf(
                Pair("vertx.logger-delegate-factory-class-name", logger_factory_version.toString()),
                Pair("java.library.path", file("$projectDir/build/dynamodb-libs").absolutePath))
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
        mustRunAfter(listOf("clean", "test"))
    }

    "publish" {
        dependsOn(listOf("signSourcesJar", "signPackageJavadoc", "signShadowJar"))
        mustRunAfter(listOf("verify", "signSourcesJar", "signPackageJavadoc", "signShadowJar"))
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
            artifact(file("$projectDir/build/libs/$nameOfArchive")) {
                builtBy(tasks.findByName("shadowJar"))

                classifier = "jar"
            }

            artifact(sourcesJar) {
                classifier = "sources"
            }

            artifact(packageJavadoc) {
                classifier = "javadoc"
            }

            pom.withXml {
                asNode().appendNode("name", "AS Storage")
                asNode().appendNode("description", "Storage Layer of AS")
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

                val dependenciesNode = asNode().appendNode("dependencies")

                configurations.compile.allDependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
            }
        }
    }
}

fun isExtract(componentIdentifier: String) : Boolean {
    return componentIdentifier.contains("com.nannoq:repository")
}

fun isSqlite(componentIdentifier: String) : Boolean {
    return componentIdentifier.contains("sqlite4java")
}

fun findFreePort() = ServerSocket(0).use {
    it.localPort
}

fun writeCustomConfToConf(vertxPort: Int): String {
    val config = JsonSlurper().parseText(File("${projectDir}/src/test/resources/app-conf.json").readText())
    val outPutConfig = File("$buildDir/tmp/app-conf-test.json")
    outPutConfig.createNewFile()

    val builder = JsonBuilder(config)
    val openJson = builder.toPrettyString().removeSuffix("}")
    val newConfig = JsonBuilder(JsonSlurper().parseText("$openJson, \"bridgePort\":$vertxPort}")).toPrettyString()

    outPutConfig.bufferedWriter().use { out ->
        out.write(newConfig)
        out.flush()
    }

    return outPutConfig.absolutePath
}