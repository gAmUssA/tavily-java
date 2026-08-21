package com.tavily.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for {@code POST /search}. Tavily's wire format is snake_case, so
 * every field carries an explicit {@link JsonProperty} — that keeps the mapping
 * correct even when a caller supplies their own {@code ObjectMapper}.
 *
 * <p>{@code includeAnswer} and {@code includeRawContent} are {@code Object} because
 * the API accepts either a boolean or a mode string ({@code "basic"}/{@code "advanced"},
 * {@code "markdown"}/{@code "text"}). Use the typed builder overloads.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRequest(
    @JsonProperty("query")                      String query,
    @JsonProperty("search_depth")               SearchDepth searchDepth,
    @JsonProperty("topic")                      Topic topic,
    @JsonProperty("max_results")                Integer maxResults,
    @JsonProperty("chunks_per_source")          Integer chunksPerSource,
    @JsonProperty("time_range")                 TimeRange timeRange,
    @JsonProperty("start_date")                 LocalDate startDate,
    @JsonProperty("end_date")                   LocalDate endDate,
    @JsonProperty("include_answer")             Object includeAnswer,
    @JsonProperty("include_raw_content")        Object includeRawContent,
    @JsonProperty("include_images")             Boolean includeImages,
    @JsonProperty("include_image_descriptions") Boolean includeImageDescriptions,
    @JsonProperty("include_favicon")            Boolean includeFavicon,
    @JsonProperty("include_domains")            List<String> includeDomains,
    @JsonProperty("exclude_domains")            List<String> excludeDomains,
    @JsonProperty("country")                    String country,
    @JsonProperty("auto_parameters")            Boolean autoParameters,
    @JsonProperty("exact_match")                Boolean exactMatch,
    @JsonProperty("safe_search")                Boolean safeSearch,
    @JsonProperty("include_usage")              Boolean includeUsage
) {

  /** Latency/quality tier. {@code BASIC} is the default; {@code ADVANCED} reranks harder. */
  public enum SearchDepth {
    @JsonProperty("ultra-fast") ULTRA_FAST,
    @JsonProperty("fast")       FAST,
    @JsonProperty("basic")      BASIC,
    @JsonProperty("advanced")   ADVANCED
  }

  /** Which Tavily agent handles the query. */
  public enum Topic {
    @JsonProperty("general") GENERAL,
    @JsonProperty("news")    NEWS,
    @JsonProperty("finance") FINANCE
  }

  /** Recency window, counted back from today. */
  public enum TimeRange {
    @JsonProperty("day")   DAY,
    @JsonProperty("week")  WEEK,
    @JsonProperty("month") MONTH,
    @JsonProperty("year")  YEAR
  }

  /** Mode for {@code include_answer}: an LLM answer synthesized over the results. */
  public enum AnswerMode {
    BASIC("basic"),
    ADVANCED("advanced");

    private final String wire;

    AnswerMode(String wire) {
      this.wire = wire;
    }

    String wire() {
      return wire;
    }
  }

  public static SearchRequest of(String query) {
    return builder().query(query).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String query;
    private SearchDepth searchDepth;
    private Topic topic;
    private Integer maxResults;
    private Integer chunksPerSource;
    private TimeRange timeRange;
    private LocalDate startDate;
    private LocalDate endDate;
    private Object includeAnswer;
    private Object includeRawContent;
    private Boolean includeImages;
    private Boolean includeImageDescriptions;
    private Boolean includeFavicon;
    private List<String> includeDomains;
    private List<String> excludeDomains;
    private String country;
    private Boolean autoParameters;
    private Boolean exactMatch;
    private Boolean safeSearch;
    private Boolean includeUsage;

    public Builder query(String v) {
      this.query = v;
      return this;
    }

    public Builder searchDepth(SearchDepth v) {
      this.searchDepth = v;
      return this;
    }

    public Builder topic(Topic v) {
      this.topic = v;
      return this;
    }

    public Builder maxResults(Integer v) {
      this.maxResults = v;
      return this;
    }

    public Builder chunksPerSource(Integer v) {
      this.chunksPerSource = v;
      return this;
    }

    public Builder timeRange(TimeRange v) {
      this.timeRange = v;
      return this;
    }

    public Builder startDate(LocalDate v) {
      this.startDate = v;
      return this;
    }

    public Builder endDate(LocalDate v) {
      this.endDate = v;
      return this;
    }

    /** {@code true} asks Tavily to synthesize an answer over the results. */
    public Builder includeAnswer(boolean v) {
      this.includeAnswer = v;
      return this;
    }

    public Builder includeAnswer(AnswerMode v) {
      this.includeAnswer = v == null ? null : v.wire();
      return this;
    }

    /** {@code true} returns full page content per result, in Tavily's default format. */
    public Builder includeRawContent(boolean v) {
      this.includeRawContent = v;
      return this;
    }

    public Builder includeRawContent(ContentFormat v) {
      this.includeRawContent = v == null ? null : v.name().toLowerCase();
      return this;
    }

    public Builder includeImages(Boolean v) {
      this.includeImages = v;
      return this;
    }

    public Builder includeImageDescriptions(Boolean v) {
      this.includeImageDescriptions = v;
      return this;
    }

    public Builder includeFavicon(Boolean v) {
      this.includeFavicon = v;
      return this;
    }

    public Builder includeDomains(List<String> v) {
      this.includeDomains = v;
      return this;
    }

    public Builder excludeDomains(List<String> v) {
      this.excludeDomains = v;
      return this;
    }

    /** Full country name, e.g. {@code "united states"} — only honored when topic is general. */
    public Builder country(String v) {
      this.country = v;
      return this;
    }

    /** Let Tavily pick depth/topic/etc. from the query itself. */
    public Builder autoParameters(Boolean v) {
      this.autoParameters = v;
      return this;
    }

    public Builder exactMatch(Boolean v) {
      this.exactMatch = v;
      return this;
    }

    public Builder safeSearch(Boolean v) {
      this.safeSearch = v;
      return this;
    }

    public Builder includeUsage(Boolean v) {
      this.includeUsage = v;
      return this;
    }

    public SearchRequest build() {
      if (query == null || query.isBlank()) {
        throw new IllegalStateException("query is required");
      }
      return new SearchRequest(query, searchDepth, topic, maxResults, chunksPerSource, timeRange,
                               startDate, endDate, includeAnswer, includeRawContent, includeImages,
                               includeImageDescriptions, includeFavicon, includeDomains,
                               excludeDomains, country, autoParameters, exactMatch, safeSearch,
                               includeUsage);
    }
  }
}
