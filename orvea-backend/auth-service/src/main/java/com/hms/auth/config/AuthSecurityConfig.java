package com.hms.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hms.common.security.GatewayHeaderAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class AuthSecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll().anyRequest()
						.authenticated())
				.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(new GatewayHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	UserDetailsService noUserDetailsService() {
		return username -> {
			throw new UsernameNotFoundException(username);
		};
	}
}
