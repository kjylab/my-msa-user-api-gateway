package dev.ktcloud.black.user.api.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["dev.ktcloud.black"])
class UserApiGatewayApplication


fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<UserApiGatewayApplication>(*args)
}