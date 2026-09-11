package com.softwarearchetypes.graphs.scenarios.cycles

import com.softwarearchetypes.graphs.fixture.cycles.BatchReservationResult
import com.softwarearchetypes.graphs.fixture.cycles.BatchReservationUseCase
import com.softwarearchetypes.graphs.fixture.cycles.InMemorySlotRepository
import com.softwarearchetypes.graphs.fixture.cycles.OwnerId
import com.softwarearchetypes.graphs.fixture.cycles.ReservationChangeRequest
import com.softwarearchetypes.graphs.fixture.cycles.Slot
import com.softwarearchetypes.graphs.fixture.cycles.SlotId
import com.softwarearchetypes.graphs.fixture.cycles.SlotRepository
import spock.lang.Specification

class BatchReservationUseCaseSpec extends Specification {

    SlotRepository slotRepository = new InMemorySlotRepository()
    BatchReservationUseCase batchReservationUseCase = new BatchReservationUseCase(slotRepository)

    def "executes dependent reservation changes when slots remain valid"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        OwnerId userX = OwnerId.of("UserX")
        OwnerId userY = OwnerId.of("UserY")
        slotOwnedBy(slotA, userX)
        slotOwnedBy(slotB, userY)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userX),
                new ReservationChangeRequest(slotB, slotA, userY)
        ])

        then:
        result.status() == BatchReservationResult.Status.SUCCESS
        result.executedRequests().size() == 2
        findSlotOwner(slotA) == userY
        findSlotOwner(slotB) == userX
    }

    def "executes dependent reservation changes and skips invalid ones"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        SlotId slotD = SlotId.of("SlotD")
        OwnerId userX = OwnerId.of("UserX")
        OwnerId userY = OwnerId.of("UserY")
        OwnerId userZ = OwnerId.of("UserZ")
        slotOwnedBy(slotA, userX)
        slotOwnedBy(slotB, userY)
        freeSlot(slotD)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotB, slotA, userY),
                new ReservationChangeRequest(slotA, slotB, userX),
                new ReservationChangeRequest(slotD, slotA, userZ)
        ])

        then:
        result.status() == BatchReservationResult.Status.SUCCESS
        result.executedRequests().size() == 2
        findSlotOwner(slotA) == userY
        findSlotOwner(slotB) == userX
        findSlotOwner(slotD) == OwnerId.empty()
    }

    def "executes a complex dependent reservation change cycle across multiple slots"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        SlotId slotC = SlotId.of("SlotC")
        SlotId slotD = SlotId.of("SlotD")
        SlotId slotE = SlotId.of("SlotE")
        OwnerId userAlice = OwnerId.of("Alice")
        OwnerId userBob = OwnerId.of("Bob")
        OwnerId userCharlie = OwnerId.of("Charlie")
        OwnerId userDiana = OwnerId.of("Diana")
        OwnerId userEve = OwnerId.of("Eve")
        slotOwnedBy(slotA, userAlice)
        slotOwnedBy(slotB, userBob)
        slotOwnedBy(slotC, userCharlie)
        slotOwnedBy(slotD, userDiana)
        slotOwnedBy(slotE, userEve)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userAlice),
                new ReservationChangeRequest(slotB, slotC, userBob),
                new ReservationChangeRequest(slotC, slotD, userCharlie),
                new ReservationChangeRequest(slotD, slotE, userDiana),
                new ReservationChangeRequest(slotE, slotA, userEve)
        ])

        then:
        result.status() == BatchReservationResult.Status.SUCCESS
        result.executedRequests().size() == 5
        findSlotOwner(slotA) == userEve
        findSlotOwner(slotB) == userAlice
        findSlotOwner(slotC) == userBob
        findSlotOwner(slotD) == userCharlie
        findSlotOwner(slotE) == userDiana
    }

    def "returns failure when no cycle exists"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        OwnerId userX = OwnerId.of("UserX")
        slotOwnedBy(slotA, userX)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userX)
        ])

        then:
        result.status() == BatchReservationResult.Status.FAILURE
        result.executedRequests().size() == 0
        findSlotOwner(slotA) == userX
    }

    private Slot slotOwnedBy(SlotId slotId, OwnerId owner) {
        Slot slot = Slot.create(slotId, owner)
        slotRepository.save(slot)
        slot
    }

    private Slot freeSlot(SlotId slotId) {
        Slot slot = Slot.create(slotId, OwnerId.empty())
        slotRepository.save(slot)
        slot
    }

    private OwnerId findSlotOwner(SlotId slotId) {
        slotRepository.findById(slotId).orElseThrow().getOwner()
    }
}
