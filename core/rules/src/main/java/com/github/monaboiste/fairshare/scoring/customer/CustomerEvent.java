package com.github.monaboiste.fairshare.scoring.customer;

import java.time.Instant;

public record CustomerEvent(String type, Instant occurredAt, double amount) {}
