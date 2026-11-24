package fairshare.infrastructure.rest

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ProblemDocumentation {
    @GetMapping("/problems/{slug}", produces = [TEXT_PLAIN_VALUE])
    fun describe(@PathVariable slug: String): ResponseEntity<String> {
        val type = ProblemType.entries.firstOrNull { it.slug == slug } ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity("${type.status.value()} ${type.title}\n\n${type.detail}\n", HttpStatus.OK)
    }
}
