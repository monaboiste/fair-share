package com.softwarearchetypes.graphs.scenarios.influence

import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.CONDUCTIVITY
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_A
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_B
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.LAB_C
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.SPECTROSCOPY
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.THERMAL
import static com.softwarearchetypes.graphs.fixture.influence.Fixtures.emptyInfrastructure

import com.softwarearchetypes.graphs.fixture.influence.InfluenceAnalyzer
import com.softwarearchetypes.graphs.fixture.influence.InfluenceMap
import com.softwarearchetypes.graphs.fixture.influence.InfluenceZone
import com.softwarearchetypes.graphs.fixture.influence.LaboratoryAdjacency
import com.softwarearchetypes.graphs.fixture.influence.PhysicsInfluence
import com.softwarearchetypes.graphs.fixture.influence.PhysicsProcess
import com.softwarearchetypes.graphs.fixture.influence.Reservation
import spock.lang.Specification

class InfluenceAnalyzerSpec extends Specification {

    def "counts one direct conflict"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(THERMAL, CONDUCTIVITY)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B))
                .build()
        def existing = new Reservation(CONDUCTIVITY, LAB_B)
        def newReservation = new Reservation(THERMAL, LAB_A)

        when:
        def conflicts = new InfluenceAnalyzer(influenceMap).countConflicts(newReservation, Set.of(existing))

        then:
        conflicts == 1
    }

    def "counts multiple direct conflicts"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(THERMAL, CONDUCTIVITY)
                .addInfluence(THERMAL, SPECTROSCOPY)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def existing1 = new Reservation(CONDUCTIVITY, LAB_B)
        def existing2 = new Reservation(SPECTROSCOPY, LAB_C)
        def newReservation = new Reservation(THERMAL, LAB_A)

        when:
        def conflicts = new InfluenceAnalyzer(influenceMap).countConflicts(newReservation, Set.of(existing1, existing2))

        then:
        conflicts == 2
    }

    def "counts no direct conflicts when there is no influence"() {
        given:
        def physics = PhysicsInfluence.builder().build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B))
                .build()
        def existing = new Reservation(SPECTROSCOPY, LAB_B)
        def newReservation = new Reservation(THERMAL, LAB_A)

        when:
        def conflicts = new InfluenceAnalyzer(influenceMap).countConflicts(newReservation, Set.of(existing))

        then:
        conflicts == 0
    }

    def "new reservation merges two influence zones into one"() {
        given:
        def processA = new PhysicsProcess("A")
        def processB = new PhysicsProcess("B")
        def processX = new PhysicsProcess("X")
        def processC = new PhysicsProcess("C")
        def processD = new PhysicsProcess("D")
        def physics = PhysicsInfluence.builder()
                .addInfluence(processA, processB)
                .addInfluence(processC, processD)
                .addInfluence(processB, processX)
                .addInfluence(processX, processC)
                .build()
        def adjacency = LaboratoryAdjacency.builder()
                .adjacent(LAB_A, LAB_A)
                .adjacent(LAB_B, LAB_B)
                .adjacent(LAB_A, LAB_C)
                .adjacent(LAB_C, LAB_B)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratoryAdjacency(adjacency)
                .build()
        def existing1 = new Reservation(processA, LAB_A)
        def existing2 = new Reservation(processB, LAB_A)
        def existing3 = new Reservation(processC, LAB_B)
        def existing4 = new Reservation(processD, LAB_B)
        def newReservation = new Reservation(processX, LAB_C)

        when:
        def analyzer = new InfluenceAnalyzer(influenceMap)
        Set<InfluenceZone> zonesBefore = analyzer.analyzeInfluenceZones(Set.of(existing1, existing2, existing3, existing4))
        InfluenceZone zoneAfter = analyzer.findInfluenceZone(newReservation, Set.of(existing1, existing2, existing3, existing4, newReservation))

        then:
        zonesBefore.size() == 2
        zoneAfter.size() == 5
        zoneAfter.countReservationsToNegotiateWith(newReservation) == 4
        zoneAfter.getReservationsToNegotiateWith(newReservation) == Set.of(existing1, existing2, existing3, existing4)
    }

    def "new reservation adds a third independent influence zone"() {
        given:
        def processA = new PhysicsProcess("A")
        def processB = new PhysicsProcess("B")
        def processC = new PhysicsProcess("C")
        def processD = new PhysicsProcess("D")
        def processE = new PhysicsProcess("E")
        def physics = PhysicsInfluence.builder()
                .addInfluence(processA, processB)
                .addInfluence(processC, processD)
                .build()
        def influenceMap = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(emptyInfrastructure())
                .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                .build()
        def existing1 = new Reservation(processA, LAB_A)
        def existing2 = new Reservation(processB, LAB_A)
        def existing3 = new Reservation(processC, LAB_B)
        def existing4 = new Reservation(processD, LAB_B)
        def newReservation = new Reservation(processE, LAB_C)

        when:
        def analyzer = new InfluenceAnalyzer(influenceMap)
        Set<InfluenceZone> zonesBefore = analyzer.analyzeInfluenceZones(Set.of(existing1, existing2, existing3, existing4))
        Set<InfluenceZone> zonesAfter = analyzer.analyzeInfluenceZones(Set.of(existing1, existing2, existing3, existing4, newReservation))
        InfluenceZone independentZone = analyzer.findInfluenceZone(newReservation, Set.of(existing1, existing2, existing3, existing4, newReservation))

        then:
        zonesBefore.size() == 2
        zonesAfter.size() == 3
        independentZone.countReservationsToNegotiateWith(newReservation) == 0
        independentZone.size() == 1
    }
}
