/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Response;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A defacto implementation of the {@code Response} interface
 * 
 * @since 1.0
 */
public class HttpResponse implements Response {
  private final int statusCode;
  private final Map<String, String> responseHeaders;
  private final Body body;
  private final Map<String, String> context;

  private HttpResponse(final Builder builder) {
    this.statusCode = builder.statusCode;
    checkArgument(HttpUtil.VALID_STATUS_CODES.contains(this.statusCode),
        "statusCode must be a valid status code [%s]", this.statusCode);
    this.responseHeaders = ImmutableMap.copyOf(builder.responseHeaders);
    this.body = checkNotNull(builder.body);
    this.context = ImmutableMap.copyOf(builder.context);
  }

  @Override
  public int getStatusCode() {
    return this.statusCode;
  }

  @Override
  public Map<String, String> headers() {
    return this.responseHeaders;
  }

  @Override
  public Body getBody() {
    return this.body;
  }

  @Override
  public Map<String, String> getContext() {
    return this.context;
  }

  @Override
  public String toString() {
    return String.format(
        "HttpResponse [%n" + "statusCode=%s,%n" + "headers=%s%n" + "body=%s%n" + "context=%s%n]",
        this.statusCode, this.responseHeaders, this.body, this.context);
  }

  /**
   * An http response builder
   */
  public static class Builder {
    private int statusCode;
    private final Map<String, String> responseHeaders;
    private Body body;
    private final Map<String, String> context;

    /**
     * Constructs a builder
     */
    public Builder() {
      this.responseHeaders = Maps.newLinkedHashMap();
      this.body = Bodies.none();
      this.context = Maps.newHashMap();
    }

    public Builder withStatusCode(final int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    /**
     * Configures a response header to include with this response
     * 
     * @param key a header key
     * @param value a header value
     * @return this builder
     */
    public Builder withHeader(final String key, final String value) {
      this.responseHeaders.put(key, value);
      return this;
    }

    /**
     * Configures a response body to include with this response
     * 
     * @param body a body
     * @return this builder
     */
    public Builder withBody(final Body body) {
      this.body = body;
      return this;
    }

    /**
     * Configures a context key to include with this response
     * 
     * @param key a context key
     * @param value a context value
     * @return this builder
     */
    public Builder withContext(final String key, final String value) {
      this.context.put(key, value);
      return this;
    }

    /**
     * Constructs an http response instance
     * 
     * @return an http response instance
     * @throws IllegalArgumentException if an invalid status code was configured with this builder
     * @throws NullPointerException if any null header keys or values were added to this builder
     */
    public HttpResponse build() {
      return new HttpResponse(this);
    }
  }
}
