package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.pricing.component.Validity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class ExchangeRateVersions {

    private final List<ExchangeRateVersion> versions;

    private ExchangeRateVersions(List<ExchangeRateVersion> versions) {
        this.versions = versions;
    }

    public static ExchangeRateVersions from(List<ExchangeRateVersion> versions) {
        return new ExchangeRateVersions(new ArrayList<>(versions));
    }

    public static ExchangeRateVersions empty() {
        return new ExchangeRateVersions(new ArrayList<>());
    }

    public void append(ExchangeRateVersion version) {
        versions.add(version);
    }

    public Optional<ExchangeRateVersion> applicableAt(CurrencyUnit source, CurrencyUnit target, LocalDateTime at) {
        ExchangeRateVersion selected = null;
        Comparator<@Nullable LocalDateTime> validFrom = Comparator.nullsFirst(Comparator.naturalOrder());
        for (ExchangeRateVersion version : versions) {
            if (version.validity().isValidAt(at)
                    && version.exchangeRate().sourceCurrency().equals(source)
                    && version.exchangeRate().targetCurrency().equals(target)
                    && (selected == null
                            || validFrom.compare(
                                            version.validity().from(),
                                            selected.validity().from())
                                    >= 0)) {
                selected = version;
            }
        }
        return Optional.ofNullable(selected);
    }

    public Optional<ComponentVersionId> latestConfiguredFor(ExchangeRate exchangeRate, Validity validity) {
        for (int index = versions.size() - 1; index >= 0; index--) {
            ExchangeRateVersion previous = versions.get(index);
            ExchangeRate configured = previous.exchangeRate();
            if (configured.sourceCurrency().equals(exchangeRate.sourceCurrency())
                    && configured.targetCurrency().equals(exchangeRate.targetCurrency())
                    && previous.validity().equals(validity)) {
                if (configured.value().compareTo(exchangeRate.value()) == 0) {
                    return Optional.of(previous.id());
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
