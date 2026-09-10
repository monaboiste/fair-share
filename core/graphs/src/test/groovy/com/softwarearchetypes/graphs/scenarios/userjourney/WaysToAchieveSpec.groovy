package com.softwarearchetypes.graphs.scenarios.userjourney

import static com.softwarearchetypes.graphs.fixture.userjourney.Product.ProductType.DISCOUNT
import static com.softwarearchetypes.graphs.fixture.userjourney.Product.ProductType.PENALTY

import com.softwarearchetypes.graphs.fixture.userjourney.Condition
import com.softwarearchetypes.graphs.fixture.userjourney.CustomerPath
import com.softwarearchetypes.graphs.fixture.userjourney.Product
import com.softwarearchetypes.graphs.fixture.userjourney.State
import com.softwarearchetypes.graphs.fixture.userjourney.UserJourney
import com.softwarearchetypes.graphs.fixture.userjourney.UserJourneyId
import spock.lang.Specification

class WaysToAchieveSpec extends Specification {

    def "finds a simple path to a product"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State penalty = State.of(Product.penalty())
        Condition payOnTime6Times = Condition.latePayments(6)
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-1"))
                .from(newLoan).on(payOnTime6Times).goto_(penalty)
                .withCurrentState(newLoan)
                .build()

        when:
        Set<CustomerPath> paths = journey.waysToAchieve(PENALTY)

        then:
        paths.size() == 1
        CustomerPath path = paths.iterator().next()
        path.length() == 1
        path.conditions().contains(Condition.latePayments(6))
    }

    def "finds multiple paths to a discount"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State afterPayments = State.of(Product.penalty())
        State discount10 = State.of(Product.discount(10))
        Condition paymentOnTime = Condition.paymentOnTime()
        Condition latePayments = Condition.latePayments(3)
        Condition promotionApproved = Condition.promotionApproved()
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-4"))
                .from(newLoan).on(paymentOnTime).goto_(discount10)
                .from(newLoan).on(latePayments).goto_(afterPayments)
                .from(afterPayments).on(promotionApproved).goto_(discount10)
                .withCurrentState(newLoan)
                .build()

        when:
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT)

        then:
        paths.size() == 2
        containsPath(paths, paymentOnTime)
        containsPath(paths, latePayments, promotionApproved)
    }

    def "returns no paths when the requested product is not present"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State penalty = State.of(Product.penalty())
        Condition latePayment = Condition.latePayments(1)
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-5"))
                .from(newLoan).on(latePayment).goto_(penalty)
                .withCurrentState(newLoan)
                .build()

        when:
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT)

        then:
        paths.isEmpty()
    }

    def "returns no paths when the requested product is unreachable from the current state"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State penalty = State.of(Product.penalty())
        State discount10 = State.of(Product.discount(10))
        Condition latePayment = Condition.latePayments(1)
        Condition payOnTime = Condition.paymentOnTime()
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-7"))
                .from(newLoan).on(latePayment).goto_(penalty)
                .from(newLoan).on(payOnTime).goto_(discount10)
                .withCurrentState(penalty)
                .build()

        when:
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT)

        then:
        paths.isEmpty()
    }

    def "finds a complex path through multiple states"() {
        given:
        State newLoan = State.of(Product.newLoan())
        State afterPayment1 = State.of(Product.newLoan(), Product.penalty())
        State afterPayment2 = State.of(Product.penalty())
        State discount10 = State.of(Product.discount(10))
        Condition step1 = Condition.paymentOnTime()
        Condition step2 = Condition.latePayments(1)
        Condition step3 = Condition.promotionApproved()
        UserJourney journey = UserJourney.builder(UserJourneyId.of("user-9"))
                .from(newLoan).on(step1).goto_(afterPayment1)
                .from(afterPayment1).on(step2).goto_(afterPayment2)
                .from(afterPayment2).on(step3).goto_(discount10)
                .withCurrentState(newLoan)
                .build()

        when:
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT)

        then:
        paths.size() == 1
        paths.iterator().next().length() == 3
    }

    private static boolean containsPath(Set<CustomerPath> paths, Condition... expectedConditions) {
        paths.any { path -> path.conditions() == expectedConditions.toList() }
    }
}
