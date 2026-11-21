package com.shipstation.deliverymanager.ai

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@ApplyExtension(SpringExtension::class)
class ApplicationContextTest :
    BehaviorSpec({

        given("the application") {
            then("spring context starts successfully") {
            }
        }
    })
