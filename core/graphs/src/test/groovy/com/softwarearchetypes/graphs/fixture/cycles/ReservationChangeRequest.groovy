package com.softwarearchetypes.graphs.fixture.cycles

record ReservationChangeRequest(SlotId fromSlot, SlotId toSlot, OwnerId userId) {}
