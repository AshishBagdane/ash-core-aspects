package com.ashishbagdane.lib.aspects.logging.models;

/**
 * Enum representing different logging levels for environments
 */
public enum LogLevel {
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR;

  public boolean isEnabled(LogLevel minimumLevel) {
    return this.ordinal() >= minimumLevel.ordinal();
  }
}
