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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val groupValue : String = "org.mikand.autonomous.services"
val versionValue : String = "1.0.0-SNAPSHOT"
val jvmTargetValue : String = "1.8"

ext.set("vertx_version", "3.5.0")
ext.set("kotlin_version", "1.2.10")
ext.set("hazelcast_version", "3.8.2")
ext.set("log4j_version", "2.9.1")
ext.set("junit_version", "4.12")
ext.set("com_lmax_version", "3.3.4")
ext.set("rest_assured_version", "3.0.3")
ext.set("logger_factory_version", "io.vertx.core.logging.Log4j2LogDelegateFactory")

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", "1.2.10"))
    }
}

plugins {
    base

    kotlin("jvm") version "1.2.10" apply false
}

apply {
    plugin("kotlin")
}

allprojects {
    group = groupValue
    version = versionValue

    repositories {
        jcenter()
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        println("Configuring $name in project ${project.name}...")

        kotlinOptions {
            jvmTarget = jvmTargetValue
        }
    }
}

dependencies {
    subprojects.forEach {
        archives(it)
    }
}

repositories {
    mavenCentral()
    jcenter()
}