package com.softwarearchetypes.graphs.scenarios.userjourney

import static com.softwarearchetypes.graphs.fixture.userjourney.Condition.ConditionType.LATE_PAYMENT
import static com.softwarearchetypes.graphs.fixture.userjourney.Condition.ConditionType.PAYMENT_ON_TIME
import static com.softwarearchetypes.graphs.fixture.userjourney.Condition.ConditionType.PROMOTION_APPROVED
import static com.softwarearchetypes.graphs.fixture.userjourney.Condition.ConditionType.RESTRUCTURING
import static com.softwarearchetypes.graphs.fixture.userjourney.Product.ProductType.DISCOUNT

import com.softwarearchetypes.graphs.fixture.userjourney.Condition
import com.softwarearchetypes.graphs.fixture.userjourney.CustomerPath
import com.softwarearchetypes.graphs.fixture.userjourney.Product
import com.softwarearchetypes.graphs.fixture.userjourney.State
import com.softwarearchetypes.graphs.fixture.userjourney.UserJourney
import com.softwarearchetypes.graphs.fixture.userjourney.UserJourneyId
import spock.lang.Specification

class WeightedPathsSpec extends Specification {

    def "finds the cheapest path by minimizing cost"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State intermediate = State.of(Product.penalty())
        State discount = State.of(Product.discount(10))
        Condition directExpensive = Condition.withCost(PAYMENT_ON_TIME, 100.0)
        Condition cheapStep1 = Condition.withCost(LATE_PAYMENT, 30.0)
        Condition cheapStep2 = Condition.withCost(RESTRUCTURING, 20.0)
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-1"))
                .from(newLoan).on(directExpensive).goto_(discount)
                .from(newLoan).on(cheapStep1).goto_(intermediate)
                .from(intermediate).on(cheapStep2).goto_(discount)
                .withCurrentState(newLoan)
                .build()

        when:
        def cheapestPath = journey.optimizedWayToAchieve(DISCOUNT, { condition -> condition.getCost() })

        then:
        cheapestPath.isPresent()
        CustomerPath path = cheapestPath.get()
        path.length() == 2
        path.conditions().contains(cheapStep1)
        path.conditions().contains(cheapStep2)
    }

    def "finds the fastest path by minimizing time"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State intermediate1 = State.of(Product.penalty())
        State intermediate2 = State.of(Product.newLoan(), Product.penalty())
        State discount = State.of(Product.discount(10))
        Condition fastPath = Condition.withTime(PAYMENT_ON_TIME, 5)
        Condition slowStep1 = Condition.withTime(LATE_PAYMENT, 15)
        Condition slowStep2 = Condition.withTime(RESTRUCTURING, 10)
        Condition mediumStep1 = Condition.withTime(PROMOTION_APPROVED, 7)
        Condition mediumStep2 = Condition.withTime(PAYMENT_ON_TIME, 4)
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-2"))
                .from(newLoan).on(fastPath).goto_(discount)
                .from(newLoan).on(slowStep1).goto_(intermediate1)
                .from(intermediate1).on(slowStep2).goto_(discount)
                .from(newLoan).on(mediumStep1).goto_(intermediate2)
                .from(intermediate2).on(mediumStep2).goto_(discount)
                .withCurrentState(newLoan)
                .build()

        when:
        def fastestPath = journey.optimizedWayToAchieve(DISCOUNT, { condition -> condition.getTime() as double })

        then:
        fastestPath.isPresent()
        CustomerPath path = fastestPath.get()
        path.length() == 1
        path.conditions().contains(fastPath)
    }

    def "demonstrates the tradeoff between cost and time"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State expressRoute = State.of(Product.penalty())
        State economyRoute = State.of(Product.discount(5))
        State discount10 = State.of(Product.discount(10))
        Condition expressStep1 = Condition.withAttributes(PAYMENT_ON_TIME, 150.0, 3, 0.0)
        Condition economyStep1 = Condition.withAttributes(LATE_PAYMENT, 20.0, 30, 0.0)
        Condition toDiscount1 = Condition.withAttributes(PROMOTION_APPROVED, 10.0, 1, 0.0)
        Condition toDiscount2 = Condition.withAttributes(RESTRUCTURING, 10.0, 1, 0.0)
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-4"))
                .from(newLoan).on(expressStep1).goto_(expressRoute)
                .from(expressRoute).on(toDiscount1).goto_(discount10)
                .from(newLoan).on(economyStep1).goto_(economyRoute)
                .from(economyRoute).on(toDiscount2).goto_(discount10)
                .withCurrentState(newLoan)
                .build()

        when:
        def cheapest = journey.optimizedWayToAchieve(DISCOUNT, { condition -> condition.getCost() })
        def fastest = journey.optimizedWayToAchieve(DISCOUNT, { condition -> condition.getTime() as double })

        then:
        cheapest.isPresent()
        fastest.isPresent()
        cheapest.get().conditions().contains(economyStep1)
        fastest.get().conditions().contains(expressStep1)
    }
}
