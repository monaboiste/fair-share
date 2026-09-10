package com.softwarearchetypes.rules.core

import com.softwarearchetypes.rules.core.selection.CandidateRule
import com.softwarearchetypes.rules.core.selection.RuleSelector
import spock.lang.Specification

class RuleCoreReusabilitySpec extends Specification {

    private static final ChangeApplicator<CreditApplication, Integer> LIMIT = [
            currentValue: { CreditApplication application -> application.limit() },
            applyChange : { CreditApplication application, Integer newLimit, String ignored ->
                new CreditApplication(application.segment(), newLimit)
            }
    ] as ChangeApplicator<CreditApplication, Integer>

    def "chain applies modifiers in order"() {
        given:
        def chain = new ChainModifier<CreditApplication>()
                .add(raiseLimitBy(500, 10_000))
                .add(raiseLimitBy(200, 10_000))

        expect:
        chain.modify(new CreditApplication("retail", 1000)).limit() == 1700
    }

    def "guardian rejects a forbidden result"() {
        given:
        Modifier<CreditApplication> tooGenerous = raiseLimitBy(5000, 3000)

        expect:
        tooGenerous.modify(new CreditApplication("retail", 1000)).limit() == 1000
    }

    def "selection uses only the applicant context"() {
        given:
        def rules = [
                new CandidateRule<>(raiseLimitBy(500, 10_000), { applicant -> applicant.yearsOfHistory() >= 3 }),
                new CandidateRule<>(raiseLimitBy(9000, 10_000), { applicant -> applicant.segment() == "vip" })
        ] as List<CandidateRule<CreditApplicant, CreditApplication>>

        when:
        Modifier<CreditApplication> selected = RuleSelector.select(new CreditApplicant("retail", 5), rules)

        then:
        selected.modify(new CreditApplication("retail", 1000)).limit() == 1500
    }

    def "identity modifier leaves the application unchanged"() {
        given:
        def application = new CreditApplication("retail", 1000)

        expect:
        new IdentityModifier<CreditApplication>().modify(application) == application
    }

    private static ConfigurableModifier<CreditApplication, Integer> raiseLimitBy(int amount, int maxLimit) {
        new ConfigurableModifier<>(
                "raise by $amount",
                { application -> application.limit() > 0 },
                { application -> application.limit() + amount },
                { application -> application.limit() <= maxLimit },
                LIMIT
        ) as ConfigurableModifier<CreditApplication, Integer>
    }

}

record CreditApplication(String segment, int limit) {}

record CreditApplicant(String segment, int yearsOfHistory) {}
