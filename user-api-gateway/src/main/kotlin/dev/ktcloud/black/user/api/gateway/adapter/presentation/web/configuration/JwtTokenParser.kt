package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenParser(@Value("\${jwt.secret}") secretKey: String) {
    private val signingKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(signingKey).build()
            .parseSignedClaims(token).payload
}
