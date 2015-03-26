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
import java.util.Map;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.HttpRequest;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A supplier of requests
 * 
 * @since 1.0
 */
public class RequestSupplier implements Supplier<Request> {
  private final Method method;
  private final Supplier<URI> uri;
  private final Map<Supplier<String>, Supplier<String>> headers;
  private final Supplier<Body> body;

  private RequestSupplier(final Builder builder) {
    this.method = checkNotNull(builder.method);
    this.uri = checkNotNull(builder.uri);
    this.headers = ImmutableMap.copyOf(builder.headers);
    this.body = builder.body;
  }

  @Override
  public Request get() {
    final HttpRequest.Builder context = new HttpRequest.Builder(this.method, this.uri.get());

    for (final Map.Entry<Supplier<String>, Supplier<String>> header : this.headers.entrySet()) {
      context.withHeader(header.getKey().get(), header.getValue().get());
    }

    if (this.body != null)
      context.withBody(this.body.get());

    return context.build();
  }

  @Override
  public String toString() {
    return String.format("RequestSupplier [%n" + "method=%s,%n" + "uri=%s,%n" + "headers=%s,%n"
        + "body=%s%n" + "]", this.method, this.uri, this.headers, this.body);
  }

  /**
   * A request supplier builder
   */
  public static class Builder {
    private final Method method;
    private final Supplier<URI> uri;
    private final Map<Supplier<String>, Supplier<String>> headers;
    private Supplier<Body> body;

    /**
     * Constructs a builder instance using the provided method and uri suppliers
     * 
     * @param method a request method supplier
     * @param uri a request uri supplier
     */
    public Builder(final Method method, final Supplier<URI> uri) {
      this.method = method;
      this.uri = uri;
      this.headers = Maps.newLinkedHashMap();
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
