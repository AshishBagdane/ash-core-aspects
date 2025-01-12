package dev.ash.core.aspects.logging.aspect;

import dev.ash.core.aspects.logging.models.LoggingProperties;
import dev.ash.core.aspects.logging.models.RequestContext;
import dev.ash.core.aspects.logging.utils.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Aspect for logging HTTP requests and responses This aspect intercepts all REST endpoints and logs the requests and responses based on the configured properties
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestResponseLoggingAspect {

  private final LoggingUtils loggingUtils;
  private final LoggingProperties loggingProperties;

  /**
   * Pointcut for all REST controller methods
   */
  @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
      "@within(org.springframework.stereotype.Controller)")
  public void controllerPointcut() {
  }

  /**
   * Pointcut for all methods in REST controllers
   */
  @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
      "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
      "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
      "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
      "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
      "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
  public void requestMappingPointcut() {
  }

  /**
   * Combined pointcut for REST endpoints
   */
  @Pointcut("controllerPointcut() && requestMappingPointcut()")
  public void restEndpointPointcut() {
  }

  /**
   * Around advice to log requests and responses
   */
  @Around("restEndpointPointcut()")
  public Object logRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!loggingProperties.getEnabled()) {
      return joinPoint.proceed();
    }

    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
      return joinPoint.proceed();
    }

      HttpServletRequest request = servletRequestAttributes.getRequest();
    HttpServletResponse response = servletRequestAttributes.getResponse();

    // Wrap request and response for content caching
    ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
    ContentCachingResponseWrapper responseWrapper = wrapResponse(response);

    // Start building request context
    long startTime = System.currentTimeMillis();
    String correlationId = loggingUtils.initializeContext(requestWrapper);

    try {
      // Build and log request context
      RequestContext.RequestContextBuilder contextBuilder = RequestContext.builder()
          .correlationId(correlationId)
          .method(requestWrapper.getMethod())
          .path(requestWrapper.getRequestURI())
          .headers(extractHeaders(requestWrapper))
          .queryParams(extractQueryParams(requestWrapper))
          .startTime(startTime);

      // Log request
      RequestContext requestContext = contextBuilder.build();
      loggingUtils.logRequest(requestContext);

      // Proceed with the actual request
      Object result = joinPoint.proceed();

      // Complete context with response data
      long endTime = System.currentTimeMillis();
      RequestContext responseContext = contextBuilder
          .responseBody(loggingUtils.extractResponseBody(responseWrapper))
          .statusCode(responseWrapper.getStatus())
          .endTime(endTime)
          .executionTime(loggingUtils.formatExecutionTime(startTime, endTime))
          .build();

      // Log response
      loggingUtils.logResponse(responseContext);

      // Copy content to original response
      if (responseWrapper != null) {
        responseWrapper.copyBodyToResponse();
      }

      return result;

    } catch (Exception e) {
      log.error("Error in logging aspect", e);
      throw e;
    } finally {
      loggingUtils.clearContext();
    }
  }

  /**
   * Wrap HttpServletRequest with ContentCachingRequestWrapper if not already wrapped
   */
  private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
    if (request instanceof ContentCachingRequestWrapper) {
      return (ContentCachingRequestWrapper) request;
    }
    return new ContentCachingRequestWrapper(request);
  }

  /**
   * Wrap HttpServletResponse with ContentCachingResponseWrapper if not already wrapped
   */
  private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
    if (response instanceof ContentCachingResponseWrapper) {
      return (ContentCachingResponseWrapper) response;
    }
    return response != null ? new ContentCachingResponseWrapper(response) : null;
  }

  /**
   * Extract headers from request
   */
  private Map<String, String> extractHeaders(HttpServletRequest request) {
    if (!loggingProperties.getInclusion().getHeaders()) {
      return Collections.emptyMap();
    }

    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames())
        .forEach(headerName ->
            headers.put(headerName, request.getHeader(headerName)));
    return headers;
  }

  /**
   * Extract query parameters from request
   */
  private Map<String, String> extractQueryParams(HttpServletRequest request) {
    if (!loggingProperties.getInclusion().getQueryParams()) {
      return Collections.emptyMap();
    }

    return Optional.ofNullable(request.getQueryString())
        .map(queryString -> {
          Map<String, String> queryParams = new HashMap<>();
          for (String param : queryString.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
              queryParams.put(pair[0], pair[1]);
            }
          }
          return queryParams;
        })
        .orElse(Collections.emptyMap());
  }
}