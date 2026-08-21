package com.tavily.sdk;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Response fixtures below are trimmed captures of real api.tavily.com payloads —
 * including the fields Tavily returns but doesn't document ({@code follow_up_questions},
 * the {@code title} on extract results).
 */
class TavilyClientTest {

  private StubHttpClient stub;

  private TavilyClient clientWith(int status, String body) {
    stub = new StubHttpClient(status, body);
    return TavilyClient.builder()
        .apiKey("tvly-test-key")
        .baseUrl("https://stub.local")
        .httpClient(stub)
        .objectMapper(TavilyClient.defaultObjectMapper())
        .build();
  }

  // ── Builder validation ────────────────────────────────────────────────────

  @Test
  void builder_throwsWhenApiKeyMissing() {
    assertThrows(IllegalStateException.class, () -> TavilyClient.builder().build());
  }

  @Test
  void builder_throwsWhenApiKeyBlank() {
    assertThrows(IllegalStateException.class, () -> TavilyClient.builder().apiKey("  ").build());
  }

  @Test
  void searchRequest_throwsWhenQueryBlank() {
    assertThrows(IllegalStateException.class, () -> SearchRequest.builder().query(" ").build());
  }

  @Test
  void extractRequest_throwsWhenNoUrls() {
    assertThrows(IllegalStateException.class, () -> ExtractRequest.builder().build());
  }

  // ── Closeable ─────────────────────────────────────────────────────────────

  @Test
  void tavilyClient_implementsCloseable() {
    assertTrue(Closeable.class.isAssignableFrom(TavilyClient.class));
  }

  @Test
  void close_doesNotThrow() {
    TavilyClient client = TavilyClient.builder().apiKey("tvly-key").build();
    assertDoesNotThrow(client::close);
  }

  // ── Wire format: bearer auth + snake_case body ────────────────────────────

  @Test
  void search_sendsBearerAuthAndSnakeCaseBody() {
    TavilyClient client = clientWith(200, """
        {"query":"q","results":[],"response_time":0.4,"request_id":"r0"}
        """);
    client.search(SearchRequest.builder()
                      .query("polaris earning rate")
                      .searchDepth(SearchRequest.SearchDepth.ADVANCED)
                      .topic(SearchRequest.Topic.NEWS)
                      .timeRange(SearchRequest.TimeRange.MONTH)
                      .maxResults(5)
                      .includeAnswer(SearchRequest.AnswerMode.BASIC)
                      .includeRawContent(ContentFormat.MARKDOWN)
                      .includeDomains(List.of("united.com"))
                      .startDate(java.time.LocalDate.of(2026, 1, 31))
                      .build());

    assertEquals("https://stub.local/search", stub.lastRequest.get().uri().toString());
    assertEquals("Bearer tvly-test-key",
                 stub.lastRequest.get().headers().firstValue("Authorization").orElseThrow());

    String body = stub.lastBody.get();
    assertTrue(body.contains("\"search_depth\":\"advanced\""), body);
    assertTrue(body.contains("\"topic\":\"news\""), body);
    assertTrue(body.contains("\"time_range\":\"month\""), body);
    assertTrue(body.contains("\"max_results\":5"), body);
    assertTrue(body.contains("\"include_answer\":\"basic\""), body);
    assertTrue(body.contains("\"include_raw_content\":\"markdown\""), body);
    assertTrue(body.contains("\"include_domains\":[\"united.com\"]"), body);
    // LocalDate must go out as YYYY-MM-DD, not an epoch array
    assertTrue(body.contains("\"start_date\":\"2026-01-31\""), body);
    // unset options must not be sent at all
    assertTrue(!body.contains("include_images"), body);
  }

  @Test
  void search_includeAnswerBooleanFormSerializesAsBoolean() {
    TavilyClient client = clientWith(200, """
        {"query":"q","results":[]}
        """);
    client.search(SearchRequest.builder().query("q").includeAnswer(true).build());
    assertTrue(stub.lastBody.get().contains("\"include_answer\":true"), stub.lastBody.get());
  }

