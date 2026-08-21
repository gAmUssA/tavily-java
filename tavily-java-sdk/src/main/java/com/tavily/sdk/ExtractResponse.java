package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from {@code POST /extract}. Note the split: URLs Tavily could not read
 * land in {@code failedResults} with a reason instead of throwing — a batch is
 * partially successful by design.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractResponse(
    @JsonProperty("results")        List<Result> results,
    @JsonProperty("failed_results") List<FailedResult> failedResults,
    @JsonProperty("response_time")  Double responseTime,
    @JsonProperty("usage")          Usage usage,
    @JsonProperty("request_id")     String requestId
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      @JsonProperty("url")         String url,
      @JsonProperty("title")       String title,
      @JsonProperty("raw_content") String rawContent,
      @JsonProperty("images")      List<String> images,
      @JsonProperty("favicon")     String favicon
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FailedResult(
      @JsonProperty("url")   String url,
      @JsonProperty("error") String error
  ) {
  }
}
