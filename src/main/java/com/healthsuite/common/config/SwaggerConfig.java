package com.healthsuite.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String SHARE_TOKEN_SCHEME = "shareToken";

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local development"),
                        new Server().url("https://api.healthsuite.com").description("Production")
                ))
                // Apply JWT auth globally — individual endpoints can override
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, jwtSecurityScheme())
                        .addSecuritySchemes(SHARE_TOKEN_SCHEME, shareTokenSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("HealthSuite API")
                .version("1.0.0")
                .description("""
                        **HealthSuite V1** — Personal health records, family mesh access, concierge appointment booking, and a doctor directory.

                        ## Authentication
                        Most endpoints require a **Bearer JWT** token obtained from `POST /api/auth/login` or `POST /api/auth/register`.

                        Certain record-access endpoints also accept an **`X-Share-Token`** header for time-limited, unauthenticated access to shared records.

                        ## Roles
                        | Role | Capabilities |
                        |------|--------------|
                        | `ROLE_USER` | Own PHR, family management, share tokens |
                        | `ROLE_PREMIUM` | All of USER + concierge ticket submission |
                        | `ROLE_SUPPORT` | Claim, confirm, and cancel concierge tickets |
                        | `ROLE_ADMIN` | System-wide analytics |
                        | `ROLE_DOCTOR` | Read-only via share token |
                        """)
                .contact(new Contact()
                        .name("HealthSuite Team")
                        .email("support@healthsuite.com"))
                .license(new License().name("Proprietary"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your access token (without the 'Bearer ' prefix)");
    }

    private SecurityScheme shareTokenSecurityScheme() {
        return new SecurityScheme()
                .name(SHARE_TOKEN_SCHEME)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Share-Token")
                .description("Time-limited share token for accessing a specific shared medical record");
    }
}
