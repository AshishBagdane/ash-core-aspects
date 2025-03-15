package com.ashishbagdane.lib.aspects.logging.masking;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * Defines a pattern for masking sensitive data
 */
@Data
@Builder
public class MaskingPattern {

  @NotNull
  private String pattern;

  @Builder.Default
  private String maskCharacter = "*";

  @Builder.Default
  private Integer visibleCharacters = 0;
}
