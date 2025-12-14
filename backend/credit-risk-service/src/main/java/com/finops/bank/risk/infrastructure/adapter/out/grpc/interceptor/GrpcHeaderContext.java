package com.finops.bank.risk.infrastructure.adapter.out.grpc.interceptor;

import io.grpc.Metadata;

public final class GrpcHeaderContext {

    public static final Metadata.Key<String> TRACE_ID_KEY =
        Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcHeaderContext() {
        throw new IllegalStateException("Utility class");
    }
}