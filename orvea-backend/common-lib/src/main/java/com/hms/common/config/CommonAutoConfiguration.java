package com.hms.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;

import com.hms.common.exception.GlobalExceptionHandler;
import com.hms.common.security.AuditorAwareImpl;
import com.hms.common.security.JwtProperties;
import com.hms.common.security.JwtTokenUtil;

@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import(GlobalExceptionHandler.class)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "physio.jwt", name = "secret")
    public JwtTokenUtil jwtTokenUtil(JwtProperties props) {
        return new JwtTokenUtil(props);
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
