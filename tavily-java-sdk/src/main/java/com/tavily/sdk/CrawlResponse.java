package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Response from {@code POST /crawl}: every page the crawler kept, already extracted. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlResponse(
    @JsonProperty("base_url")      String baseUrl,
    @JsonProperty("results")       List<Result> results,
    @JsonProperty("response_time") Double responseTime,
    @JsonProperty("usage")         Usage usage,
    @JsonProperty("request_id")    String requestId
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      @JsonProperty("url")         String url,
      @JsonProperty("raw_content") String rawContent,
      @JsonProperty("favicon")     String favicon
  ) {
  }
}
