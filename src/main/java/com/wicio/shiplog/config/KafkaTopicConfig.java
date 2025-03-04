package com.wicio.shiplog.config;

import static com.wicio.shiplog.kafka.KafkaTopicName.VESSEL_LOG;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic vesselLogTopic() {
    return TopicBuilder
        .name(VESSEL_LOG)
        .partitions(1)
        .replicas(3)
        .build();
  }
}
