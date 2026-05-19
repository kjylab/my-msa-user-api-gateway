package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class JwtAuthFilter(private val jwtTokenParser: JwtTokenParser) : WebFilter {

    companion object {
        private val PERMIT_PATHS = listOf(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/",
            "/healthz",
            "/actuator",
            "/prometheus",
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        if (PERMIT_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val token = exchange.request.headers.getFirst("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
            ?: return unauthorized(exchange, "인증이 필요합니다.")

        return try {
            val claims = jwtTokenParser.parse(token)
            val mutated = exchange.mutate().request(
                exchange.request.mutate()
                    .header("X-User-Id", claims.subject)
                    .header("X-User-Email", claims["email"] as? String ?: "")
                    .header("X-User-Role", claims["role"] as? String ?: "")
                    .header("X-User-Name", claims["name"] as? String ?: "")
                    .build()
            ).build()
            chain.filter(mutated)
        } catch (e: ExpiredJwtException) {
            unauthorized(exchange, "토큰이 만료되었습니다.")
        } catch (e: JwtException) {
            unauthorized(exchange, "유효하지 않은 토큰입니다.")
        }
    }

    private fun unauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = """{"message":"$message"}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}
