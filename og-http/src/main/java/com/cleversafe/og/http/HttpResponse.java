//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Mar 11, 2014
// ---------------------

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
  private final Map<String, String> headers;
  private final Body body;

  private HttpResponse(final Builder builder) {
    this.statusCode = builder.statusCode;
    checkArgument(HttpUtil.VALID_STATUS_CODES.contains(this.statusCode),
        "statusCode must be a valid status code [%s]", this.statusCode);
    this.headers = ImmutableMap.copyOf(builder.headers);
    this.body = checkNotNull(builder.body);

  }

  @Override
  public int getStatusCode() {
    return this.statusCode;
  }

  @Override
  public Map<String, String> headers() {
    return this.headers;
  }

  @Override
  public Body getBody() {
    return this.body;
  }

  /**
   * An http response builder
   */
  public static class Builder {
    private int statusCode;
    private final Map<String, String> headers;
    private Body body;

    /**
     * Constructs a builder
     */
    public Builder() {
      this.headers = Maps.newLinkedHashMap();
      this.body = Bodies.none();
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
      this.headers.put(key, value);
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
