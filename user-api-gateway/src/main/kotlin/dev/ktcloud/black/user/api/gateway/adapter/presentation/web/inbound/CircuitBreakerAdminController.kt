package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test/cb")
class CircuitBreakerAdminController(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    @PostMapping("/{name}/force-open")
    fun forceOpen(@PathVariable name: String): ResponseEntity<Map<String, String>> {
        val cb = circuitBreakerRegistry.circuitBreaker(name)
        cb.transitionToOpenState()
        return ResponseEntity.ok(mapOf("name" to name, "state" to cb.state.name))
    }

    @PostMapping("/{name}/force-closed")
    fun forceClosed(@PathVariable name: String): ResponseEntity<Map<String, String>> {
        val cb = circuitBreakerRegistry.circuitBreaker(name)
        cb.transitionToClosedState()
        return ResponseEntity.ok(mapOf("name" to name, "state" to cb.state.name))
    }
}
