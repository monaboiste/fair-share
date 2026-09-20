package com.github.monaboiste.fairshare.graphs.scenarios.influence

import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.CONDUCTIVITY
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.LAB_A
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.LAB_B
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.LAB_C
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.SPECTROSCOPY
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.THERMAL
import static com.github.monaboiste.fairshare.graphs.fixture.influence.Fixtures.emptyInfrastructure

import com.github.monaboiste.fairshare.graphs.fixture.influence.InfluenceMap
import com.github.monaboiste.fairshare.graphs.fixture.influence.InfluenceUnit
import com.github.monaboiste.fairshare.graphs.fixture.influence.InfrastructureInfluence
import com.github.monaboiste.fairshare.graphs.fixture.influence.Laboratory
import com.github.monaboiste.fairshare.graphs.fixture.influence.LaboratoryAdjacency
import com.github.monaboiste.fairshare.graphs.fixture.influence.PhysicsInfluence
import spock.lang.Specification

class InfluenceMapSpec extends Specification {

    def "creates an influence graph as the cartesian product of physics and laboratories"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(THERMAL, CONDUCTIVITY)
                .build()
        Set<Laboratory> laboratories = Set.of(LAB_A, LAB_B, LAB_C)
        def infrastructureInfluence = emptyInfrastructure()

        when:
        def influence = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(infrastructureInfluence)
                .withLaboratories(laboratories)
                .build()

        then:
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_A))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_C))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_A))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_C))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_C), new InfluenceUnit(CONDUCTIVITY, LAB_A))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_C), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_C), new InfluenceUnit(CONDUCTIVITY, LAB_C))
        influence.asGraph().edgeSet().size() == 9
    }

    def "combines the physics cartesian product with infrastructure constraints"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(THERMAL, CONDUCTIVITY)
                .build()
        Set<Laboratory> laboratories = Set.of(LAB_A, LAB_B)
        def infrastructureInfluence = InfrastructureInfluence.builder()
                .addConstraint(SPECTROSCOPY, LAB_A, THERMAL, LAB_B)
                .build()

        when:
        def influence = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(infrastructureInfluence)
                .withLaboratories(laboratories)
                .build()

        then:
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_A))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_A))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(SPECTROSCOPY, LAB_A), new InfluenceUnit(THERMAL, LAB_B))
        influence.asGraph().edgeSet().size() == 5
    }

    def "creates an influence graph based on laboratory adjacency"() {
        given:
        def physics = PhysicsInfluence.builder()
                .addInfluence(THERMAL, CONDUCTIVITY)
                .build()
        def adjacency = LaboratoryAdjacency.builder()
                .adjacent(LAB_A, LAB_B)
                .adjacent(LAB_B, LAB_C)
                .build()
        def infrastructureInfluence = emptyInfrastructure()

        when:
        def influence = InfluenceMap.builder()
                .withPhysics(physics)
                .withInfrastructure(infrastructureInfluence)
                .withLaboratoryAdjacency(adjacency)
                .build()

        then:
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_A), new InfluenceUnit(CONDUCTIVITY, LAB_B))
        influence.asGraph().containsEdge(new InfluenceUnit(THERMAL, LAB_B), new InfluenceUnit(CONDUCTIVITY, LAB_C))
        influence.asGraph().edgeSet().size() == 2
    }
}
