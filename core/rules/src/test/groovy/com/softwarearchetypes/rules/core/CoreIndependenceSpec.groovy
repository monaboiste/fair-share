package com.softwarearchetypes.rules.core

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Specification

class CoreIndependenceSpec extends Specification {

    private static final List<String> FORBIDDEN = [
            "com.softwarearchetypes.rules.discounting",
            "com.softwarearchetypes.quantity",
            "com.softwarearchetypes.scoring"
    ]

    def "rule core does not depend on a domain plugin"() {
        when:
        def leaks = Files.walk(Path.of("src/main/java/com/softwarearchetypes/rules/core")).withCloseable { sources ->
            sources
                    .filter { path -> path.toString().endsWith(".java") }
                    .flatMap { path -> forbiddenImports(path) }
                    .sorted()
                    .toList()
        }

        then:
        leaks.empty
    }

    private static def forbiddenImports(Path source) {
        Files.readAllLines(source).stream()
                .filter { line -> line.startsWith("import ") }
                .filter { line -> FORBIDDEN.any { forbidden -> line.contains(forbidden) } }
                .map { line -> "${source.fileName}: ${line.trim()}" }
    }
}
