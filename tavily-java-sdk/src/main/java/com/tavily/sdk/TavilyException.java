package com.tavily.sdk;

public class TavilyException extends RuntimeException {

  private final int statusCode;
  private final String responseBody;

  public TavilyException(int statusCode, String responseBody, String message) {
    super(message);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public TavilyException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = -1;
    this.responseBody = null;
  }

  /** HTTP status of the failed call, or {@code -1} if the call never got a response. */
  public int statusCode() {
    return statusCode;
  }

  public String responseBody() {
    return responseBody;
  }
}
