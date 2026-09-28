package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.pricing.component.Validity;
import java.time.LocalDateTime;

public record ExchangeRateVersion(
        ComponentVersionId id, ExchangeRate exchangeRate, Validity validity, LocalDateTime definedAt) {}
