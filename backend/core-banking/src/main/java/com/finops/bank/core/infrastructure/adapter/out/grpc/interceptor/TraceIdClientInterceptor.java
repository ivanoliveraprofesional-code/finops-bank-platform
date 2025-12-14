package com.finops.bank.core.infrastructure.adapter.out.grpc.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import java.util.UUID;

@Order(1)
@GrpcGlobalClientInterceptor
public class TraceIdClientInterceptor implements ClientInterceptor {

    @Override
    public <I, O> ClientCall<I, O> interceptCall(
            MethodDescriptor<I, O> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<I, O>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<O> responseListener, Metadata headers) {
                String traceId = MDC.get("traceId");
                if (traceId == null) {
                    traceId = UUID.randomUUID().toString();
                    MDC.put("traceId", traceId);
                }

                headers.put(GrpcHeaderContext.TRACE_ID_KEY, traceId);

                super.start(responseListener, headers);
            }
        };
    }
}