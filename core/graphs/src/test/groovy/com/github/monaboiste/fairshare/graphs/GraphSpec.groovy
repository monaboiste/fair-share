package com.github.monaboiste.fairshare.graphs


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

    def "vertices contain every edge endpoint"() {
        given:
        Graph<String, String> graph = new Graph<>()
        graph.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"))
        graph.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"))

        when:
        Set<Node<String>> vertices = graph.vertices()

        then:
        vertices == [new Node<>("A"), new Node<>("B"), new Node<>("C")] as Set
    }

    def "edges list every added edge"() {
        given:
        Graph<String, String> graph = new Graph<>()
        Edge<String, String> first = new Edge<>(new Node<>("A"), new Node<>("B"), "edge1")
        Edge<String, String> second = new Edge<>(new Node<>("B"), new Node<>("C"), "edge2")
        graph.addEdge(first)
        graph.addEdge(second)

        when:
        List<Edge<String, String>> edges = graph.edges()

        then:
        edges.containsAll([first, second])
        edges.size() == 2
    }

    def "empty graph has no vertices or edges"() {
        given:
        Graph<String, String> graph = new Graph<>()

        expect:
        graph.vertices().isEmpty()
        graph.edges().isEmpty()
    }

    def "addVertex adds an isolated vertex with no edges"() {
        given:
        Graph<String, String> graph = new Graph<>()

        when:
        graph.addVertex(new Node<>("A"))

        then:
        graph.vertices() == [new Node<>("A")] as Set
        graph.edges().isEmpty()
    }

    def "addVertex is idempotent and keeps existing edges"() {
        given:
        Graph<String, String> graph = new Graph<>()
        Edge<String, String> edge = new Edge<>(new Node<>("A"), new Node<>("B"), "edge")
        graph.addEdge(edge)

        when:
        graph.addVertex(new Node<>("A"))
        graph.addVertex(new Node<>("C"))

        then:
        graph.vertices() == [new Node<>("A"), new Node<>("B"), new Node<>("C")] as Set
        graph.edges() == [edge]
    }
}
