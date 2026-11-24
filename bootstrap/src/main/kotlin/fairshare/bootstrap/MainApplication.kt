package fairshare.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
class MainApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}
