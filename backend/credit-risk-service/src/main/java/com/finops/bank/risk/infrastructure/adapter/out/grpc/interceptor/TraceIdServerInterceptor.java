package com.finops.bank.risk.infrastructure.adapter.out.grpc.interceptor;

import org.slf4j.MDC;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

@Slf4j
@GrpcGlobalServerInterceptor
public class TraceIdServerInterceptor implements ServerInterceptor {
		private static final String TRACE_ID = "traceId";
	
    @Override
    public <I, O> ServerCall.Listener<I> interceptCall(
            ServerCall<I, O> call, Metadata headers, ServerCallHandler<I, O> next) {

        String traceId = headers.get(GrpcHeaderContext.TRACE_ID_KEY);
        
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }

        ServerCall<I, O> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<I, O>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                MDC.remove(TRACE_ID);
                super.close(status, trailers);
            }
        };

        ServerCall.Listener<I> originalListener = next.startCall(wrappedCall, headers);

        return new SimpleForwardingServerCallListener<I>(originalListener) {
            @Override
            public void onMessage(I message) {
                if (traceId != null) MDC.put(TRACE_ID, traceId);
                try {
                    super.onMessage(message);
                } finally {
                    MDC.remove(TRACE_ID);
                }
            }

            @Override
            public void onHalfClose() {
                if (traceId != null) MDC.put(TRACE_ID, traceId);
                try {
                    super.onHalfClose();
                } finally {
                    MDC.remove(TRACE_ID);
                }
            }

            @Override
            public void onCancel() {
                if (traceId != null) MDC.put(TRACE_ID, traceId);
                try {
                    super.onCancel();
                } finally {
                    MDC.remove(TRACE_ID);
                }
            }
        };
    }
}