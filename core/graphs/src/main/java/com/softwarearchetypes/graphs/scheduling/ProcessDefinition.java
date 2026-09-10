package com.softwarearchetypes.graphs.scheduling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

record ProcessDefinition(
        Set<ProcessStep> steps,
        Graph<ProcessStep, DefaultEdge> dependencyGraph,
        Map<EdgeKey, DependencyType> edgeDependencyTypes) {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private final Set<ProcessStep> steps = new HashSet<>();
        private final DirectedAcyclicGraph<ProcessStep, DefaultEdge> graph =
                new DirectedAcyclicGraph<>(DefaultEdge.class);
        private final Map<EdgeKey, DependencyType> edgeDependencyTypes = new HashMap<>();

        Builder addStep(ProcessStep step) {
            steps.add(step);
            graph.addVertex(step);
            return this;
        }

        Builder addDependency(ProcessStep from, ProcessStep to) {
            steps.add(from);
            steps.add(to);
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            return this;
        }

        Builder addDependency(ProcessStep from, ProcessStep to, DependencyType dependencyType) {
            addDependency(from, to);
            edgeDependencyTypes.put(new EdgeKey(from, to), dependencyType);
            return this;
        }

        Schedule build() {
            ProcessDefinition processDefinition = new ProcessDefinition(steps, graph, edgeDependencyTypes);
            return calculateSchedule(processDefinition);
        }

        private Schedule calculateSchedule(ProcessDefinition processDefinition) {
            List<ProcessStep> scheduleSteps = new ArrayList<>();
            TopologicalOrderIterator<ProcessStep, DefaultEdge> iterator =
                    new TopologicalOrderIterator<>(processDefinition.dependencyGraph);

            while (iterator.hasNext()) {
                scheduleSteps.add(iterator.next());
            }

            return new Schedule(scheduleSteps);
        }
    }

    private record EdgeKey(ProcessStep from, ProcessStep to) {}
}
