/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

/**
 * A supplier of requests
 * 
 * @since 1.0
 */
public class RequestSupplier implements Supplier<Request> {
  private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");
  private final Supplier<String> id;
  private final Method method;
  private final Scheme scheme;
  private final Supplier<String> host;
  private final Integer port;
  private final String uriRoot;
  private final Supplier<String> container;
  private final CachingSupplier<String> object;
  private final Map<String, String> queryParameters;
  private final boolean trailingSlash;
  private final Map<String, String> headers;
  private final String username;
  private final String password;
  private final Supplier<Body> body;

  public RequestSupplier(final Supplier<String> id, final Method method, final Scheme scheme,
      final Supplier<String> host, final Integer port, final String uriRoot,
      final Supplier<String> container, final CachingSupplier<String> object,
      final Map<String, String> queryParameters, final boolean trailingSlash,
      final Map<String, String> headers, final String username, final String password,
      final Supplier<Body> body) {

    this.id = id;
    this.method = checkNotNull(method);
    this.scheme = checkNotNull(scheme);
    this.host = checkNotNull(host);
    this.port = port;
    this.uriRoot = uriRoot;
    this.container = checkNotNull(container);
    this.object = object;
    this.queryParameters = ImmutableMap.copyOf(queryParameters);
    this.trailingSlash = trailingSlash;
    this.headers = ImmutableMap.copyOf(headers);
    this.username = username;
    this.password = password;
    this.body = body;
  }

  @Override
  public Request get() {
    final HttpRequest.Builder context = new HttpRequest.Builder(this.method, getUrl());

    for (final Map.Entry<String, String> header : this.headers.entrySet()) {
      context.withHeader(header.getKey(), header.getValue());
    }

    if (this.id != null)
      context.withHeader(Headers.X_OG_REQUEST_ID, this.id.get());

    if (this.object != null)
      context.withHeader(Headers.X_OG_OBJECT_NAME, this.object.getCachedValue());

    if (this.username != null && this.password != null) {
      context.withHeader(Headers.X_OG_USERNAME, username);
      context.withHeader(Headers.X_OG_PASSWORD, password);
    }

    if (this.body != null)
      context.withBody(this.body.get());

    return context.build();
  }

  private URI getUrl() {
    final StringBuilder s =
        new StringBuilder().append(this.scheme).append("://").append(this.host.get());
    appendPort(s);
    appendPath(s);
    appendTrailingSlash(s);
    appendQueryParams(s);

    try {
      return new URI(s.toString());
    } catch (final URISyntaxException e) {
      // Wrapping checked exception as unchecked because most callers will not be able to handle
      // it and I don't want to include URISyntaxException in the entire signature chain
      throw new IllegalArgumentException(e);
    }
  }

  private void appendPort(final StringBuilder s) {
    if (this.port != null)
      s.append(":").append(this.port);
  }

  private void appendPath(final StringBuilder s) {
    s.append("/");
    if (this.uriRoot != null)
      s.append(this.uriRoot).append("/");

    s.append(this.container.get());

    if (this.object != null)
      s.append("/").append(this.object.get());
  }

  private void appendTrailingSlash(final StringBuilder s) {
    if (this.trailingSlash)
      s.append("/");
  }

  private void appendQueryParams(final StringBuilder s) {
    final String queryParams = PARAM_JOINER.join(this.queryParameters);
    if (queryParams.length() > 0)
      s.append("?").append(queryParams);
  }

  @Override
  public String toString() {
    return String.format("RequestSupplier [%n" + "method=%s,%n" + "scheme=%s,%n" + "host=%s,%n"
        + "port=%s,%n" + "uriRoot=%s,%n" + "container=%s,%n" + "object=%s,%n"
        + "queryParameters=%s,%n" + "trailingSlash=%s,%n" + "headers=%s,%n" + "body=%s%n" + "]",
        this.method, this.scheme, this.host, this.port, this.uriRoot, this.container, this.object,
        this.queryParameters, this.trailingSlash, this.headers, this.body);
  }
}
