package com.softwarearchetypes.graphs.scheduling.concurrency

import com.softwarearchetypes.graphs.scheduling.ProcessStep
import spock.lang.Specification

class ConcurrencySpec extends Specification {

    private static final ProcessStep MEASUREMENT = new ProcessStep("Measurement")
    private static final ProcessStep CALIBRATION = new ProcessStep("Calibration")
    private static final ProcessStep VALIDATION = new ProcessStep("Validation")
    private static final ProcessStep FINAL_TEST = new ProcessStep("Final Test")

    def "assigns laboratory steps to three minimal environments"() {
        given:
        def process = Concurrency.builder()

        when:
        def environments = process
                .addConflict(MEASUREMENT, CALIBRATION)
                .addConflict(MEASUREMENT, VALIDATION)
                .addConflict(FINAL_TEST, MEASUREMENT)
                .addConflict(FINAL_TEST, CALIBRATION)
                .addConflict(FINAL_TEST, VALIDATION)
                .build()

        then:
        environments.environmentCount() == 3
        environments.canRunConcurrently(CALIBRATION, VALIDATION)
        !environments.canRunConcurrently(MEASUREMENT, CALIBRATION)
        !environments.canRunConcurrently(MEASUREMENT, VALIDATION)
        !environments.canRunConcurrently(FINAL_TEST, MEASUREMENT)
        !environments.canRunConcurrently(FINAL_TEST, CALIBRATION)
        !environments.canRunConcurrently(FINAL_TEST, VALIDATION)
    }

    def "assigns steps without conflicts to one environment"() {
        given:
        def step1 = new ProcessStep("Step 1")
        def step2 = new ProcessStep("Step 2")
        def step3 = new ProcessStep("Step 3")

        when:
        def environments = Concurrency.builder()
                .addStep(step1)
                .addStep(step2)
                .addStep(step3)
                .build()

        then:
        environments.environmentCount() == 1
        environments.canRunConcurrently(step1, step2)
        environments.canRunConcurrently(step2, step3)
        environments.canRunConcurrently(step1, step3)
    }

    def "assigns a complete conflict graph to the maximum environments"() {
        given:
        def step1 = new ProcessStep("Step 1")
        def step2 = new ProcessStep("Step 2")
        def step3 = new ProcessStep("Step 3")

        when:
        def environments = Concurrency.builder()
                .addConflict(step1, step2)
                .addConflict(step1, step3)
                .addConflict(step2, step3)
                .build()

        then:
        environments.environmentCount() == 3
        !environments.canRunConcurrently(step1, step2)
        !environments.canRunConcurrently(step1, step3)
        !environments.canRunConcurrently(step2, step3)
    }

    def "assigns chain conflicts to two environments"() {
        given:
        def stepA = new ProcessStep("Step A")
        def stepB = new ProcessStep("Step B")
        def stepC = new ProcessStep("Step C")

        when:
        def environments = Concurrency.builder()
                .addConflict(stepA, stepB)
                .addConflict(stepB, stepC)
                .build()

        then:
        environments.environmentCount() == 2
        !environments.canRunConcurrently(stepA, stepB)
        !environments.canRunConcurrently(stepB, stepC)
        environments.canRunConcurrently(stepA, stepC)
    }
}
