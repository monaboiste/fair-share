package fairshare.acceptance.support

import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class FairShareTestEnvironment(workDirectory: Path) : AutoCloseable {
    private val log = workDirectory.resolve("application.log")
    private val application = ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-jar",
        checkNotNull(System.getProperty("fair-share.test.jar")) { "Run acceptance tests with Gradle" },
        "--server.port=0",
        "--server.address=127.0.0.1",
        "--fair-share.persistence.sqlite.jdbc-url=jdbc:sqlite:${workDirectory.resolve("groups.db")}",
        "--fair-share.rest.problem-origin=https://reference.example.test",
        "--logging.structured.format.console=",
        "--spring.main.banner-mode=off",
    )
        .directory(workDirectory.toFile())
        .redirectErrorStream(true)
        .redirectOutput(log.toFile())
        .start()

    private var http: FairShareClient? = null

    fun awaitClient(): FairShareClient {
        val deadline = TimeSource.Monotonic.markNow() + 60.seconds
        while (application.isAlive && !deadline.hasPassedNow()) {
            val port = STARTUP_PORT.find(log.readText())?.groupValues?.get(1)
            if (port != null) {
                val client = FairShareClient(URI("http://127.0.0.1:$port"))
                http = client
                if (client.awaitReadiness(Duration.ofSeconds(10))) return client
                break
            }
            Thread.sleep(50)
        }
        error("Packaged Fair Share did not become ready:\n${log.readText()}")
    }

    override fun close() {
        try {
            http?.close()
        } finally {
            application.destroy()
            if (!application.waitFor(5, TimeUnit.SECONDS)) {
                application.destroyForcibly()
                application.waitFor(5, TimeUnit.SECONDS)
            }
        }
    }
}

private val STARTUP_PORT = Regex("Tomcat started on port (\\d+)")
