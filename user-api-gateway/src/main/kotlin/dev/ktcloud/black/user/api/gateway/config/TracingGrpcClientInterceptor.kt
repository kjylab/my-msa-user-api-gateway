package dev.ktcloud.black.user.api.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@GrpcGlobalClientInterceptor
class TracingGrpcClientInterceptor : ClientInterceptor {

    @Lazy @Autowired private lateinit var tracer: Tracer
    @Lazy @Autowired private lateinit var propagator: Propagator

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> =
        object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                tracer.currentSpan()?.let { span ->
                    propagator.inject(span.context(), headers) { carrier, key, value ->
                        carrier?.put(
                            Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER),
                            value
                        )
                    }
                }
                super.start(responseListener, headers)
            }
        }
}
