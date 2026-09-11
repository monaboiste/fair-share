package com.softwarearchetypes.graphs


import spock.lang.Specification

class GraphSpec extends Specification {

    def "intersection contains only common edges"() {
        given:
        Graph<String, String> graph1 = new Graph<>()
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"))
        Graph<String, String> graph2 = new Graph<>()
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))

        when:
        Graph<String, String> intersection = graph1.intersection(graph2)

        then:
        intersection.hasEdge(new Node<>("A"), new Node<>("B"))
        intersection.hasEdge(new Node<>("B"), new Node<>("C"))
        !intersection.hasEdge(new Node<>("C"), new Node<>("A"))
    }

    def "intersection finds a cycle only when all edges are common"() {
        given:
        Graph<String, String> graph1 = new Graph<>()
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"))
        Graph<String, String> graph2 = new Graph<>()
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))
        graph2.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"))

        when:
        Graph<String, String> intersection = graph1.intersection(graph2)

        then:
        def cycle = intersection.findFirstCycle()
        cycle.isPresent()
        cycle.get().edges().size() == 3
    }

    def "intersection does not find a cycle when an edge is missing from the second graph"() {
        given:
        Graph<String, String> graph1 = new Graph<>()
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"))
        Graph<String, String> graph2 = new Graph<>()
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))

        when:
        Graph<String, String> intersection = graph1.intersection(graph2)

        then:
        !intersection.findFirstCycle().isPresent()
    }

    def "intersection of empty graphs is empty"() {
        given:
        Graph<String, String> graph1 = new Graph<>()
        Graph<String, String> graph2 = new Graph<>()

        when:
        Graph<String, String> intersection = graph1.intersection(graph2)

        then:
        !intersection.findFirstCycle().isPresent()
    }

    def "intersection with an empty graph is empty"() {
        given:
        Graph<String, String> graph1 = new Graph<>()
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))
        Graph<String, String> graph2 = new Graph<>()

        when:
        Graph<String, String> intersection = graph1.intersection(graph2)

        then:
        !intersection.hasEdge(new Node<>("A"), new Node<>("B"))
        !intersection.hasEdge(new Node<>("B"), new Node<>("C"))
    }
}
