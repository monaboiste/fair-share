#!/usr/bin/env python3
"""Write Gradle quality reports to GitHub Actions job summaries."""

import argparse
import os
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


def report_files(pattern: str) -> list[Path]:
    return sorted(Path(".").glob(pattern))


def project_name(report: Path) -> str:
    parts = report.parts
    try:
        build_index = parts.index("build")
    except ValueError:
        return "root"
    return ":" + ":".join(parts[:build_index]) if build_index else "root"


def parse_xml(report: Path) -> ET.Element | None:
    try:
        return ET.parse(report).getroot()
    except ET.ParseError as exc:
        print(f"::warning file={report}::Cannot parse XML: {exc}")
        return None


def percentage(numerator: int, denominator: int) -> float | None:
    return 100 * numerator / denominator if denominator else None


def fmt(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.1f}%"


def sort_score(value: float | None) -> float:
    return value if value is not None else 101.0


@dataclass
class Coverage:
    line_covered: int = 0
    line_missed: int = 0
    branch_covered: int = 0
    branch_missed: int = 0

    def add(self, counter: ET.Element) -> None:
        kind = counter.get("type")
        covered = int(counter.get("covered", 0))
        missed = int(counter.get("missed", 0))
        if kind == "LINE":
            self.line_covered += covered
            self.line_missed += missed
        elif kind == "BRANCH":
            self.branch_covered += covered
            self.branch_missed += missed

    def line(self) -> float | None:
        return percentage(self.line_covered, self.line_covered + self.line_missed)

    def branch(self) -> float | None:
        return percentage(self.branch_covered, self.branch_covered + self.branch_missed)


def coverage_summary() -> list[str]:
    lines = ["## Coverage (Kover)", ""]
    reports = report_files("**/build/reports/kover/report.xml")
    if not reports:
        print("::warning::No Kover XML reports found")
        return lines + ["No Kover XML report found.", ""]

    overall = Coverage()
    modules: defaultdict[str, Coverage] = defaultdict(Coverage)
    packages: defaultdict[tuple[str, str], Coverage] = defaultdict(Coverage)

    for report in reports:
        root = parse_xml(report)
        if root is None:
            continue
        module = project_name(report)
        for counter in root.findall("counter"):
            overall.add(counter)
            modules[module].add(counter)
        for package in root.findall("package"):
            name = package.get("name", "(unknown)").replace("/", ".")
            for counter in package.findall("counter"):
                packages[(module, name)].add(counter)

    lines += [
        f"**Overall line coverage: {fmt(overall.line())}**  ",
        f"**Overall branch coverage: {fmt(overall.branch())}**",
        "",
        "| Module | Lines | Branches | Missed lines |",
        "| --- | ---: | ---: | ---: |",
    ]
    for module, stats in sorted(modules.items(), key=lambda item: (sort_score(item[1].line()), item[0])):
        lines.append(f"| `{module}` | {fmt(stats.line())} | {fmt(stats.branch())} | {stats.line_missed} |")

    if packages:
        lines += [
            "", "<details><summary>Coverage by package</summary>", "",
            "| Module | Package | Lines | Branches | Missed lines |",
            "| --- | --- | ---: | ---: | ---: |",
        ]
        for (module, package), stats in sorted(
            packages.items(), key=lambda item: (sort_score(item[1].line()), item[0])
        ):
            lines.append(
                f"| `{module}` | `{package}` | {fmt(stats.line())} | {fmt(stats.branch())} | {stats.line_missed} |"
            )
        lines += ["", "</details>"]
    return lines + [""]


def detekt_summary() -> list[str]:
    lines = ["## Static analysis (Detekt)", ""]
    reports = report_files("**/build/reports/detekt/detektMain.xml")
    if not reports:
        print("::warning::No Detekt Checkstyle XML reports found")
        return lines + ["No Detekt XML report found (check build logs if compilation failed).", ""]

    counts: defaultdict[str, Counter] = defaultdict(Counter)
    for report in reports:
        root = parse_xml(report)
        if root is None:
            continue
        module = project_name(report)
        counts[module]  # Keep modules with zero findings.
        for file in root.findall("file"):
            for error in file.findall("error"):
                severity = error.get("severity", "unknown").lower()
                counts[module][severity] += 1

    lines += [
        "| Module | Errors | Warnings | Other findings |",
        "| --- | ---: | ---: | ---: |",
    ]
    for module, count in sorted(counts.items()):
        other = sum(n for severity, n in count.items() if severity not in {"error", "warning"})
        lines.append(f"| `{module}` | {count['error']} | {count['warning']} | {other} |")
    return lines + ["", "Full HTML, Markdown and SARIF reports are in the quality artifact.", ""]


