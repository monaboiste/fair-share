package com.softwarearchetypes.graphs.fixture.userjourney

record Condition(ConditionType type, Map<String, Object> attributes) {

    enum ConditionType {
        LATE_PAYMENT,
        PAYMENT_ON_TIME,
        RESTRUCTURING,
        PROMOTION_APPROVED
    }

    static Condition of(ConditionType type) {
        return new Condition(type, new HashMap<>())
    }

    static Condition of(ConditionType type, Map<String, Object> attributes) {
        return new Condition(type, new HashMap<>(attributes))
    }

    static Condition latePayments(int count) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("counter", count)
        return new Condition(ConditionType.LATE_PAYMENT, attributes)
    }

    static Condition paymentOnTime() {
        return new Condition(ConditionType.PAYMENT_ON_TIME, new HashMap<>())
    }

    static Condition restructuring() {
        return new Condition(ConditionType.RESTRUCTURING, new HashMap<>())
    }

    static Condition promotionApproved() {
        return new Condition(ConditionType.PROMOTION_APPROVED, new HashMap<>())
    }

    static Condition withCost(ConditionType type, double cost) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("cost", cost)
        return new Condition(type, attributes)
    }

    static Condition withTime(ConditionType type, int timeInDays) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("time", timeInDays)
        return new Condition(type, attributes)
    }

    static Condition withRisk(ConditionType type, double riskScore) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("risk", riskScore)
        return new Condition(type, attributes)
    }

    static Condition withAttributes(ConditionType type, double cost, int time, double risk) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("cost", cost)
        attributes.put("time", time)
        attributes.put("risk", risk)
        return new Condition(type, attributes)
    }

    double getCost() {
        return (double) attributes.getOrDefault("cost", 1.0)
    }

    int getTime() {
        return (int) attributes.getOrDefault("time", 1)
    }

    double getRisk() {
        return (double) attributes.getOrDefault("risk", 0.0)
    }
}
