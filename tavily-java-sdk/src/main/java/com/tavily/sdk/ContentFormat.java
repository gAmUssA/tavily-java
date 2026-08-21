package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Output format for extracted page content. Used by {@code /extract} and {@code /crawl}. */
public enum ContentFormat {
  @JsonProperty("markdown") MARKDOWN,
  @JsonProperty("text")     TEXT
}
