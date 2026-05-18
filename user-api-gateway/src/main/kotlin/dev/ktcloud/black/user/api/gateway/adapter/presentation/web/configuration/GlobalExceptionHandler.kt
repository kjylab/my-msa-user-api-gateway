package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(StatusException::class, StatusRuntimeException::class)
    fun handleGrpcException(e: Exception): ResponseEntity<Map<String, Any>> {
        val grpcStatus = when (e) {
            is StatusException -> e.status
            is StatusRuntimeException -> e.status
            else -> Status.INTERNAL
        }
        val httpStatus = when (grpcStatus.code) {
            Status.Code.NOT_FOUND -> HttpStatus.NOT_FOUND
            Status.Code.INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST
            Status.Code.ALREADY_EXISTS -> HttpStatus.CONFLICT
            Status.Code.PERMISSION_DENIED -> HttpStatus.FORBIDDEN
            Status.Code.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return ResponseEntity(
            mapOf(
                "status" to httpStatus.value(),
                "error" to httpStatus.reasonPhrase,
                "message" to (grpcStatus.description ?: "gRPC service error")
            ),
            httpStatus
        )
    }
}
