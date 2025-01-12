package dev.ash.core.aspects.logging.masking;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Utility class for masking sensitive data in strings
 */
@Slf4j
public class DataMasker {

  private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

  /**
   * Masks sensitive data based on provided patterns
   *
   * @param content  The content to mask
   * @param patterns List of masking patterns to apply
   * @return Masked content
   */
  public String maskSensitiveData(String content, List<MaskingPattern> patterns) {
    if (!StringUtils.hasText(content) || patterns == null || patterns.isEmpty()) {
      return content;
    }

    String maskedContent = content;
    for (MaskingPattern maskingPattern : patterns) {
      try {
        maskedContent = applyMaskingPattern(maskedContent, maskingPattern);
      } catch (Exception e) {
        log.warn("Error applying masking pattern: {}", maskingPattern.getPattern(), e);
      }
    }
    return maskedContent;
  }

  /**
   * Applies a single masking pattern to the content
   *
   * @param content        The content to mask
   * @param maskingPattern The pattern to apply
   * @return Masked content
   */
  private String applyMaskingPattern(String content, MaskingPattern maskingPattern) {
    Pattern pattern = patternCache.computeIfAbsent(
        maskingPattern.getPattern(),
        key -> Pattern.compile(key, Pattern.CASE_INSENSITIVE)
    );

    Matcher matcher = pattern.matcher(content);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String match = matcher.group();
      String masked = createMask(match, maskingPattern);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
    }
    matcher.appendTail(sb);

    return sb.toString();
  }

  /**
   * Creates a mask for the matched content
   *
   * @param match          The matched content to mask
   * @param maskingPattern The pattern defining how to mask
   * @return Masked string
   */
  private String createMask(String match, MaskingPattern maskingPattern) {
    int visibleChars = maskingPattern.getVisibleCharacters();
    String maskChar = maskingPattern.getMaskCharacter();

    if (visibleChars <= 0) {
      return maskChar.repeat(8);
    }

    if (match.length() <= visibleChars) {
      return match;
    }

    // Keep visible characters at start
    String visible = match.substring(0, visibleChars);
    String masked = maskChar.repeat(Math.min(8, match.length() - visibleChars));

    return visible + masked;
  }

  /**
   * Clears the pattern cache
   */
  public void clearPatternCache() {
    patternCache.clear();
  }
}