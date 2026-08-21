package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for {@code POST /crawl}. Tavily's crawl is agentic: give it a start
 * URL plus plain-English {@code instructions} ("find the award chart pages") and it
 * decides which links to follow, instead of you writing path regexes — though
 * {@code selectPaths} / {@code excludePaths} are there when you want the leash.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CrawlRequest(
    @JsonProperty("url")               String url,
    @JsonProperty("instructions")      String instructions,
    @JsonProperty("max_depth")         Integer maxDepth,
    @JsonProperty("max_breadth")       Integer maxBreadth,
    @JsonProperty("limit")             Integer limit,
    @JsonProperty("chunks_per_source") Integer chunksPerSource,
    @JsonProperty("select_paths")      List<String> selectPaths,
    @JsonProperty("select_domains")    List<String> selectDomains,
    @JsonProperty("exclude_paths")     List<String> excludePaths,
    @JsonProperty("exclude_domains")   List<String> excludeDomains,
    @JsonProperty("allow_external")    Boolean allowExternal,
    @JsonProperty("extract_depth")     ExtractDepth extractDepth,
    @JsonProperty("format")            ContentFormat format,
    @JsonProperty("include_images")    Boolean includeImages,
    @JsonProperty("include_favicon")   Boolean includeFavicon,
    @JsonProperty("timeout")           Double timeout,
    @JsonProperty("include_usage")     Boolean includeUsage
) {

  public static CrawlRequest of(String url) {
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
    private Integer chunksPerSource;
    private List<String> selectPaths;
    private List<String> selectDomains;
    private List<String> excludePaths;
    private List<String> excludeDomains;
    private Boolean allowExternal;
    private ExtractDepth extractDepth;
    private ContentFormat format;
    private Boolean includeImages;
    private Boolean includeFavicon;
    private Double timeout;
    private Boolean includeUsage;

    public Builder url(String v) {
      this.url = v;
      return this;
    }

    /** Plain-English steering, e.g. {@code "only pages about award charts"}. */
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

    public Builder chunksPerSource(Integer v) {
      this.chunksPerSource = v;
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

    /** Crawl budget in seconds, 10–150. */
    public Builder timeout(Double v) {
      this.timeout = v;
      return this;
    }

    public Builder includeUsage(Boolean v) {
      this.includeUsage = v;
      return this;
    }

    public CrawlRequest build() {
      if (url == null || url.isBlank()) {
        throw new IllegalStateException("url is required");
      }
      return new CrawlRequest(url, instructions, maxDepth, maxBreadth, limit, chunksPerSource,
                              selectPaths, selectDomains, excludePaths, excludeDomains,
                              allowExternal, extractDepth, format, includeImages, includeFavicon,
                              timeout, includeUsage);
    }
  }
}
