package fairshare.domain.validation

/** An expected domain input failure. The message is diagnostic, not intended for display to users. */
public class InvalidInputException(
    /** Machine-readable reason for rejecting the input. */
    public val code: ValidationErrorCode,
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
