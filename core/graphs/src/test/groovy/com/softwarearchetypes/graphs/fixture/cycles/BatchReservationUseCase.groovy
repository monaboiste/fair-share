package com.softwarearchetypes.graphs.fixture.cycles

import static java.util.stream.Collectors.toSet

import com.softwarearchetypes.graphs.Edge
import com.softwarearchetypes.graphs.Graph
import com.softwarearchetypes.graphs.Node
import java.util.stream.Stream

class BatchReservationUseCase {

    private final SlotRepository slotRepository

    BatchReservationUseCase(SlotRepository slotRepository) {
        this.slotRepository = slotRepository
    }

    BatchReservationResult execute(List<ReservationChangeRequest> requests) {
        Graph<SlotId, ReservationChangeRequest> graph = buildGraph(requests)
        return executeDependentRequests(findDependentRequests(graph))
    }

    BatchReservationResult execute(List<ReservationChangeRequest> requests, Eligibility eligibility) {
        Graph<OwnerId, ReservationChangeRequest> intersection =
                buildOwnerGraph(requests).intersection(eligibility.asGraph())
        return executeDependentRequests(findDependentRequests(intersection))
    }

    private BatchReservationResult executeDependentRequests(Set<ReservationChangeRequest> requests) {
        if (requests.isEmpty()) {
            return BatchReservationResult.none()
        }
        Map<SlotId, Slot> slots = loadAllSlots(requests)
        requests.forEach(request -> slots.get(request.fromSlot()).release())
        requests.forEach(request -> slots.get(request.toSlot()).assignTo(request.userId()))
        slotRepository.saveAll(slots.values())
        return BatchReservationResult.success(requests)
    }

    private Graph<SlotId, ReservationChangeRequest> buildGraph(List<ReservationChangeRequest> requests) {
        Graph<SlotId, ReservationChangeRequest> graph = new Graph<>()
        for (ReservationChangeRequest request : requests) {
            Node<SlotId> fromNode = new Node<>(request.fromSlot())
            Node<SlotId> toNode = new Node<>(request.toSlot())
            Edge<SlotId, ReservationChangeRequest> edge = new Edge<>(fromNode, toNode, request)
            graph.addEdge(edge)
        }
        return graph
    }

    private Map<SlotId, Slot> loadAllSlots(Set<ReservationChangeRequest> dependentRequests) {
        Set<SlotId> allSlotIds = dependentRequests.stream()
                .flatMap(r -> Stream.of(r.fromSlot(), r.toSlot()))
                .collect(toSet())
        return slotRepository.findAll(allSlotIds)
    }

    /**
     * Builds a graph whose edges connect each source slot's current owner to the corresponding target slot's current
     * owner.
     */
    private Graph<OwnerId, ReservationChangeRequest> buildOwnerGraph(List<ReservationChangeRequest> requests) {
        Graph<OwnerId, ReservationChangeRequest> graph = new Graph<>()

        Set<SlotId> allSlotIds = requests.stream()
                .flatMap(r -> Stream.of(r.fromSlot(), r.toSlot()))
                .collect(toSet())
        Map<SlotId, Slot> slots = slotRepository.findAll(allSlotIds)

        for (ReservationChangeRequest request : requests) {
            Slot fromSlot = slots.get(request.fromSlot())
            Slot toSlot = slots.get(request.toSlot())

            if (fromSlot != null && toSlot != null) {
                Node<OwnerId> fromOwner = new Node<>(fromSlot.getOwner())
                Node<OwnerId> toOwner = new Node<>(toSlot.getOwner())
                Edge<OwnerId, ReservationChangeRequest> edge = new Edge<>(fromOwner, toOwner, request)
                graph.addEdge(edge)
            }
        }

        return graph
    }

    private <T> Set<ReservationChangeRequest> findDependentRequests(Graph<T, ReservationChangeRequest> graph) {
        return graph.findFirstCycle()
                .map(path -> path.edges().stream().map(Edge::property).collect(toSet()))
                .orElseGet(Collections::emptySet) as Set<ReservationChangeRequest>
    }
}
