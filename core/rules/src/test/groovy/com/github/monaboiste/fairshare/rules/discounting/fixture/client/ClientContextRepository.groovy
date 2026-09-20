package com.github.monaboiste.fairshare.rules.discounting.fixture.client

interface ClientContextRepository {

    ClientContext loadClientContext(UUID clientId)
}
