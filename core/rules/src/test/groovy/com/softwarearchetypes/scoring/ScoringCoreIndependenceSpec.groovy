package com.softwarearchetypes.scoring

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Specification

class ScoringCoreIndependenceSpec extends Specification {

    private static final List<Path> CORE = [
            Path.of("src/main/java/com/softwarearchetypes/scoring/ast"),
            Path.of("src/main/java/com/softwarearchetypes/scoring/algebra"),
            Path.of("src/main/java/com/softwarearchetypes/scoring/context"),
            Path.of("src/main/java/com/softwarearchetypes/scoring/EventRuleEngine.java")
    ]

    private static final List<String> FORBIDDEN = [
            "com.softwarearchetypes.scoring.customer",
            "Customer",
            "PURCHASE_AMOUNT",
            "COMPLAINT"
    ]

    def "scoring core names no business metric"() {
        when:
        def leaks = CORE
                .collectMany { path -> sources(path) }
                .collectMany { source -> forbiddenLines(source) }
                .sort()

        then:
        leaks.empty
    }

    private static List<Path> sources(Path path) {
        if (!Files.isDirectory(path)) {
            return [path]
        }
        Files.walk(path).withCloseable { files ->
            files.filter { file -> file.toString().endsWith(".java") }.toList()
        }
    }

    private static List<String> forbiddenLines(Path source) {
        Files.readAllLines(source)
                .findAll { line -> FORBIDDEN.any { forbidden -> line.contains(forbidden) } }
                .collect { line -> "${source.fileName}: ${line.trim()}" }
    }
}