def dependencies_summary() -> list[str]:
    lines = ["## Dependency analysis", ""]
    reports = report_files("**/build/reports/dependency-analysis/project-health-report.txt")
    if not reports:
        print("::warning::No dependency-analysis reports found")
        return lines + ["No dependency-analysis report found.", ""]
    for report in reports:
        module = project_name(report)
        content = report.read_text(encoding="utf-8", errors="replace").strip()
        if not content:
            lines.append(f"- `{module}`: report is empty (no advice).")
            continue
        limit = 10000
        text = content[:limit]
        if len(content) > limit:
            text += "\n... truncated; see artifact for full report"
        # Triple-tilde fences are unlikely to occur in a plain-text health report.
        lines += [
            f"<details><summary>Dependency advice for {module}</summary>",
            "", "~~~text", text, "~~~", "", "</details>", "",
        ]
    return lines + [""]


@dataclass
class Mutations:
    total: int = 0
    detected: int = 0
    killed: int = 0
    survived: int = 0
    no_coverage: int = 0
    other: int = 0

    def add(self, mutation: ET.Element) -> None:
        status = mutation.get("status", "UNKNOWN")
        detected = mutation.get("detected")
        self.total += 1
        was_detected = (
            detected == "true"
            if detected is not None
            else status in {"KILLED", "TIMED_OUT"}
        )
        if was_detected:
            self.detected += 1
        if status == "KILLED":
            self.killed += 1
        elif status == "SURVIVED":
            self.survived += 1
        elif status == "NO_COVERAGE":
            self.no_coverage += 1
        else:
            self.other += 1

    def score(self) -> float | None:
        return percentage(self.detected, self.total)

    def strength(self) -> float | None:
        # PIT: killed / (killed + survived), excluding mutants without coverage.
        return percentage(self.killed, self.killed + self.survived)


def mutations_summary() -> list[str]:
    lines = ["## Mutation testing (PIT)", ""]
    reports = report_files("**/build/reports/pitest/mutations.xml")
    if not reports:
        print("::warning::No PIT XML reports found (possibly no mutations generated)")
        return lines + ["No PIT XML report found (possibly no mutations generated).", ""]

    overall = Mutations()
    modules: defaultdict[str, Mutations] = defaultdict(Mutations)
    packages: defaultdict[tuple[str, str], Mutations] = defaultdict(Mutations)
    for report in reports:
        root = parse_xml(report)
        if root is None:
            continue
        module = project_name(report)
        for mutation in root.findall("mutation"):
            mutated_class = mutation.findtext("mutatedClass") or "(unknown)"
            package = mutated_class.rsplit(".", 1)[0] if "." in mutated_class else "(default)"
            for stats in (overall, modules[module], packages[(module, package)]):
                stats.add(mutation)

    lines += [
        f"**Overall mutation score: {fmt(overall.score())}**  ",
        f"**Overall test strength: {fmt(overall.strength())}**  ",
        f"Detected: {overall.detected} / {overall.total} mutations",
        "",
        "| Module | Score | Test strength | Survived | No coverage | Other | Total |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for module, stats in sorted(modules.items(), key=lambda item: (sort_score(item[1].score()), item[0])):
        lines.append(
            f"| `{module}` | {fmt(stats.score())} | {fmt(stats.strength())} | "
            f"{stats.survived} | {stats.no_coverage} | {stats.other} | {stats.total} |"
        )

    if packages:
        lines += [
            "", "<details><summary>Mutations by package</summary>", "",
            "| Module | Package | Score | Test strength | Survived | No coverage | Other | Total |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |",
        ]
        for (module, package), stats in sorted(
            packages.items(), key=lambda item: (sort_score(item[1].score()), item[0])
        ):
            lines.append(
                f"| `{module}` | `{package}` | {fmt(stats.score())} | {fmt(stats.strength())} | "
                f"{stats.survived} | {stats.no_coverage} | {stats.other} | {stats.total} |"
            )
        lines += ["", "</details>"]

    return lines + ["", "Other includes timed-out, invalid, or errored mutations.", ""]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("kind", choices=["verify", "mutation"])
    args = parser.parse_args()

    parts = (
        coverage_summary() + detekt_summary() + dependencies_summary()
        if args.kind == "verify"
        else mutations_summary()
    )
    body = "\n".join(parts).rstrip() + "\n"
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if summary_path:
        with Path(summary_path).open("a", encoding="utf-8") as summary:
            summary.write(body)
    else:
        print(body)


if __name__ == "__main__":
    main()
