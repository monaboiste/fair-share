package com.softwarearchetypes.rules.discounting.fixture.client

enum ClientStatus {

    STANDARD{
        @Override
        <R> R accept(ClientStatusVisitor<R> visitor) {
            visitor.visitStandard()
        }
    },

    VIP{
        @Override
        <R> R accept(ClientStatusVisitor<R> visitor) {
            visitor.visitVIP()
        }
    },

    GOLD{
        @Override
        <R> R accept(ClientStatusVisitor<R> visitor) {
            visitor.visitGold()
        }
    }

    abstract <R> R accept(ClientStatusVisitor<R> visitor)
}
