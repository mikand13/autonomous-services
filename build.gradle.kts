import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
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
    group = "org.mikand.autonomous.services"
    version = "1.0.0-SNAPSHOT"

    repositories {
        jcenter()
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        println("Configuring $name in project ${project.name}...")

        kotlinOptions {
            jvmTarget = "1.8"
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