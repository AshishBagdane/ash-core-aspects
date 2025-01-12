package dev.ash.core.aspects.logging.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ash.core.aspects.logging.masking.MaskingPattern;
import dev.ash.core.aspects.logging.models.LogLevel;
import dev.ash.core.aspects.logging.models.LoggingProperties;
import dev.ash.core.aspects.logging.models.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Utility class for handling request/response logging operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingUtils {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final String UNKNOWN = "unknown";

  private final ObjectMapper objectMapper;
  private final LoggingProperties loggingProperties;

  // Cache compiled patterns for better performance
  private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

  /**
   * Initialize logging context for the current request
   *
   * @param request The HTTP request
   * @return Generated correlation ID
   */
  public String initializeContext(HttpServletRequest request) {
    String correlationId = extractOrGenerateCorrelationId(request);
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    return correlationId;
  }

  /**
   * Log request details based on configuration
   *
   * @param context Request context
   */
  public void logRequest(RequestContext context) {
    if (!isLoggingEnabled()) {
      return;
    }

    LogLevel logLevel = determineLogLevel();
    String maskedRequest = maskSensitiveData(buildRequestMessage(context));

    switch (logLevel) {
      case DEBUG -> log.debug(maskedRequest);
      case INFO -> log.info(maskedRequest);
      case WARN -> log.warn(maskedRequest);
      case ERROR -> log.error(maskedRequest);
      case TRACE -> log.trace(maskedRequest);
    }
  }

  /**
   * Log response details based on configuration
   *
   * @param context Request context
   */
  public void logResponse(RequestContext context) {
    if (!isLoggingEnabled()) {
      return;
    }

    LogLevel logLevel = determineLogLevel();
    String maskedResponse = maskSensitiveData(buildResponseMessage(context));

    switch (logLevel) {
      case DEBUG -> log.debug(maskedResponse);
      case INFO -> log.info(maskedResponse);
      case WARN -> log.warn(maskedResponse);
      case ERROR -> log.error(maskedResponse);
      case TRACE -> log.trace(maskedResponse);
    }
  }

  /**
   * Extract request body from wrapper
   *
   * @param wrapper ContentCachingRequestWrapper
   * @return Request body as string
   */
  public String extractRequestBody(ContentCachingRequestWrapper wrapper) {
    if (!shouldLogBody(wrapper.getContentType())) {
      return "[Body logging disabled for content type: " + wrapper.getContentType() + "]";
    }

    byte[] content = wrapper.getContentAsByteArray();
    if (content.length == 0) {
      return StringUtils.EMPTY;
    }

    try {
      return new String(content, wrapper.getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      log.warn("Failed to extract request body", e);
      return "[Error extracting body]";
    }
  }

  /**
   * Extract response body from wrapper
   *
   * @param wrapper ContentCachingResponseWrapper
   * @return Response body as string
   */
  public String extractResponseBody(ContentCachingResponseWrapper wrapper) {
    if (!shouldLogBody(wrapper.getContentType())) {
      return "[Body logging disabled for content type: " + wrapper.getContentType() + "]";
    }

    byte[] content = wrapper.getContentAsByteArray();
    if (content.length == 0) {
      return StringUtils.EMPTY;
    }

    try {
      return new String(content, wrapper.getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      log.warn("Failed to extract response body", e);
      return "[Error extracting body]";
    }
  }

  /**
   * Format execution time in a human-readable format
   *
   * @param startTime Start timestamp
   * @param endTime   End timestamp
   * @return Formatted duration string
   */
  public String formatExecutionTime(long startTime, long endTime) {
    Duration duration = Duration.ofMillis(endTime - startTime);
    if (duration.toMillis() < 1000) {
      return duration.toMillis() + "ms";
    }
    return duration.toSeconds() + "s";
  }

  /**
   * Clean up logging context
   */
  public void clearContext() {
    MDC.remove(CORRELATION_ID_MDC_KEY);
  }

  private String extractOrGenerateCorrelationId(HttpServletRequest request) {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    return StringUtils.isNotBlank(correlationId) ? correlationId : UUID.randomUUID().toString();
  }

  private boolean isLoggingEnabled() {
    return loggingProperties.getEnabled() &&
        !isExcludedPath(MDC.get("requestUri"));
  }

  private boolean isExcludedPath(String path) {
    return loggingProperties.getExcludedPaths().stream()
        .anyMatch(excludedPath -> path != null && path.matches(excludedPath));
  }

  private LogLevel determineLogLevel() {
    String environment = System.getProperty("spring.profiles.active", "default");
    return loggingProperties.getEnvironments()
        .getOrDefault(environment, new LoggingProperties.EnvironmentConfig(LogLevel.INFO))
        .getLevel();
  }

  private String maskSensitiveData(String content) {
    if (StringUtils.isBlank(content)) {
      return content;
    }

    String maskedContent = content;
    for (MaskingPattern maskingPattern : loggingProperties.getMaskPatterns()) {
      Pattern pattern = patternCache.computeIfAbsent(
          maskingPattern.getPattern(),
          Pattern::compile
      );
      maskedContent = pattern.matcher(maskedContent).replaceAll(createMask(maskingPattern));
    }
    return maskedContent;
  }

  private String createMask(MaskingPattern maskingPattern) {
    int visibleChars = maskingPattern.getVisibleCharacters();
    if (visibleChars <= 0) {
      return maskingPattern.getMaskCharacter().repeat(8);
    }
    return "$1" + maskingPattern.getMaskCharacter().repeat(8);
  }

  private boolean shouldLogBody(String contentType) {
    if (!loggingProperties.getInclusion().getBody()) {
      return false;
    }

    return contentType != null && (
        contentType.contains(MediaType.APPLICATION_JSON_VALUE) ||
            contentType.contains(MediaType.APPLICATION_XML_VALUE) ||
            contentType.contains(MediaType.TEXT_PLAIN_VALUE)
    );
  }

  private String buildRequestMessage(RequestContext context) {
    StringBuilder message = new StringBuilder()
        .append("Incoming Request | ")
        .append("Method: ").append(context.getMethod())
        .append(" | Path: ").append(context.getPath());

    if (loggingProperties.getInclusion().getHeaders()) {
      message.append(" | Headers: ").append(context.getHeaders());
    }

    if (loggingProperties.getInclusion().getQueryParams()) {
      message.append(" | Query Parameters: ").append(context.getQueryParams());
    }

    if (loggingProperties.getInclusion().getBody() && StringUtils.isNotBlank(context.getRequestBody())) {
      message.append(" | Body: ").append(context.getRequestBody());
    }

    return message.toString();
  }

  private String buildResponseMessage(RequestContext context) {
    StringBuilder message = new StringBuilder()
        .append("Outgoing Response | ")
        .append("Status: ").append(context.getStatusCode());

    if (loggingProperties.getInclusion().getBody() && StringUtils.isNotBlank(context.getResponseBody())) {
      message.append(" | Body: ").append(context.getResponseBody());
    }

    if (loggingProperties.getInclusion().getPerformance()) {
      message.append(" | Execution Time: ").append(context.getExecutionTime());
    }

    return message.toString();
  }
}