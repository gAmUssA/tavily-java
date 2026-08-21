package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for {@code POST /extract}: hand Tavily one or more URLs, get clean
 * markdown (or text) back. Pass a {@code query} to have the page's chunks reranked
 * by relevance to it instead of returning the whole page verbatim.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractRequest(
    @JsonProperty("urls")              List<String> urls,
    @JsonProperty("query")             String query,
    @JsonProperty("chunks_per_source") Integer chunksPerSource,
    @JsonProperty("extract_depth")     ExtractDepth extractDepth,
    @JsonProperty("format")            ContentFormat format,
    @JsonProperty("include_images")    Boolean includeImages,
    @JsonProperty("include_favicon")   Boolean includeFavicon,
    @JsonProperty("timeout")           Double timeout,
    @JsonProperty("include_usage")     Boolean includeUsage
) {

  public static ExtractRequest of(String url) {
    return builder().url(url).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private List<String> urls;
    private String query;
    private Integer chunksPerSource;
    private ExtractDepth extractDepth;
    private ContentFormat format;
    private Boolean includeImages;
    private Boolean includeFavicon;
    private Double timeout;
    private Boolean includeUsage;

    public Builder urls(List<String> v) {
      this.urls = v;
      return this;
    }

    public Builder url(String v) {
      this.urls = List.of(v);
      return this;
    }

    public Builder query(String v) {
      this.query = v;
      return this;
    }

    public Builder chunksPerSource(Integer v) {
      this.chunksPerSource = v;
      return this;
    }

    public Builder extractDepth(ExtractDepth v) {
      this.extractDepth = v;
      return this;
    }

    public Builder format(ContentFormat v) {
      this.format = v;
      return this;
    }

    public Builder includeImages(Boolean v) {
      this.includeImages = v;
      return this;
    }

    public Builder includeFavicon(Boolean v) {
      this.includeFavicon = v;
      return this;
    }

    /** Per-request crawl budget in seconds, 1.0–60.0. */
    public Builder timeout(Double v) {
      this.timeout = v;
      return this;
    }

    public Builder includeUsage(Boolean v) {
      this.includeUsage = v;
      return this;
    }

    public ExtractRequest build() {
      if (urls == null || urls.isEmpty()) {
        throw new IllegalStateException("at least one url is required");
      }
      return new ExtractRequest(urls, query, chunksPerSource, extractDepth, format, includeImages,
                                includeFavicon, timeout, includeUsage);
    }
  }
}