  // ── Search ────────────────────────────────────────────────────────────────

  @Test
  void search_parsesSuccessResponse() {
    TavilyClient client = clientWith(200, """
        {"query":"polaris earning rate","follow_up_questions":null,"answer":"5x miles.",
         "images":[],
         "results":[
           {"url":"https://upgradedpoints.com/x","title":"MileagePlus Review",
            "content":"Status | Earning Rate | …","score":0.7389,"raw_content":null,
            "favicon":"https://upgradedpoints.com/favicon.png","id":"6bf310-00"}],
         "response_time":1.42,"usage":{"credits":1},"request_id":"req-1"}
        """);

    SearchResponse resp = client.search(SearchRequest.of("polaris earning rate"));

    assertEquals("polaris earning rate", resp.query());
    assertEquals("5x miles.", resp.answer());
    assertEquals("req-1", resp.requestId());
    assertEquals(1.42, resp.responseTime());
    assertEquals(1.0, resp.usage().credits());
    assertNull(resp.followUpQuestions());

    SearchResponse.Result first = resp.results().get(0);
    assertEquals("MileagePlus Review", first.title());
    assertEquals("https://upgradedpoints.com/x", first.url());
    assertEquals("Status | Earning Rate | …", first.content());
    assertEquals(0.7389, first.score());
    assertNull(first.rawContent());
    assertEquals("6bf310-00", first.id());
  }

  @Test
  void search_parsesBothImageShapes() {
    TavilyClient client = clientWith(200, """
        {"query":"q","results":[],
         "images":["https://a.example/1.png",
                   {"url":"https://b.example/2.png","description":"a chart"}]}
        """);

    SearchResponse resp = client.search(SearchRequest.of("q"));
    assertEquals("https://a.example/1.png", resp.images().get(0).url());
    assertNull(resp.images().get(0).description());
    assertEquals("a chart", resp.images().get(1).description());
  }

  // ── Extract ───────────────────────────────────────────────────────────────

  @Test
  void extract_parsesResultsAndFailedResults() {
    TavilyClient client = clientWith(200, """
        {"results":[
           {"url":"https://docs.tavily.com/welcome","title":"Welcome",
            "raw_content":"# Welcome","images":[],
            "favicon":"https://docs.tavily.com/favicon.png"}],
         "failed_results":[
           {"url":"https://www.united.com/gated","error":"Request timed out"}],
         "response_time":2.1,"usage":{"credits":0},"request_id":"req-2"}
        """);

    ExtractResponse resp = client.extract(ExtractRequest.builder()
                                              .url("https://docs.tavily.com/welcome")
                                              .extractDepth(ExtractDepth.ADVANCED)
                                              .format(ContentFormat.MARKDOWN)
                                              .build());

    assertEquals("# Welcome", resp.results().get(0).rawContent());
    assertEquals("Welcome", resp.results().get(0).title());
    assertEquals("Request timed out", resp.failedResults().get(0).error());
    assertEquals("req-2", resp.requestId());

    String body = stub.lastBody.get();
    assertTrue(body.contains("\"extract_depth\":\"advanced\""), body);
    assertTrue(body.contains("\"format\":\"markdown\""), body);
    assertTrue(body.contains("\"urls\":[\"https://docs.tavily.com/welcome\"]"), body);
  }

  /** A URL Tavily can't read is a failed_result, not an HTTP error — the tool layer depends on this. */
  @Test
  void extract_allUrlsFailedStillReturns200Shape() {
    TavilyClient client = clientWith(200, """
        {"results":[],"failed_results":[{"url":"https://dead.example","error":"Failed to fetch url"}],
         "response_time":0.9,"request_id":"req-3"}
        """);

    ExtractResponse resp = client.extract(ExtractRequest.of("https://dead.example"));
    assertTrue(resp.results().isEmpty());
    assertEquals("https://dead.example", resp.failedResults().get(0).url());
  }

