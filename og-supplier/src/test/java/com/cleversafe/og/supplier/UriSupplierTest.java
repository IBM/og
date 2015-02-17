/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.http.Scheme;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UriSupplierTest {
  private String host;
  private String container;
  private List<Supplier<String>> path;

  @Before
  public void before() {
    this.host = "192.168.8.1";
    this.container = "container";
    this.path = Lists.newArrayList();
    this.path.add(Suppliers.of(this.container));
  }

  @Test(expected = NullPointerException.class)
  public void nullScheme() {
    new UriSupplier.Builder(this.host, this.path).withScheme((Scheme) null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullSchemeSupplier() {
    new UriSupplier.Builder(this.host, this.path).withScheme((Supplier<Scheme>) null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullHost() {
    new UriSupplier.Builder((String) null, this.path).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullHostSupplier() {
    new UriSupplier.Builder((Supplier<String>) null, this.path).build();
  }

  @Test
  public void nullPort() {
    // can set port to null, it gets ignored when assembling url in get
    new UriSupplier.Builder(this.host, this.path).onPort((Supplier<Integer>) null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativePort() {
    new UriSupplier.Builder(this.host, this.path).onPort(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroPort() {
    new UriSupplier.Builder(this.host, this.path).onPort(0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void largePort() {
    new UriSupplier.Builder(this.host, this.path).onPort(65536).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPath() {
    new UriSupplier.Builder(this.host, (List<Supplier<String>>) null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidPath() {
    new UriSupplier.Builder(this.host, ImmutableList.of(Suppliers.of("containe\r"))).build().get();
  }

  @Test(expected = NullPointerException.class)
  public void queryParametersNullKey() {
    new UriSupplier.Builder(this.host, this.path).withQueryParameter(null, "value").build();
  }

  @Test(expected = NullPointerException.class)
  public void queryParametersNullValue() {
    new UriSupplier.Builder(this.host, this.path).withQueryParameter("key", null).build();
  }

  @Test
  public void uriSupplier() {
    final URI uri = new UriSupplier.Builder(this.host, this.path).build().get();

    assertThat(Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)), is(Scheme.HTTP));
    assertThat(uri.getHost(), is(this.host));
    assertThat(uri.getPort(), is(-1));
    assertThat(uri.getPath(), is("/" + this.container));
    assertThat(uri.getQuery(), nullValue());
  }

  @Test
  public void scheme() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).withScheme(Scheme.HTTPS).build().get();
    assertThat(Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)), is(Scheme.HTTPS));
  }

  @Test
  public void schemeSupplier() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).withScheme(Suppliers.of(Scheme.HTTPS))
            .build().get();
    assertThat(Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)), is(Scheme.HTTPS));
  }

  @Test
  public void host() {
    final URI uri = new UriSupplier.Builder("10.1.1.1", this.path).build().get();
    assertThat(uri.getHost(), is("10.1.1.1"));
  }

  @Test
  public void port() {
    final URI uri = new UriSupplier.Builder(this.host, this.path).onPort(80).build().get();
    assertThat(uri.getPort(), is(80));
  }

  @Test
  public void portSupplier() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).onPort(Suppliers.of(8080)).build().get();
    assertThat(uri.getPort(), is(8080));
  }

  @Test
  public void path() {
    final URI uri =
        new UriSupplier.Builder(this.host, ImmutableList.of(Suppliers.of("container"),
            Suppliers.of("object"))).build().get();
    assertThat(uri.getPath(), is("/container/object"));
  }

  @Test
  public void pathModification() {
    final List<Supplier<String>> path = Lists.newArrayList();
    path.add(Suppliers.of("container"));
    final Supplier<URI> s = new UriSupplier.Builder(this.host, this.path).build();
    path.add(Suppliers.of("object"));
    final URI uri = s.get();
    assertThat(uri.getPath(), is("/container"));
  }

  @Test
  public void trailingSlash() {
    final URI uri = new UriSupplier.Builder(this.host, this.path).withTrailingSlash().build().get();
    assertThat(uri.getPath(), is("/container/"));
  }

  @Test
  public void queryParameters() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).withQueryParameter("key", "value").build()
            .get();
    assertThat(uri.getQuery(), is("key=value"));
  }

  @Test
  public void queryParameters2() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).withQueryParameter("key", "value")
            .withQueryParameter("key2", "value2").build().get();
    assertThat(uri.getQuery(), is("key=value&key2=value2"));
  }

  @Test
  public void queryParameters3() {
    final URI uri =
        new UriSupplier.Builder(this.host, this.path).withQueryParameter("key2", "value2")
            .withQueryParameter("key1", "value1").build().get();
    assertThat(uri.getQuery(), is("key2=value2&key1=value1"));
  }

  @Test
  public void queryParametersModification() {
    final UriSupplier.Builder b =
        new UriSupplier.Builder(this.host, this.path).withQueryParameter("key", "value");
    final Supplier<URI> s = b.build();
    b.withQueryParameter("key2", "value2");
    final URI uri = s.get();

    assertThat(uri.getQuery(), is("key=value"));
  }
}
