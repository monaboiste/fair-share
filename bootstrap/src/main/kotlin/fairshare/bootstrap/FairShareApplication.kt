package fairshare.bootstrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(proxyBeanMethods = false)
class FairShareApplication

@Suppress("SpreadOperator")
internal fun main(args: Array<String>) {
    SpringApplication(FairShareApplication::class.java).run(*args)
}
