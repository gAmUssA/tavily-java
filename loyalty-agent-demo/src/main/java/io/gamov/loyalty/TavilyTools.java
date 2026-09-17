package io.gamov.loyalty;

import org.jboss.logging.Logger;

import com.tavily.sdk.ContentFormat;
import com.tavily.sdk.ExtractDepth;
import com.tavily.sdk.ExtractRequest;
import com.tavily.sdk.ExtractResponse;
import com.tavily.sdk.SearchRequest;
import com.tavily.sdk.SearchResponse;
import com.tavily.sdk.TavilyClient;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TavilyTools {

  private static final Logger LOG = Logger.getLogger(TavilyTools.class);

  /**
   * Cap on text handed back to the model (~15k tokens). Keep it generous: guides like
   * NerdWallet's MileagePlus page put the earning table past character 12,000, and a tighter
   * cap had the model fill the missing table in from memory while still citing the page.
   */
  private static final int MAX_PAGE_CHARS = 60_000;

  @Inject
  TavilyClient tavily;

  @Inject
  AgentEventBus events;

  @Tool("""
      Search the live web for current information. Use this whenever the user asks about facts \
      that may have changed recently (airline loyalty programs, award charts, transfer ratios, \
      devaluation announcements, fare-class earnings rates, etc.). The LLM's training data is \
      stale — this tool sees the live web. Returns a list of {title, url, snippet} results. \
      Snippets are short. Call extractUrl on the most relevant result to get full page content.
      """)
  public String webSearch(
      @P("The search query in plain English (e.g., 'United MileagePlus Polaris business earning rate 2026')") String query,
      @P("How recent results should be: 'day', 'week', 'month', 'year', or null for any time. Use 'month' or 'week' for current loyalty program rules.") String recency) {

    SearchRequest.TimeRange timeRange = parseRecency(recency);
    LOG.infof("→ tavily.search(query=%s, timeRange=%s)", query, timeRange);
    events.emit("[tool] → tavily.search query=\"" + query + "\""
                + (timeRange == null ? "" : " time_range=" + timeRange.name().toLowerCase()));
    long t0 = System.currentTimeMillis();

    SearchRequest req = SearchRequest.builder()
        .query(query)
        .maxResults(5)
        .searchDepth(SearchRequest.SearchDepth.BASIC)
        .topic(SearchRequest.Topic.GENERAL)
        .timeRange(timeRange)
        .build();

    SearchResponse res = tavily.search(req);
    long elapsed = System.currentTimeMillis() - t0;
    int count = res.results() == null ? 0 : res.results().size();
    LOG.infof("← tavily.search %d results in %dms", count, elapsed);
    events.emit("[tool] ← " + count + " results in " + elapsed + "ms");

    if (count == 0) {
      return "No results found for: " + query;
    }

    StringBuilder out = new StringBuilder();
    out.append("Found ").append(count).append(" results for: ").append(query).append("\n\n");
    for (int i = 0; i < count; i++) {
      SearchResponse.Result r = res.results().get(i);
      out.append("[").append(i + 1).append("] ").append(r.title()).append("\n");
      out.append("    URL: ").append(r.url()).append("\n");
      if (r.publishedDate() != null) {
        out.append("    Published: ").append(r.publishedDate()).append("\n");
      }
      if (r.score() != null) {
        out.append("    Relevance: ").append(String.format("%.2f", r.score())).append("\n");
      }
      String snippet = truncate(r.content(), 800);
      if (snippet != null) {
        out.append("    Snippet: ").append(snippet).append("\n");
      }
      out.append("\n");
    }
    return out.toString();
  }

  @Tool("""
      Fetch the full contents of a specific web page as clean markdown. Use this when you have \
      a URL (typically from webSearch) and need the full text — e.g., to compare a devaluation \
      post against current charts, or to extract a table that didn't fit in a search snippet. \
      \
      If the response says the page could not be fetched, the page is gated or dead. Pick a \
      different URL from the search results and try once more — DO NOT retry the same URL.
      """)
  public String extractUrl(
      @P("The fully-qualified URL to extract") String url) {

    LOG.infof("→ tavily.extract(url=%s)", url);
    events.emit("[tool] → tavily.extract " + url);
    long t0 = System.currentTimeMillis();

    ExtractRequest req = ExtractRequest.builder()
        .url(url)
        // ADVANCED pays for itself here: airline and blog pages hide the numbers in
        // tables and lazily-rendered blocks that basic extraction drops.
        .extractDepth(ExtractDepth.ADVANCED)
        .format(ContentFormat.MARKDOWN)
        .timeout(30.0)
        .build();

    ExtractResponse res = tavily.extract(req);
    long elapsed = System.currentTimeMillis() - t0;

    // Tavily reports an unreadable URL as a failed_result on a 200 — not an HTTP error.
    if (res.results() == null || res.results().isEmpty()) {
      String reason = res.failedResults() != null && !res.failedResults().isEmpty()
                      ? res.failedResults().get(0).error()
                      : "no content returned";
      LOG.infof("← tavily.extract failed: %s in %dms", reason, elapsed);
      events.emit("[tool] ← failed: " + reason + " in " + elapsed + "ms");
      return "[Could not fetch %s: %s. Pick a different URL from the search results.]"
          .formatted(url, reason);
    }

    ExtractResponse.Result page = res.results().get(0);
    String text = page.rawContent();
    int len = text == null ? 0 : text.length();
    LOG.infof("← tavily.extract success, %d chars in %dms", len, elapsed);
    events.emit("[tool] ← success " + len + " chars in " + elapsed + "ms");

    if (text == null || text.isBlank()) {
      return "[Fetched %s but the page had no readable text. Pick a different URL.]".formatted(url);
    }
    if (text.length() > MAX_PAGE_CHARS) {
      text = text.substring(0, MAX_PAGE_CHARS)
             + "\n\n[TRUNCATED: showing %d of %d chars. Anything past this point is NOT in view. Do not state figures you did not see above.]"
                 .formatted(MAX_PAGE_CHARS, len);
    }
    return "Page: " + url + "\n\n" + text;
  }

  private static SearchRequest.TimeRange parseRecency(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return switch (s.toLowerCase()) {
      case "day", "d"   -> SearchRequest.TimeRange.DAY;
      case "week", "w"  -> SearchRequest.TimeRange.WEEK;
      case "month", "m" -> SearchRequest.TimeRange.MONTH;
      case "year", "y"  -> SearchRequest.TimeRange.YEAR;
      default           -> null;    // models will send "recent", "any", "null" — degrade gracefully
    };
  }

  private static String truncate(String s, int max) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.length() > max ? s.substring(0, max) + "…[truncated]" : s;
  }
}
