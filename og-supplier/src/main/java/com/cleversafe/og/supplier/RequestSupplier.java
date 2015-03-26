/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkArgument;
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
import com.google.common.collect.Maps;

/**
 * A supplier of requests
 * 
 * @since 1.0
 */
public class RequestSupplier implements Supplier<Request> {
  private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");
  private final Method method;
  private final Scheme scheme;
  private final Supplier<String> host;
  private final Integer port;
  private final String uriRoot;
  private final Supplier<String> container;
  private final CachingSupplier<String> object;
  private final Map<String, String> queryParameters;
  private final boolean trailingSlash;
  private final Map<Supplier<String>, Supplier<String>> headers;
  private final String username;
  private final String password;
  private final Supplier<Body> body;

  private RequestSupplier(final Builder builder) {
    this.method = checkNotNull(builder.method);
    this.scheme = checkNotNull(builder.scheme);
    this.host = checkNotNull(builder.host);
    this.port = builder.port;
    this.uriRoot = builder.uriRoot;
    this.container = checkNotNull(builder.container);
    this.object = builder.object;
    this.queryParameters = ImmutableMap.copyOf(builder.queryParameters);
    this.trailingSlash = builder.trailingSlash;
    this.headers = ImmutableMap.copyOf(builder.headers);
    this.username = builder.username;
    this.password = builder.password;
    this.body = builder.body;
  }

  @Override
  public Request get() {
    final HttpRequest.Builder context = new HttpRequest.Builder(this.method, getUrl());

    for (final Map.Entry<Supplier<String>, Supplier<String>> header : this.headers.entrySet()) {
      context.withHeader(header.getKey().get(), header.getValue().get());
    }

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

  /**
   * A request supplier builder
   */
  public static class Builder {
    private final Method method;
    private Scheme scheme;
    private final Supplier<String> host;
    private Integer port;
    private String uriRoot;
    private final Supplier<String> container;
    private CachingSupplier<String> object;
    private final Map<String, String> queryParameters;
    private boolean trailingSlash;
    private final Map<Supplier<String>, Supplier<String>> headers;
    private String username;
    private String password;
    private Supplier<Body> body;

    /**
     * Constructs a builder instance using the provided method and uri suppliers
     * 
     * @param method a request method supplier
     * @param uri a request uri supplier
     */
    public Builder(final Method method, final Supplier<String> host,
        final Supplier<String> container) {
      this.method = method;
      this.scheme = Scheme.HTTP;
      this.host = host;
      this.container = container;
      this.queryParameters = Maps.newLinkedHashMap();
      this.headers = Maps.newLinkedHashMap();
    }

    /**
     * Configures the uri scheme
     * 
     * @param scheme the uri scheme
     * @return this builder
     */
    public Builder withScheme(final Scheme scheme) {
      this.scheme = scheme;
      return this;
    }

    /**
     * Configures the uri port
     * 
     * @param port the uri port
     * @return this builder
     */
    public Builder onPort(final int port) {
      checkArgument(port > 0 && port < 65536, "port must be in range [1, 65535] [%s]", port);
      this.port = port;
      return this;
    }

    /**
     * Configures the uri root
     * 
     * @param uriRoot the uri root
     * @return this builder
     */
    public Builder withUriRoot(final String uriRoot) {
      this.uriRoot = uriRoot;
      return this;
    }

    /**
     * Configures the object name
     * 
     * @param object the object name
     * @return this builder
     */
    public Builder withObject(final CachingSupplier<String> object) {
      this.object = object;
      return this;
    }

    /**
     * Configures a uri query parameter
     * 
     * @param key the query parameter key
     * @param value the query paremeter value
     * @return this builder
     */
    public Builder withQueryParameter(final String key, final String value) {
      this.queryParameters.put(key, value);
      return this;
    }

    /**
     * Configures a trailing slash at the end of the supplied uri
     * 
     * @return this builder
     */
    public Builder withTrailingSlash() {
      this.trailingSlash = true;
      return this;
    }

    /**
     * Configures a request header to include with this request suppliers
     * 
     * @param key a header key
     * @param value a header value
     * @return this builder
     */
    public Builder withHeader(final String key, final String value) {
      return withHeader(Suppliers.of(key), Suppliers.of(value));
    }

    /**
     * Configures a request header to include with this request supplier, using suppliers for the
     * key and value
     * 
     * @param key a header key
     * @param value a header value
     * @return this builder
     */
    public Builder withHeader(final Supplier<String> key, final Supplier<String> value) {
      this.headers.put(key, value);
      return this;
    }

    /**
     * Configures a request to use credentials
     * 
     * @param username a username
     * @param password a password
     * @return this builder
     */
    public Builder withCredentials(final String username, final String password) {
      this.username = username;
      this.password = password;
      return this;
    }

    /**
     * Configures a request body to include with this request supplier
     * 
     * @param body an body
     * @return this builder
     */
    public Builder withBody(final Body body) {
      return withBody(Suppliers.of(body));
    }

    /**
     * Configures a request body to include with this request supplier, using a supplier for the
     * body
     * 
     * @param body an body
     * @return this builder
     */
    public Builder withBody(final Supplier<Body> body) {
      this.body = checkNotNull(body);
      return this;
    }

    /**
     * Constructs a request supplier instance
     * 
     * @return a request supplier instance
     * @throws NullPointerException if any null header keys or values were added to this builder
     */
    public RequestSupplier build() {
      return new RequestSupplier(this);
    }
  }
}
