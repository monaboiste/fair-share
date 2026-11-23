package fairshare.bootstrap

import fairshare.application.group.GroupStore
import fairshare.application.group.GroupUseCases
import fairshare.infrastructure.persistence.SqliteGroupStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication(scanBasePackages = ["fairshare.bootstrap", "fairshare.infrastructure.http"])
internal class MainApplication {
    @Bean
    fun groupStore(@Value("\${fair-share.database-url:jdbc:sqlite:fair-share.db}") jdbcUrl: String): GroupStore =
        SqliteGroupStore(jdbcUrl)

    @Bean
    fun groupUseCases(store: GroupStore): GroupUseCases = GroupUseCases(store)
}

@Suppress("SpreadOperator")
internal fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}
