package com.softwarearchetypes.rules.discounting.fixture.client

interface ClientContextRepository {

    ClientContext loadClientContext(UUID clientId)
}
