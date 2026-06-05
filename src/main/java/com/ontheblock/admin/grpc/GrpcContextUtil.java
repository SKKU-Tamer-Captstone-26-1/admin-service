package com.ontheblock.admin.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

import java.util.UUID;

/**
 * Extracts x-user-id injected by gateway's metadata_forward.go and stores it in gRPC Context.
 * This allows gRPC service implementations to identify the calling user for audit logging.
 */
@GrpcGlobalServerInterceptor
public class GrpcContextUtil implements ServerInterceptor {

    private static final Metadata.Key<String> USER_ID_KEY =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

    private static final Context.Key<String> USER_ID_CONTEXT_KEY = Context.key("userId");

    @Override
    public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata headers,
                                                        ServerCallHandler<Q, A> next) {
        String userId = headers.get(USER_ID_KEY);
        Context ctx = Context.current().withValue(USER_ID_CONTEXT_KEY, userId != null ? userId : "");
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    public static UUID getUserId() {
        String raw = USER_ID_CONTEXT_KEY.get(Context.current());
        if (raw == null || raw.isBlank()) return UUID.fromString("00000000-0000-0000-0000-000000000000");
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
}
