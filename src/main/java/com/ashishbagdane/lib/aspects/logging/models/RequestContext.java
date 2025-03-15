package com.ashishbagdane.lib.aspects.logging.models;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Holds context information for a single request/response cycle
 */
@Getter
@Builder
public class RequestContext {

  private final String correlationId;
  private final String method;
  private final String path;
  private final Map<String, String> headers;
  private final Map<String, String> queryParams;
  private final String requestBody;
  private final String responseBody;
  private final long startTime;
  private final long endTime;
  private final String executionTime;
  private final int statusCode;

  @Builder.Default
  private final Map<String, Object> additionalInfo = new HashMap<>();
}
