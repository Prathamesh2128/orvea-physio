package com.hms.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

public class JwtTokenUtilTest {
	private JwtProperties props;
	private JwtTokenUtil util;
	private AuthenticatedUser user;

	private static String newSecret() {
		return Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
	}

	@BeforeEach
	void setUp() {
		props = new JwtProperties();
		props.setSecret(newSecret());
		util = new JwtTokenUtil(props);
		util.init();
		user = new AuthenticatedUser(UUID.randomUUID(), "dr.rao", UUID.randomUUID(), "sid-1", List.of("DOCTOR"),
				List.of("PATIENT_READ", "LYMPHEDEMA_CREATE"));
	}
	
	@Test
	@DisplayName("access token round-trips identity, roles, permissions and session id")
	void accessTokenRoundTrip() {
		String token = util.generateAccessToken(user);
		Claims claims = util.parse(token);
		AuthenticatedUser parsed = util.toUser(claims);
		
		assertEquals(user.userId(), parsed.userId());
		assertEquals("dr.rao", parsed.username());
        assertEquals(user.clinicId(), parsed.clinicId());
        assertEquals("sid-1", parsed.sessionId());
        assertEquals(List.of("DOCTOR"), parsed.roles());
        assertEquals(List.of("PATIENT_READ", "LYMPHEDEMA_CREATE"), parsed.permissions());
        assertEquals("access", claims.get("typ", String.class));
	}
}
