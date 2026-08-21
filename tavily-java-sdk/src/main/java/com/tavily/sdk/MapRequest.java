package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for {@code POST /map}: same traversal as {@code /crawl}, but it
 * returns the URL graph only — no page extraction, so it's fast and cheap when you
 * just want to know what a site contains.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MapRequest(
    @JsonProperty("url")             String url,
    @JsonProperty("instructions")    String instructions,
    @JsonProperty("max_depth")       Integer maxDepth,
    @JsonProperty("max_breadth")     Integer maxBreadth,
    @JsonProperty("limit")           Integer limit,
    @JsonProperty("select_paths")    List<String> selectPaths,
    @JsonProperty("select_domains")  List<String> selectDomains,
    @JsonProperty("exclude_paths")   List<String> excludePaths,
    @JsonProperty("exclude_domains") List<String> excludeDomains,
    @JsonProperty("allow_external")  Boolean allowExternal,
    @JsonProperty("timeout")         Double timeout,
    @JsonProperty("include_usage")   Boolean includeUsage
) {

  public static MapRequest of(String url) {
    return builder().url(url).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String url;
    private String instructions;
    private Integer maxDepth;
    private Integer maxBreadth;
    private Integer limit;
    private List<String> selectPaths;
    private List<String> selectDomains;
    private List<String> excludePaths;
    private List<String> excludeDomains;
    private Boolean allowExternal;
    private Double timeout;
    private Boolean includeUsage;

    public Builder url(String v) {
      this.url = v;
      return this;
    }

    public Builder instructions(String v) {
      this.instructions = v;
      return this;
    }

    public Builder maxDepth(Integer v) {
      this.maxDepth = v;
      return this;
    }

    public Builder maxBreadth(Integer v) {
      this.maxBreadth = v;
      return this;
    }

    public Builder limit(Integer v) {
      this.limit = v;
      return this;
    }

    public Builder selectPaths(List<String> v) {
      this.selectPaths = v;
      return this;
    }

    public Builder selectDomains(List<String> v) {
      this.selectDomains = v;
      return this;
    }

    public Builder excludePaths(List<String> v) {
      this.excludePaths = v;
      return this;
    }

    public Builder excludeDomains(List<String> v) {
      this.excludeDomains = v;
      return this;
    }

    public Builder allowExternal(Boolean v) {
      this.allowExternal = v;
      return this;
    }

    public Builder timeout(Double v) {
      this.timeout = v;
      return this;
    }

    public Builder includeUsage(Boolean v) {
      this.includeUsage = v;
      return this;
    }

    public MapRequest build() {
      if (url == null || url.isBlank()) {
        throw new IllegalStateException("url is required");
      }
      return new MapRequest(url, instructions, maxDepth, maxBreadth, limit, selectPaths,
                            selectDomains, excludePaths, excludeDomains, allowExternal, timeout,
                            includeUsage);
    }
  }
}
