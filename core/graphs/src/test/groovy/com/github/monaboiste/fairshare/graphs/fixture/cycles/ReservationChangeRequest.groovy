package com.github.monaboiste.fairshare.graphs.fixture.cycles

record ReservationChangeRequest(SlotId fromSlot, SlotId toSlot, OwnerId userId) {}
