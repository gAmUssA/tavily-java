package io.gamov.loyalty;

import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.concurrent.CompletableFuture;

import dev.langchain4j.model.chat.ChatModel;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/agent")
public class AgentResource {

  @Inject
  LoyaltyAgent agent;

  @Inject
  ChatModel rawModel;

  @Inject
  AgentEventBus eventBus;

  @GET
  @Path("/stream")
  @RestStreamElementType(MediaType.TEXT_PLAIN)
  public Multi<String> stream(@QueryParam("q") String question,
                              @QueryParam("mode") String mode) {
    if (question == null || question.isBlank()) {
      return Multi.createFrom().items("[error] missing 'q' query parameter");
    }
    boolean raw = "raw".equalsIgnoreCase(mode);
    eventBus.reset();
    return Multi.createFrom().<String>emitter(emitter -> {
      emitter.emit("[query] " + question);
      if (raw) emitter.emit("[mode] raw — no tools, no web search");
      long start = System.currentTimeMillis();

      CompletableFuture<String> future = CompletableFuture.supplyAsync(
          () -> raw
              ? rawModel.chat(question)
              : agent.ask(question),
          Infrastructure.getDefaultWorkerPool()
      );

      try {
        while (!future.isDone()) {
          drainTo(emitter);
          Thread.sleep(80);
        }
        drainTo(emitter);
        String answer = future.get();
        for (String line : answer.split("\n")) {
          emitter.emit("[answer] " + line);
        }
        emitter.emit("[done] " + (System.currentTimeMillis() - start) + "ms");
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        emitter.emit("[error] interrupted");
      } catch (Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        emitter.emit("[error] " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
      } finally {
        emitter.complete();
      }
    }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }

  private void drainTo(io.smallrye.mutiny.subscription.MultiEmitter<? super String> emitter) {
    String ev;
    while ((ev = eventBus.poll()) != null) {
      emitter.emit(ev);
    }
  }
}
