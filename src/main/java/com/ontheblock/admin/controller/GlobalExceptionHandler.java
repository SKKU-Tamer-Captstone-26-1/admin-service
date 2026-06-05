package com.ontheblock.admin.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGrpcStatus(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        HttpStatus http = switch (code) {
            case NOT_FOUND          -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS     -> HttpStatus.CONFLICT;
            case INVALID_ARGUMENT   -> HttpStatus.BAD_REQUEST;
            case PERMISSION_DENIED  -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED    -> HttpStatus.UNAUTHORIZED;
            case FAILED_PRECONDITION -> HttpStatus.UNPROCESSABLE_ENTITY;
            case UNIMPLEMENTED      -> HttpStatus.NOT_IMPLEMENTED;
            default                 -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(http)
                .body(Map.of("error", ex.getStatus().getDescription() != null
                        ? ex.getStatus().getDescription() : code.name()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
