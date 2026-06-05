package com.ontheblock.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Admin Service API")
                        .description("On the Block 어드민 서비스 REST API (gRPC 서비스의 테스트/문서화용)")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("ActorId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("액터 UUID (운영자 또는 관리자 user_id)")))
                .addSecurityItem(new SecurityRequirement().addList("ActorId"));
    }

    // 모든 엔드포인트에 X-User-Id 헤더를 자동으로 추가
    @Bean
    public OperationCustomizer actorIdHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-User-Id")
                    .description("액터 UUID")
                    .required(false)
                    .schema(new StringSchema()
                            .example("00000000-0000-0000-0000-000000000001")));
            return operation;
        };
    }
}
