package com.softwarearchetypes.scoring.customer;

import com.softwarearchetypes.scoring.ast.Metric;

public final class CustomerMetrics {

    public static final Metric YEARLY_PURCHASE_AMOUNT = Metric.of("customer.yearly-purchase-amount");
    public static final Metric QUARTERLY_COMPLAINT_COUNT = Metric.of("customer.quarterly-complaint-count");
    public static final Metric LAST_PURCHASE_DAYS_AGO = Metric.of("customer.last-purchase-days-ago");

    private CustomerMetrics() {}
}
