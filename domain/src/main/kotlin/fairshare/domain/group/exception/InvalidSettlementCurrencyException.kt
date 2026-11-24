package fairshare.domain.group.exception

public class InvalidSettlementCurrencyException(cause: Throwable? = null) :
    IllegalArgumentException("Settlement currency must be a supported ISO 4217 currency with minor units", cause)
