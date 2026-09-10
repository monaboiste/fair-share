package com.softwarearchetypes.graphs.fixture.influence

record BridgingReservations(Set<Reservation> reservations) {
    BridgingReservations(Set<Reservation> reservations) {
        this.reservations = Set.copyOf(reservations)
    }

    boolean isBridging(Reservation reservation) {
        return reservations.contains(reservation)
    }

    int count() {
        return reservations.size()
    }

    boolean isEmpty() {
        return reservations.isEmpty()
    }
}
