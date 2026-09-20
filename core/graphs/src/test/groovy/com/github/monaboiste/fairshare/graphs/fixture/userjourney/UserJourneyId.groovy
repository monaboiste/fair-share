package com.github.monaboiste.fairshare.graphs.fixture.userjourney

record UserJourneyId(String value) {

    static UserJourneyId of(String value) {
        return new UserJourneyId(value)
    }
}
