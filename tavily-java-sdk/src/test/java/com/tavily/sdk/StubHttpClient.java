package com.tavily.sdk;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal {@link HttpClient} that answers every call with a canned status + body and
 * records the last request it was handed. Keeps the SDK tests free of a mock server.
 */
final class StubHttpClient extends HttpClient {

  private final int status;
  private final String body;
  final AtomicReference<HttpRequest> lastRequest = new AtomicReference<>();
  final AtomicReference<String> lastBody = new AtomicReference<>();

  StubHttpClient(int status, String body) {
    this.status = status;
    this.body = body;
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
    return capture(request);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> handler) {
    return CompletableFuture.completedFuture(capture(request));
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> handler,
                                                          HttpResponse.PushPromiseHandler<T> push) {
    return CompletableFuture.completedFuture(capture(request));
  }

  private <T> HttpResponse<T> capture(HttpRequest request) {
    lastRequest.set(request);
    request.bodyPublisher().ifPresent(bp -> lastBody.set(BodyCollector.collect(bp)));
    return response();
  }

  @SuppressWarnings("unchecked")
  private <T> HttpResponse<T> response() {
    return (HttpResponse<T>) new HttpResponse<String>() {
      @Override public int statusCode() { return status; }
      @Override public String body() { return body; }
      @Override public HttpRequest request() { return lastRequest.get(); }
      @Override public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
      @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
      @Override public java.net.URI uri() { return null; }
      @Override public Version version() { return Version.HTTP_1_1; }
      @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
    };
  }

  @Override public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
  @Override public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
  @Override public Redirect followRedirects() { return Redirect.NORMAL; }
  @Override public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
  @Override public javax.net.ssl.SSLContext sslContext() { return null; }
  @Override public javax.net.ssl.SSLParameters sslParameters() { return null; }
  @Override public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
  @Override public Version version() { return Version.HTTP_1_1; }
  @Override public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }

  /** Drains an {@link HttpRequest.BodyPublisher} into a String, synchronously. */
  private static final class BodyCollector {

    static String collect(HttpRequest.BodyPublisher publisher) {
      StringBuilder sb = new StringBuilder();
      java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
      publisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
          s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(java.nio.ByteBuffer item) {
          sb.append(java.nio.charset.StandardCharsets.UTF_8.decode(item));
        }

        @Override
        public void onError(Throwable t) {
          done.countDown();
        }

        @Override
        public void onComplete() {
          done.countDown();
        }
      });
      try {
        done.await(5, java.util.concurrent.TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return sb.toString();
    }
  }
}
