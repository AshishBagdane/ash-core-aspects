package dev.ash.core.aspects.logging.models;

import dev.ash.core.aspects.logging.masking.MaskingPattern;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


/**
 * Configuration properties for request/response logging
 */
@Data
@Builder
@Validated
@ConfigurationProperties(prefix = "library.logging")
public class LoggingProperties {

  @NotNull
  private Boolean enabled;

  @Builder.Default
  private List<String> excludedPaths = new ArrayList<>();

  @Builder.Default
  private List<MaskingPattern> maskPatterns = new ArrayList<>();

  @Builder.Default
  private InclusionConfig inclusion = new InclusionConfig();

  @Builder.Default
  private AsyncConfig async = new AsyncConfig();

  @Builder.Default
  private Map<String, EnvironmentConfig> environments = new HashMap<>();

  @Data
  @Builder
  public static class InclusionConfig {

    @Builder.Default
    private Boolean headers = true;

    @Builder.Default
    private Boolean queryParams = true;

    @Builder.Default
    private Boolean body = true;

    @Builder.Default
    private Boolean performance = true;

    // Default constructor with default values
    public InclusionConfig() {
      this.headers = true;
      this.queryParams = true;
      this.body = true;
      this.performance = true;
    }

    // All args constructor for builder
    public InclusionConfig(Boolean headers, Boolean queryParams, Boolean body, Boolean performance) {
      this.headers = headers != null ? headers : true;
      this.queryParams = queryParams != null ? queryParams : true;
      this.body = body != null ? body : true;
      this.performance = performance != null ? performance : true;
    }
  }

  @Data
  @Builder
  public static class AsyncConfig {

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String timeout = "30s";

    // Default constructor with default values
    public AsyncConfig() {
      this.enabled = true;
      this.timeout = "30s";
    }

    // All args constructor for builder
    public AsyncConfig(Boolean enabled, String timeout) {
      this.enabled = enabled != null ? enabled : true;
      this.timeout = timeout != null ? timeout : "30s";
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EnvironmentConfig {

    @NotNull
    private LogLevel level;
  }
}