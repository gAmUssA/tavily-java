package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Response from {@code POST /map}: the discovered URLs, nothing else. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MapResponse(
    @JsonProperty("base_url")      String baseUrl,
    @JsonProperty("results")       List<String> results,
    @JsonProperty("response_time") Double responseTime,
    @JsonProperty("usage")         Usage usage,
    @JsonProperty("request_id")    String requestId
) {
}
