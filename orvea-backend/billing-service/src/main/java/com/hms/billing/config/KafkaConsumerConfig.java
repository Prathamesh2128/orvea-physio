package com.hms.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

	/** 3 retries, 2 s apart, then the record goes to <topic>.DLT. */
	@Bean
	DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
		return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(2000L, 3L));
	}
}
