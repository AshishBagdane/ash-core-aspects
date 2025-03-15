package com.ashishbagdane.lib.aspects.logging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ashishbagdane.lib.aspects.logging.aspect.RequestResponseLoggingAspect;
import com.ashishbagdane.lib.aspects.logging.models.LoggingProperties;
import com.ashishbagdane.lib.aspects.logging.utils.LoggingUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for request/response logging functionality This class sets up all required beans and can be enabled/disabled via properties
 */
@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "library.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({RequestResponseLoggingAspect.class})
public class LoggingConfiguration {

  /**
   * Creates ObjectMapper bean if not already present
   *
   * @return ObjectMapper instance
   */
  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  /**
   * Creates LoggingUtils bean
   *
   * @param objectMapper      for JSON processing
   * @param loggingProperties configuration properties
   * @return LoggingUtils instance
   */
  @Bean
  @ConditionalOnMissingBean
  public LoggingUtils loggingUtils(ObjectMapper objectMapper, LoggingProperties loggingProperties) {
    return new LoggingUtils(objectMapper, loggingProperties);
  }

  /**
   * Example usage in application.yml:
   *
   * library:
   *   logging:
   *     enabled: true
   *     mask-patterns:
   *       - pattern: "password"
   *       - pattern: "token"
   *     excluded-paths:
   *       - "/health"
   *       - "/metrics"
   *     inclusion:
   *       headers: true
   *       query-params: true
   *       body: true
   *       performance: true
   *     async:
   *       enabled: true
   *       timeout: 30s
   *     environments:
   *       dev:
   *         level: DEBUG
   *       prod:
   *         level: INFO
   */
}
