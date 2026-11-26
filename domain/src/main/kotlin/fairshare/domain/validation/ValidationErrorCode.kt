package fairshare.domain.validation

/** Identifies a domain input validation failure independently of its diagnostic message. */
public enum class ValidationErrorCode(
    /** Stable error identifier. */
    public val id: String,
) {
    /** Currency code is unknown, malformed, or has no defined minor units. */
    UNSUPPORTED_CURRENCY("error.validation.unsupported-currency"),

    /** Amount has more fractional digits than the currency allows. */
    EXCESS_PRECISION("error.validation.excess-precision"),
}
