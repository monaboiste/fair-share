package fairshare.domain.money

internal fun <T : Any> jodaValue(value: T?): T = checkNotNull(value) { "Joda-Money unexpectedly returned null" }
