package io.gamov.loyalty;

import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Single-request event bus that lets tool methods emit progress events into the
 * SSE stream while the agent loop runs. Application-scoped on purpose: the demo
 * only handles one query at a time, so a shared queue is sufficient.
 */
@ApplicationScoped
public class AgentEventBus {

  private final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();

  public void emit(String event) {
    events.add(event);
  }

  public String poll() {
    return events.poll();
  }

  public void reset() {
    events.clear();
  }
}