  // ── Crawl / Map ───────────────────────────────────────────────────────────

  @Test
  void crawl_parsesResponseAndSendsInstructions() {
    TavilyClient client = clientWith(200, """
        {"base_url":"https://docs.tavily.com",
         "results":[{"url":"https://docs.tavily.com/welcome","raw_content":"# Welcome"}],
         "response_time":6.3,"usage":{"credits":0},"request_id":"req-4"}
        """);

    CrawlResponse resp = client.crawl(CrawlRequest.builder()
                                          .url("https://docs.tavily.com")
                                          .instructions("pages about the search endpoint")
                                          .maxDepth(1)
                                          .limit(3)
                                          .build());

    assertEquals("https://docs.tavily.com", resp.baseUrl());
    assertEquals("# Welcome", resp.results().get(0).rawContent());
    String body = stub.lastBody.get();
    assertTrue(body.contains("\"instructions\":\"pages about the search endpoint\""), body);
    assertTrue(body.contains("\"max_depth\":1"), body);
  }

  @Test
  void map_parsesUrlList() {
    TavilyClient client = clientWith(200, """
        {"base_url":"https://docs.tavily.com",
         "results":["https://docs.tavily.com/","https://docs.tavily.com/welcome"],
         "response_time":0.07,"usage":{"credits":0},"request_id":"req-5"}
        """);

    MapResponse resp = client.map(MapRequest.builder().url("https://docs.tavily.com").limit(5).build());
    assertEquals(2, resp.results().size());
    assertEquals("https://docs.tavily.com/welcome", resp.results().get(1));
  }

  // ── Error handling ────────────────────────────────────────────────────────

  @Test
  void search_throwsTavilyExceptionOn401() {
    TavilyClient client = clientWith(401, "{\"detail\":{\"error\":\"Unauthorized: missing or invalid API key.\"}}");
    TavilyException ex = assertThrows(TavilyException.class,
                                      () -> client.search(SearchRequest.of("test")));
    assertEquals(401, ex.statusCode());
    assertTrue(ex.responseBody().contains("Unauthorized"));
  }

  @Test
  void search_throwsTavilyExceptionOn432UsageLimit() {
    TavilyClient client = clientWith(432, "{\"detail\":{\"error\":\"Plan limit exceeded.\"}}");
    TavilyException ex = assertThrows(TavilyException.class,
                                      () -> client.search(SearchRequest.of("test")));
    assertEquals(432, ex.statusCode());
  }

  @Test
  void search_throwsTavilyExceptionOnUnparseableBody() {
    TavilyClient client = clientWith(200, "not json at all");
    TavilyException ex = assertThrows(TavilyException.class,
                                      () -> client.search(SearchRequest.of("test")));
    assertTrue(ex.getMessage().contains("Failed to parse"));
  }

  // ── Async ─────────────────────────────────────────────────────────────────

  @Test
  void searchAsync_returnsCompletableFuture() {
    TavilyClient client = clientWith(200, """
        {"query":"async","results":[],"request_id":"async-1"}
        """);
    CompletableFuture<SearchResponse> future = client.searchAsync(SearchRequest.of("async"));
    assertNotNull(future);
    assertEquals("async-1", future.join().requestId());
  }

  @Test
  void extractAsync_failsWithTavilyExceptionOn4xx() {
    TavilyClient client = clientWith(403, "{\"detail\":\"Forbidden\"}");
    CompletableFuture<ExtractResponse> future =
        client.extractAsync(ExtractRequest.of("https://x.example"));
    Exception ex = assertThrows(Exception.class, future::join);
    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
    assertInstanceOf(TavilyException.class, cause);
    assertEquals(403, ((TavilyException) cause).statusCode());
  }
}
