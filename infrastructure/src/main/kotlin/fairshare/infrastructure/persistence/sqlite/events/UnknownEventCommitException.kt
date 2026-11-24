package fairshare.infrastructure.persistence.sqlite.events

import java.sql.SQLException

internal class UnknownEventCommitException(message: String, cause: SQLException) : RuntimeException(message, cause)
