package com.softwarearchetypes.graphs.scenarios.cycles

import com.softwarearchetypes.graphs.fixture.cycles.BatchReservationResult
import com.softwarearchetypes.graphs.fixture.cycles.BatchReservationUseCase
import com.softwarearchetypes.graphs.fixture.cycles.Eligibility
import com.softwarearchetypes.graphs.fixture.cycles.InMemorySlotRepository
import com.softwarearchetypes.graphs.fixture.cycles.OwnerId
import com.softwarearchetypes.graphs.fixture.cycles.ReservationChangeRequest
import com.softwarearchetypes.graphs.fixture.cycles.Slot
import com.softwarearchetypes.graphs.fixture.cycles.SlotId
import com.softwarearchetypes.graphs.fixture.cycles.SlotRepository
import spock.lang.Specification

class BatchReservationEligibilityUseCaseSpec extends Specification {

    SlotRepository slotRepository = new InMemorySlotRepository()
    BatchReservationUseCase batchReservationUseCase = new BatchReservationUseCase(slotRepository)

    def "executes a reservation change when all users are eligible"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        OwnerId userX = OwnerId.of("UserX")
        OwnerId userY = OwnerId.of("UserY")
        slotOwnedBy(slotA, userX)
        slotOwnedBy(slotB, userY)
        Eligibility eligibility = new Eligibility()
        eligibility.markTransferEligible(userX, userY)
        eligibility.markTransferEligible(userY, userX)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userX),
                new ReservationChangeRequest(slotB, slotA, userY)
        ], eligibility)

        then:
        result.status() == BatchReservationResult.Status.SUCCESS
        result.executedRequests().size() == 2
        findSlotOwner(slotA) == userY
        findSlotOwner(slotB) == userX
    }

    def "does not execute a reservation change when one user is not eligible"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        OwnerId userX = OwnerId.of("UserX")
        OwnerId userY = OwnerId.of("UserY")
        slotOwnedBy(slotA, userX)
        slotOwnedBy(slotB, userY)
        Eligibility eligibility = new Eligibility()
        eligibility.markTransferEligible(userX, userY)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userX),
                new ReservationChangeRequest(slotB, slotA, userY)
        ], eligibility)

        then:
        result.status() == BatchReservationResult.Status.FAILURE
        result.executedRequests().size() == 0
        findSlotOwner(slotA) == userX
        findSlotOwner(slotB) == userY
    }

    def "executes a long reservation change cycle when all users are eligible"() {
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
        Eligibility eligibility = new Eligibility()
        eligibility.markTransferEligible(userAlice, userBob)
        eligibility.markTransferEligible(userBob, userCharlie)
        eligibility.markTransferEligible(userCharlie, userDiana)
        eligibility.markTransferEligible(userDiana, userEve)
        eligibility.markTransferEligible(userEve, userAlice)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userAlice),
                new ReservationChangeRequest(slotB, slotC, userBob),
                new ReservationChangeRequest(slotC, slotD, userCharlie),
                new ReservationChangeRequest(slotD, slotE, userDiana),
                new ReservationChangeRequest(slotE, slotA, userEve)
        ], eligibility)

        then:
        result.status() == BatchReservationResult.Status.SUCCESS
        result.executedRequests().size() == 5
        findSlotOwner(slotA) == userEve
        findSlotOwner(slotB) == userAlice
        findSlotOwner(slotC) == userBob
        findSlotOwner(slotD) == userCharlie
        findSlotOwner(slotE) == userDiana
    }

    def "does not execute a cycle when one edge is ineligible"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        SlotId slotC = SlotId.of("SlotC")
        OwnerId userAlice = OwnerId.of("Alice")
        OwnerId userBob = OwnerId.of("Bob")
        OwnerId userCharlie = OwnerId.of("Charlie")
        slotOwnedBy(slotA, userAlice)
        slotOwnedBy(slotB, userBob)
        slotOwnedBy(slotC, userCharlie)
        Eligibility eligibility = new Eligibility()
        eligibility.markTransferEligible(userAlice, userBob)
        eligibility.markTransferEligible(userBob, userCharlie)

        when:
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userAlice),
                new ReservationChangeRequest(slotB, slotC, userBob),
                new ReservationChangeRequest(slotC, slotA, userCharlie)
        ], eligibility)

        then:
        result.status() == BatchReservationResult.Status.FAILURE
        result.executedRequests().size() == 0
        findSlotOwner(slotA) == userAlice
        findSlotOwner(slotB) == userBob
        findSlotOwner(slotC) == userCharlie
    }

    def "does not execute a cycle after eligibility is revoked"() {
        given:
        SlotId slotA = SlotId.of("SlotA")
        SlotId slotB = SlotId.of("SlotB")
        OwnerId userX = OwnerId.of("UserX")
        OwnerId userY = OwnerId.of("UserY")
        slotOwnedBy(slotA, userX)
        slotOwnedBy(slotB, userY)
        Eligibility eligibility = new Eligibility()
        eligibility.markTransferEligible(userX, userY)
        eligibility.markTransferEligible(userY, userX)

        when:
        eligibility.markTransferIneligible(userY, userX)
        BatchReservationResult result = batchReservationUseCase.execute([
                new ReservationChangeRequest(slotA, slotB, userX),
                new ReservationChangeRequest(slotB, slotA, userY)
        ], eligibility)

        then:
        result.status() == BatchReservationResult.Status.FAILURE
        result.executedRequests().size() == 0
        findSlotOwner(slotA) == userX
        findSlotOwner(slotB) == userY
    }

    private Slot slotOwnedBy(SlotId slotId, OwnerId owner) {
        Slot slot = Slot.create(slotId, owner)
        slotRepository.save(slot)
        slot
    }

    private OwnerId findSlotOwner(SlotId slotId) {
        slotRepository.findById(slotId).orElseThrow().getOwner()
    }
}
