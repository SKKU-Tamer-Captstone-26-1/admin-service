package com.ontheblock.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

final class ActorIdResolver {

    private ActorIdResolver() {}

    static UUID resolve(HttpServletRequest request) {
        String raw = request.getHeader("X-User-Id");
        if (raw == null || raw.isBlank()) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
}
