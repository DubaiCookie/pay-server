package com.payserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Server API")
                        .description("Toss Payment 기반 결제/환불 서비스 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pay Server Team")
                                .email("contact@payserver.com")));
    }
}
