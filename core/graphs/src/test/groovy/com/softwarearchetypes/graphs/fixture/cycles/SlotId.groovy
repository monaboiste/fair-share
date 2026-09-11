package com.softwarearchetypes.graphs.fixture.cycles

record SlotId(String value) {

    static SlotId of(String value) {
        return new SlotId(value)
    }
}
