package com.softwarearchetypes.graphs.scenarios.influence

import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_A
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_B
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_C
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.emptyInfrastructure

import com.softwarearchetypes.graphs.fixture.influence.BridgingReservations
import com.softwarearchetypes.graphs.fixture.influence.InfluenceAnalyzer
import com.softwarearchetypes.graphs.fixture.influence.InfluenceMap
import com.softwarearchetypes.graphs.fixture.influence.PhysicsInfluence
import com.softwarearchetypes.graphs.fixture.influence.PhysicsProcess
import com.softwarearchetypes.graphs.fixture.influence.Reservation
import spock.lang.Specification

class BridgingReservationsAnalyzerSpec extends Specification {

    static final PhysicsProcess PROCESS_A = new PhysicsProcess("A")
    static final PhysicsProcess PROCESS_B = new PhysicsProcess("B")
    static final PhysicsProcess PROCESS_C = new PhysicsProcess("C")
    static final PhysicsProcess PROCESS_D = new PhysicsProcess("D")

    def "independent reservations are not critical"() {
        given:
        def processA = new PhysicsProcess("A")
        def processB = new PhysicsProcess("B")
        def processC = new PhysicsProcess("C")
        def physics = PhysicsInfluence.builder()
                .addInfluence(processA, processB)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def r1 = new Reservation(processA, LAB_A)
        def r2 = new Reservation(processB, LAB_B)
        def r3 = new Reservation(processC, LAB_C)

        when:
        BridgingReservations bridging = new InfluenceAnalyzer(influenceMap)
                .identifyCriticalReservations(Set.of(r1, r2, r3))

        then:
        bridging.isEmpty()
    }

    def "a reservation connecting two groups is critical"() {
        given:
        def processA = new PhysicsProcess("A")
        def processB = new PhysicsProcess("B")
        def processC = new PhysicsProcess("C")
        def processX = new PhysicsProcess("X")
        def processD = new PhysicsProcess("D")
        def processE = new PhysicsProcess("E")
        def processF = new PhysicsProcess("F")
        def physics = PhysicsInfluence.builder()
                .addInfluence(processA, processB)
                .addInfluence(processB, processC)
                .addInfluence(processC, processA)
                .addInfluence(processA, processX)
                .addInfluence(processX, processD)
                .addInfluence(processD, processE)
                .addInfluence(processE, processF)
                .addInfluence(processF, processD)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def r1 = new Reservation(processA, LAB_A)
        def r2 = new Reservation(processB, LAB_A)
        def r3 = new Reservation(processC, LAB_A)
        def r4 = new Reservation(processX, LAB_B)
        def r5 = new Reservation(processD, LAB_C)
        def r6 = new Reservation(processE, LAB_C)
        def r7 = new Reservation(processF, LAB_C)

        when:
        BridgingReservations bridging = new InfluenceAnalyzer(influenceMap)
                .identifyCriticalReservations(Set.of(r1, r2, r3, r4, r5, r6, r7))

        then:
        bridging.count() == 3
        bridging.isBridging(r4)
        bridging.isBridging(r1)
        bridging.isBridging(r5)
    }

    def "a fully connected group has no critical reservations"() {
        given:
        def processA = new PhysicsProcess("A")
        def processB = new PhysicsProcess("B")
        def processC = new PhysicsProcess("C")
        def physics = PhysicsInfluence.builder()
                .addInfluence(processA, processB)
                .addInfluence(processB, processC)
                .addInfluence(processC, processA)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def r1 = new Reservation(processA, LAB_A)
        def r2 = new Reservation(processB, LAB_B)
        def r3 = new Reservation(processC, LAB_C)

        when:
        BridgingReservations bridging = new InfluenceAnalyzer(influenceMap)
                .identifyCriticalReservations(Set.of(r1, r2, r3))

        then:
        bridging.isEmpty()
        bridging.count() == 0
    }

    def "a long chain has multiple critical reservations"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(PROCESS_A, PROCESS_B)
                .addInfluence(PROCESS_B, PROCESS_C)
                .addInfluence(PROCESS_C, PROCESS_D)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def r1 = new Reservation(PROCESS_A, LAB_A)
        def r2 = new Reservation(PROCESS_B, LAB_B)
        def r3 = new Reservation(PROCESS_C, LAB_C)
        def r4 = new Reservation(PROCESS_D, LAB_A)

        when:
        BridgingReservations bridging = new InfluenceAnalyzer(influenceMap)
                .identifyCriticalReservations(Set.of(r1, r2, r3, r4))

        then:
        bridging.count() == 2
        bridging.isBridging(r2)
        bridging.isBridging(r3)
    }

    def "an empty set has no critical reservations"() {
        given:
        def physics = PhysicsInfluence.builder().build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A))
                .build()

        when:
        BridgingReservations bridging = new InfluenceAnalyzer(influenceMap)
                .identifyCriticalReservations(Set.of())

        then:
        bridging.isEmpty()
        bridging.count() == 0
    }
}
