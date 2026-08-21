package io.gamov.loyalty;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.tavily.sdk.TavilyClient;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TavilyConfig {

  // Optional because SmallRye Config treats an empty env var as missing —
  // a plain String here would fail injection before the friendly check below runs.
  @ConfigProperty(name = "tavily.api.key")
  Optional<String> apiKey;

  @Produces
  @ApplicationScoped
  public TavilyClient tavilyClient() {
    return TavilyClient.builder().apiKey(apiKey.orElseThrow(TavilyConfig::missingKey)).build();
  }

  void onStart(@Observes StartupEvent ev) {
    if (apiKey.isEmpty() || apiKey.get().isBlank()) {
      throw missingKey();
    }
  }

  private static IllegalStateException missingKey() {
    return new IllegalStateException(
        "TAVILY_API_KEY is not set. Add it to .env or export it before starting the app.");
  }
}
