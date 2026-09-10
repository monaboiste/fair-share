package com.softwarearchetypes.graphs.scheduling

import spock.lang.Specification

class ScheduleSpec extends Specification {

    private static final ProcessStep DRYING = new ProcessStep("Drying")
    private static final ProcessStep CALIBRATION = new ProcessStep("Calibration")
    private static final ProcessStep MEASUREMENT = new ProcessStep("Measurement")
    private static final ProcessStep ANALYSIS = new ProcessStep("Analysis")
    private static final ProcessStep VALIDATION = new ProcessStep("Validation")
    private static final ProcessStep PREPARATION = new ProcessStep("Preparation")
    private static final ProcessStep FINALIZATION = new ProcessStep("Finalization")
    private static final ProcessStep STEP_1 = new ProcessStep("Step 1")
    private static final ProcessStep STEP_2 = new ProcessStep("Step 2")
    private static final ProcessStep STEP_3 = new ProcessStep("Step 3")
    private static final ProcessStep PATH_1 = new ProcessStep("Path 1")
    private static final ProcessStep PATH_2 = new ProcessStep("Path 2")

    def "orders a simple linear process"() {
        given:
        def process = ProcessDefinition.builder()

        when:
        def schedule = process
                .addDependency(DRYING, MEASUREMENT, DependencyType.finishToStart("Sample must be dry"))
                .addDependency(MEASUREMENT, ANALYSIS, DependencyType.dataFlow("Spectrum"))
                .build()

        then:
        schedule.steps() == [DRYING, MEASUREMENT, ANALYSIS]
        schedule.first() == DRYING
        schedule.last() == ANALYSIS
    }

    def "respects dependencies in a complex process"() {
        given:
        def process = ProcessDefinition.builder()

        when:
        def schedule = process
                .addDependency(DRYING, MEASUREMENT)
                .addDependency(CALIBRATION, MEASUREMENT)
                .addDependency(MEASUREMENT, ANALYSIS)
                .addDependency(ANALYSIS, VALIDATION)
                .build()

        then:
        schedule.size() == 5
        schedule.last() == VALIDATION
        isBefore(schedule, DRYING, MEASUREMENT)
        isBefore(schedule, CALIBRATION, MEASUREMENT)
        isBefore(schedule, MEASUREMENT, ANALYSIS)
        isBefore(schedule, ANALYSIS, VALIDATION)
    }

    def "schedules a single step process"() {
        given:
        def process = ProcessDefinition.builder()

        when:
        def schedule = process
                .addStep(DRYING)
                .build()

        then:
        schedule.size() == 1
        schedule.first() == DRYING
        schedule.last() == DRYING
    }

    def "orders both branches of a diamond process"() {
        given:
        def process = ProcessDefinition.builder()

        when:
        def schedule = process
                .addDependency(PREPARATION, PATH_1)
                .addDependency(PREPARATION, PATH_2)
                .addDependency(PATH_1, FINALIZATION)
                .addDependency(PATH_2, FINALIZATION)
                .build()

        then:
        schedule.size() == 4
        schedule.first() == PREPARATION
        schedule.last() == FINALIZATION
        isBefore(schedule, PREPARATION, PATH_1)
        isBefore(schedule, PREPARATION, PATH_2)
        isBefore(schedule, PATH_1, FINALIZATION)
        isBefore(schedule, PATH_2, FINALIZATION)
    }

    def "rejects a cyclic dependency"() {
        given:
        def process = ProcessDefinition.builder()

        when:
        process
                .addDependency(STEP_1, STEP_2)
                .addDependency(STEP_2, STEP_3)
                .addDependency(STEP_3, STEP_1)
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    private static boolean isBefore(Schedule schedule, ProcessStep earlier, ProcessStep later) {
        schedule.steps().indexOf(earlier) < schedule.steps().indexOf(later)
    }
}
