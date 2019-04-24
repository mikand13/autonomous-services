import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.io.File
import java.net.ServerSocket

class BuildUtils {
    fun calculateVersion(version: String, branch: String): String {
        return when {
            branch == "develop" -> "$version-SNAPSHOT"
            branch == "master" -> version
            branch.startsWith("release", ignoreCase = true) -> version
            else -> "$version-$branch"
        }
    }

    fun gitBranch(): String {
        return Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD")
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
    }

    fun findFreePort() = ServerSocket(0).use {
        it.localPort
    }

    fun writeCustomConfToConf(projectDir: String, buildDir: String, vertxPort: Int): String {
        val file = File("$projectDir/src/test/resources/app-conf.json")
        val config = JsonSlurper().parseText(file.readText())
        val outPutConfig = File("$buildDir/tmp/app-conf-test.json")
        outPutConfig.createNewFile()

        val builder = JsonBuilder(config)
        val openJson = builder.toPrettyString().removeSuffix("}")
        val newConfig = JsonBuilder(JsonSlurper()
                .parseText("$openJson, \"gateway\":{\"bridgePort\":$vertxPort}}")).toPrettyString()

        outPutConfig.bufferedWriter().use { out ->
            out.write(newConfig)
            out.flush()
        }

        return outPutConfig.absolutePath
    }
}
