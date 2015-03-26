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
import java.util.List;
import java.util.Map;

import com.cleversafe.og.http.Scheme;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A supplier of uris
 * 
 * @since 1.0
 */
public class UriSupplier implements Supplier<URI> {
  private final Scheme scheme;
  private final Supplier<String> host;
  private final Integer port;
  private final List<Supplier<String>> path;
  private final Map<String, String> queryParameters;
  private final boolean trailingSlash;
  private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");

  private UriSupplier(final Builder builder) {
    this.scheme = checkNotNull(builder.scheme);
    this.host = checkNotNull(builder.host);
    this.port = builder.port;
    this.path = ImmutableList.copyOf(builder.path);
    this.queryParameters = ImmutableMap.copyOf(builder.queryParameters);
    this.trailingSlash = builder.trailingSlash;
  }

  @Override
  public URI get() {
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
    for (final Supplier<String> part : this.path) {
      s.append("/").append(part.get());
    }
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
    return String.format("UriSupplier [%n" + "scheme=%s,%n" + "host=%s,%n" + "port=%s,%n"
        + "path=%s,%n" + "queryParameters=%s,%n" + "trailingSlash=%s%n" + "]", this.scheme,
        this.host, this.port, this.path, this.queryParameters, this.trailingSlash);
  }

  /**
   * A uri supplier builder
   */
  public static class Builder {
    private Scheme scheme;
    private final Supplier<String> host;
    private Integer port;
    private final List<Supplier<String>> path;
    private final Map<String, String> queryParameters;
    private boolean trailingSlash;

    /**
     * Constructs a builder instance using the provided host and path
     * 
     * @param host the host name or ip address
     * @param path the uri resource path
     */
    public Builder(final String host, final List<Supplier<String>> path) {
      this(Suppliers.of(host), path);
    }

    /**
     * Constructs a builder instance using the provided host and path suppliers
     * 
     * @param host the host name or ip address
     * @param path the uri resource path
     */
    public Builder(final Supplier<String> host, final List<Supplier<String>> path) {
      this.scheme = Scheme.HTTP;
      this.host = host;
      this.path = path;
      this.queryParameters = Maps.newLinkedHashMap();
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
     * Constructs a uri supplier instance
     * 
     * @return a uri supplier instance
     * @throws NullPointerException if path contains any null elements, or queryParameters contains
     *         any null keys or values
     */
    public UriSupplier build() {
      return new UriSupplier(this);
    }
  }
}
