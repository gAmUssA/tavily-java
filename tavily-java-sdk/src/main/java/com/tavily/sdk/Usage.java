package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Credit usage for a request. Only returned when {@code includeUsage(true)} is
 * set; Tavily's shape here is evolving, so unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Usage(
    Integer requests,
    Double credits
) {
}
