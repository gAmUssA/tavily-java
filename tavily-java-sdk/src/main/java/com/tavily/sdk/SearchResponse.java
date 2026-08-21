package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response from {@code POST /search}. {@code answer} is only populated when the
 * request asked for it; {@code images} likewise.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(
    @JsonProperty("query")          String query,
    @JsonProperty("answer")         String answer,
    @JsonProperty("results")        List<Result> results,
    @JsonProperty("images")         List<Image> images,
    @JsonProperty("follow_up_questions") List<String> followUpQuestions,
    @JsonProperty("auto_parameters") Map<String, Object> autoParameters,
    @JsonProperty("response_time")  Double responseTime,
    @JsonProperty("usage")          Usage usage,
    @JsonProperty("request_id")     String requestId
) {

  /**
   * One search hit. {@code content} is the relevance-ranked snippet Tavily always
   * returns; {@code rawContent} is the full page and only arrives when the request
   * set {@code includeRawContent}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      @JsonProperty("id")             String id,
      @JsonProperty("title")          String title,
      @JsonProperty("url")            String url,
      @JsonProperty("content")        String content,
      @JsonProperty("score")          Double score,
      @JsonProperty("raw_content")    String rawContent,
      @JsonProperty("published_date") String publishedDate,
      @JsonProperty("favicon")        String favicon
  ) {
  }

  /**
   * A query-related image. Tavily returns bare URL strings, or
   * {@code {url, description}} objects when {@code includeImageDescriptions} is
   * set — one delegating creator absorbs both shapes.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Image(String url, String description) {

    @JsonCreator
    static Image from(Object raw) {
      if (raw instanceof String s) {
        return new Image(s, null);
      }
      if (raw instanceof Map<?, ?> m) {
        return new Image(str(m.get("url")), str(m.get("description")));
      }
      return null;
    }

    private static String str(Object o) {
      return o == null ? null : o.toString();
    }
  }
}
