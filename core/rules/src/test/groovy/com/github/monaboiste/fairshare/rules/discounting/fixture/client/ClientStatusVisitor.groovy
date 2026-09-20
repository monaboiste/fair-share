package com.github.monaboiste.fairshare.rules.discounting.fixture.client

interface ClientStatusVisitor<R> {

    R visitStandard()

    R visitVIP()

    R visitGold()
}
