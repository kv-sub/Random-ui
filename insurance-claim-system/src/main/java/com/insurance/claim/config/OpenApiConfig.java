package com.insurance.claim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Insurance Claim Submission System API")
                        .version("1.0.0")
                        .description("REST API for submitting and managing insurance claims with policy validation, " +
                                "duplicate detection, and coverage limit enforcement.")
                        .contact(new Contact()
                                .name("Insurance Claims Team")
                                .email("claims@insurance.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
