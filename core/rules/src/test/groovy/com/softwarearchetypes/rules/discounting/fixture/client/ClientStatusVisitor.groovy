package com.softwarearchetypes.rules.discounting.fixture.client

interface ClientStatusVisitor<R> {

    R visitStandard()

    R visitVIP()

    R visitGold()
}
