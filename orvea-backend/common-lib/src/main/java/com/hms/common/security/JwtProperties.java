package com.hms.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "physio.jwt")
public class JwtProperties {
    /** Base64 encoded HS256 secret, min 256 bit. */
    private String secret;
    private long accessTokenExpiryMs = 15 * 60 * 1000L;          // 15 min
    private long refreshTokenExpiryMs = 7 * 24 * 60 * 60 * 1000L; // 7 days
    private String issuer = "physio-hms";
}
