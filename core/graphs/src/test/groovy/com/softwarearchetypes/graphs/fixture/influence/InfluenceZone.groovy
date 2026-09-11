package com.softwarearchetypes.graphs.fixture.influence

record InfluenceZone(Set<Reservation> reservations) {
    InfluenceZone(Set<Reservation> reservations) {
        this.reservations = Set.copyOf(reservations)
    }

    int countReservationsToNegotiateWith(Reservation reservation) {
        if (!reservations.contains(reservation)) {
            return 0
        }
        return reservations.size() - 1
    }

    Set<Reservation> getReservationsToNegotiateWith(Reservation reservation) {
        if (!reservations.contains(reservation)) {
            return Set.of()
        }
        Set<Reservation> toNegotiate = new HashSet<>(reservations)
        toNegotiate.remove(reservation)
        return Set.copyOf(toNegotiate)
    }

    int size() {
        return reservations.size()
    }
}
