package dev.ktcloud.black.user.api.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import org.springframework.stereotype.Component

@Component
@GrpcGlobalClientInterceptor
class TracingGrpcClientInterceptor : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> =
        object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                GlobalOpenTelemetry.getPropagators()
                    .textMapPropagator
                    .inject(Context.current(), headers) { carrier, key, value ->
                        carrier?.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
                    }
                super.start(responseListener, headers)
            }
        }
}
