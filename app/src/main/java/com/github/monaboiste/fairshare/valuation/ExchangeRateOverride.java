package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import java.time.LocalDateTime;

public record ExchangeRateOverride(ExchangeRate rate, ComponentVersionId versionId, LocalDateTime definedAt) {}
