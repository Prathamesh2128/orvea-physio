package com.hms.common.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import com.hms.common.audit.AuditPublisher;
import com.hms.common.audit.EventBackedAuditPublisher;
import com.hms.common.audit.NoOpAuditPublisher;
import com.hms.common.event.EventPublisher;
import com.hms.common.event.EventTopics;
import com.hms.common.event.KafkaEventPublisher;

@AutoConfiguration(before = CommonAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "physio.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	EventPublisher eventPublisher(KafkaTemplate<String, String> template,
			@Value("${spring.application.name:app}") String applicationName) {
		return new KafkaEventPublisher(template, applicationName);
	}

	@Bean
	@ConditionalOnMissingBean
	AuditPublisher auditPublisher(ObjectProvider<EventPublisher> events,
			@Value("${physio.audit.enabled:true}") boolean enabled) {
		EventPublisher publisher = events.getIfAvailable();
		return enabled && publisher != null ? new EventBackedAuditPublisher(publisher) : new NoOpAuditPublisher();
	}

	/**
	 * Topics plus dead-letter topics (same partition count so dead-lettering
	 * works).
	 */
	@Bean
	KafkaAdmin.NewTopics physioTopics(@Value("${physio.kafka.replicas:1}") int replicas) {
		List<NewTopic> topics = new ArrayList<>();
		for (String t : List.of(EventTopics.APPOINTMENT, EventTopics.NOTIFICATION, EventTopics.AUDIT)) {
			topics.add(TopicBuilder.name(t).partitions(3).replicas(replicas).build());
			topics.add(TopicBuilder.name(t + ".DLT").partitions(3).replicas(replicas).build());
		}
		return new KafkaAdmin.NewTopics(topics.toArray(new NewTopic[0]));
	}
}
