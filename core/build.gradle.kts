import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

val mainClass by extra { "org.mikand.autonomous.services.core.CoreLauncher" }
val mainVerticleName by extra { "org.mikand.autonomous.services.core.CoreDeploymentVerticle" }

project.setProperty("mainClassName", "org.mikand.autonomous.services.core.CoreLauncher")

dependencies {
    // Nannoq
    api(Libs.nannoq_cluster)
}

val shadowJar by tasks.getting(ShadowJar::class) {
    manifest {
        attributes(mapOf(
                Pair("Main-Class", mainClass),
                Pair("Main-Verticle", mainVerticleName))
        )
    }
}

tasks {
    val run by existing(JavaExec::class) {
        args("run", mainVerticleName,
                "--redeploy=${project.extra["watchForChange"] as String}",
                "--launcher-class=$mainClass",
                "--on-redeploy=${project.extra["doOnChange"] as String}",
                "-conf ${project.extra["confFile"] as String}")
    }
}

publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            pom.withXml {
                asNode().appendNode("name", "AS Core")
                asNode().appendNode("description", "Core Components of AS")
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