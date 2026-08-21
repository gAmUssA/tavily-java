package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How hard Tavily works to pull content off a page. {@code ADVANCED} handles
 * tables, embedded content and bot-gated pages; it costs more credits.
 * Used by {@code /extract} and {@code /crawl}.
 */
public enum ExtractDepth {
  @JsonProperty("basic")    BASIC,
  @JsonProperty("advanced") ADVANCED
}
