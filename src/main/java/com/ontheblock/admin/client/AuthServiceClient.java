package com.ontheblock.admin.client;

import com.ontheblock.auth.v1.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthServiceClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    public AdminListUsersResponse listUsers(Role roleFilter, int pageSize, String pageToken) {
        try {
            return authStub.adminListUsers(AdminListUsersRequest.newBuilder()
                    .setRoleFilter(roleFilter != null ? roleFilter : Role.ROLE_UNSPECIFIED)
                    .setPageSize(pageSize)
                    .setPageToken(pageToken != null ? pageToken : "")
                    .build());
        } catch (StatusRuntimeException e) {
            log.error("auth-service AdminListUsers failed: {}", e.getStatus());
            throw e;
        }
    }

    public UserResponse createUser(String username, String password, Role role) {
        try {
            return authStub.adminCreateUser(AdminCreateUserRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setRole(role)
                    .build()).getUser();
        } catch (StatusRuntimeException e) {
            log.error("auth-service AdminCreateUser failed: {}", e.getStatus());
            throw e;
        }
    }

    public UserResponse updateUser(String userId, String newPassword, String newUsername) {
        AdminUpdateUserRequest.Builder req = AdminUpdateUserRequest.newBuilder()
                .setUserId(userId);
        if (newPassword != null && !newPassword.isBlank()) req.setNewPassword(newPassword);
        if (newUsername != null && !newUsername.isBlank()) req.setNewUsername(newUsername);
        try {
            return authStub.adminUpdateUser(req.build()).getUser();
        } catch (StatusRuntimeException e) {
            log.error("auth-service AdminUpdateUser failed: {}", e.getStatus());
            throw e;
        }
    }

    public void deleteUser(String userId) {
        try {
            authStub.adminDeleteUser(AdminDeleteUserRequest.newBuilder()
                    .setUserId(userId)
                    .build());
        } catch (StatusRuntimeException e) {
            log.error("auth-service AdminDeleteUser failed: {}", e.getStatus());
            throw e;
        }
    }

    public UserResponse getUser(String userId) {
        try {
            return authStub.adminGetUser(AdminGetUserRequest.newBuilder()
                    .setUserId(userId)
                    .build()).getUser();
        } catch (StatusRuntimeException e) {
            log.error("auth-service AdminGetUser failed: {}", e.getStatus());
            throw e;
        }
    }

    public int countActiveManagers() {
        try {
            AdminListUsersResponse resp = authStub.adminListUsers(AdminListUsersRequest.newBuilder()
                    .setRoleFilter(Role.ROLE_UNSPECIFIED)
                    .setPageSize(1000)
                    .build());
            return resp.getUsersCount();
        } catch (StatusRuntimeException e) {
            log.warn("auth-service count managers failed: {}", e.getStatus());
            return 0;
        }
    }
}
