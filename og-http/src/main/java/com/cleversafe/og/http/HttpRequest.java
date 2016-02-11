/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A defacto implementation of the {@code Request} interface
 * 
 * @since 1.0
 */
public class HttpRequest implements Request {
  private final Method method;
  private final URI uri;
  private final Map<String, List<String>> queryParameters;
  private final Map<String, String> requestHeaders;
  private final Body body;
  private final long messageTime;
  private static final DateTimeFormatter RFC1123 =
      DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z").withLocale(Locale.US);

  private HttpRequest(final Builder builder) {
    this.method = checkNotNull(builder.method);
    this.uri = checkNotNull(builder.uri);
    // recursively immutable copy
    final ImmutableMap.Builder<String, List<String>> queryParametersBuilder =
        ImmutableMap.builder();
    for (final Map.Entry<String, List<String>> entry : builder.queryParameters.entrySet()) {
      queryParametersBuilder.put(entry.getKey(),
          // cannot use ImmutableList.copyOf because it rejects null values
          // must take null values to support query parameter keys without values
          Collections.unmodifiableList(Lists.newArrayList(entry.getValue())));
    }
    this.queryParameters = queryParametersBuilder.build();
    this.requestHeaders = ImmutableMap.copyOf(builder.requestHeaders);
    this.body = checkNotNull(builder.body);
    this.messageTime = builder.messageTime;
  }

  @Override
  public Method getMethod() {
    return this.method;
  }

  @Override
  public URI getUri() {
    return this.uri;
  }

  @Override
  public Map<String, List<String>> getQueryParameters() {
    return this.queryParameters;
  }

  @Override
  public Map<String, String> headers() {
    return this.requestHeaders;
  }

  @Override
  public Body getBody() {
    return this.body;
  }

  @Override
  public long getMessageTime() {
    return this.messageTime;
  }

  @Override
  public String toString() {
    return String.format(
        "HttpRequest [%n" + "method=%s,%n" + "uri=%s,%n" + "queryParameters=%s,%n" + "headers=%s%n"
            + "body=%s%n]",
        this.method, this.uri, this.queryParameters, this.requestHeaders, this.body);
  }

  /**
   * An http request builder
   */
  public static class Builder {
    private final Method method;
    private final URI uri;
    private final Map<String, List<String>> queryParameters;
    private final Map<String, String> requestHeaders;
    private Body body;
    private long messageTime;

    /**
     * Constructs a builder
     * <p>
     * Note: this builder automatically includes a {@code Date} header with an rfc1123 formatted
     * datetime set to the time of builder construction
     * 
     * @param method the request method for this request
     * @param uri the uri for this request
     */
    public Builder(final Method method, final URI uri) {
      this.method = method;
      this.uri = uri;
      this.queryParameters = Maps.newLinkedHashMap();
      this.requestHeaders = Maps.newLinkedHashMap();
      this.messageTime = System.currentTimeMillis();
      this.requestHeaders.put("Date", RFC1123.print(new DateTime(this.messageTime)));
      this.body = Bodies.none();
    }

    /**
     * Configures a request query parameter to include with this request
     * 
     * @param key a query parameter key
     * @param value a query parameter value
     * @return this builder
     */
    public Builder withQueryParameter(final String key, final String value) {
      List<String> parameterValues = this.queryParameters.get(checkNotNull(key));
      if (parameterValues == null) {
        parameterValues = Lists.newArrayList();
        this.queryParameters.put(key, parameterValues);
      }
      parameterValues.add(value);
      return this;
    }

    /**
     * Configures a request header to include with this request
     * 
     * @param key a header key
     * @param value a header value
     * @return this builder
     */
    public Builder withHeader(final String key, final String value) {
      this.requestHeaders.put(key, value);
      return this;
    }

    /**
     * Configures a request body to include with this request
     * 
     * @param body a body
     * @return this builder
     */
    public Builder withBody(final Body body) {
      this.body = body;
      return this;
    }

    public Builder withMessageTime(final long messageTime) {
      this.messageTime = messageTime;
      this.requestHeaders.put("Date", RFC1123.print(new DateTime(messageTime)));
      return this;
    }

    /**
     * Constructs an http request instance
     * 
     * @return an http request instance
     * @throws NullPointerException if any null header keys or values were added to this builder
     */
    public HttpRequest build() {
      return new HttpRequest(this);
    }
  }
}
