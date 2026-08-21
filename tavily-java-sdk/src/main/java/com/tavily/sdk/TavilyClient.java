package com.tavily.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Client for Tavily's web-access API (https://api.tavily.com).
 * Covers {@code /search}, {@code /extract}, {@code /crawl} and {@code /map}.
 * Auth is a bearer token; all endpoints are POST with JSON bodies.
 */
public final class TavilyClient implements Closeable {

  private static final String DEFAULT_BASE_URL = "https://api.tavily.com";

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient http;
  private final ObjectMapper json;
  private final Duration requestTimeout;

  private TavilyClient(Builder b) {
    this.baseUrl = b.baseUrl;
    this.apiKey = b.apiKey;
    this.requestTimeout = b.requestTimeout;
    this.http = b.httpClient != null ? b.httpClient
                                     : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.json = b.objectMapper != null ? b.objectMapper : defaultObjectMapper();
  }

  /** Mapper that writes java.time types as ISO-8601 strings — {@code YYYY-MM-DD} for dates. */
  public static ObjectMapper defaultObjectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public SearchResponse search(SearchRequest req) {
    return post("/search", req, SearchResponse.class);
  }

  public CompletableFuture<SearchResponse> searchAsync(SearchRequest req) {
    return postAsync("/search", req, SearchResponse.class);
  }

  public ExtractResponse extract(ExtractRequest req) {
    return post("/extract", req, ExtractResponse.class);
  }

  public CompletableFuture<ExtractResponse> extractAsync(ExtractRequest req) {
    return postAsync("/extract", req, ExtractResponse.class);
  }

  public CrawlResponse crawl(CrawlRequest req) {
    return post("/crawl", req, CrawlResponse.class);
  }

  public CompletableFuture<CrawlResponse> crawlAsync(CrawlRequest req) {
    return postAsync("/crawl", req, CrawlResponse.class);
  }

  public MapResponse map(MapRequest req) {
    return post("/map", req, MapResponse.class);
  }

  public CompletableFuture<MapResponse> mapAsync(MapRequest req) {
    return postAsync("/map", req, MapResponse.class);
  }

  private HttpRequest buildHttpRequest(String path, String payload) {
    return HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + path))
        .timeout(requestTimeout)
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(BodyPublishers.ofString(payload))
        .build();
  }

  private <T> T post(String path, Object body, Class<T> responseType) {
    try {
      String payload = json.writeValueAsString(body);
      HttpRequest request = buildHttpRequest(path, payload);
      HttpResponse<String> response = http.send(request, BodyHandlers.ofString());
      return deserialize(response, path, responseType);
    } catch (TavilyException e) {
      throw e;
    } catch (Exception e) {
      throw new TavilyException("Tavily API call failed: " + path, e);
    }
  }

  private <T> CompletableFuture<T> postAsync(String path, Object body, Class<T> responseType) {
    try {
      String payload = json.writeValueAsString(body);
      HttpRequest request = buildHttpRequest(path, payload);
      return http.sendAsync(request, BodyHandlers.ofString())
          .thenApply(response -> deserialize(response, path, responseType));
    } catch (Exception e) {
      return CompletableFuture.failedFuture(new TavilyException("Tavily API call failed: " + path, e));
    }
  }

  private <T> T deserialize(HttpResponse<String> response, String path, Class<T> responseType) {
    int code = response.statusCode();
    if (code >= 200 && code < 300) {
      try {
        return json.readValue(response.body(), responseType);
      } catch (Exception e) {
        throw new TavilyException("Failed to parse response from " + path, e);
      }
    }
    String errBody = response.body();
    String snippet = errBody == null ? "(empty)"
                                     : (errBody.length() > 500 ? errBody.substring(0, 500) + "…" : errBody);
    throw new TavilyException(code, errBody,
                              "Tavily API error %d on POST %s — body: %s".formatted(code, path, snippet));
  }

  @Override
  public void close() {
    http.close();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String apiKey;
    private String baseUrl = DEFAULT_BASE_URL;
    private Duration requestTimeout = Duration.ofSeconds(60);
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    /** Your {@code tvly-…} key from https://app.tavily.com. */
    public Builder apiKey(String v) {
      this.apiKey = v;
      return this;
    }

    public Builder baseUrl(String v) {
      this.baseUrl = v;
      return this;
    }

    public Builder requestTimeout(Duration v) {
      this.requestTimeout = v;
      return this;
    }

    public Builder httpClient(HttpClient v) {
      this.httpClient = v;
      return this;
    }

    public Builder objectMapper(ObjectMapper v) {
      this.objectMapper = v;
      return this;
    }

    public TavilyClient build() {
      if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException("apiKey is required");
      }
      return new TavilyClient(this);
    }
  }
}
