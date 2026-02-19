package com.uniqdata.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI backendOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backend API (연구 서비스)")
                        .description(
                                "연구 CRUD, 참여자 관리, 대시보드. 블록체인 처리가 필요할 때 Core(블록체인 서버) API를 호출합니다."
                        )
                        .version("1.0.0"));
    }
}
