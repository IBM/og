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
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class RequestSupplierTest {
  private Method method;
  private Supplier<String> host;
  private Supplier<String> container;
  private List<Supplier<String>> path;

  @Before
  public void before() throws URISyntaxException {
    this.method = Method.PUT;
    this.host = Suppliers.of("192.168.8.1");
    this.container = Suppliers.of("container");
    this.path = Lists.newArrayList();
    this.path.add(this.container);
  }

  // final Method method, final Supplier<URI> uri, final Supplier<String> host,
  // final List<Supplier<String>> path

  @Test(expected = NullPointerException.class)
  public void nullMethod() {
    new RequestSupplier.Builder(null, this.host, this.path).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullHostSupplier() {
    new RequestSupplier.Builder(this.method, null, this.path).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPathSupplier() {
    new RequestSupplier.Builder(this.method, this.host, null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullScheme() {
    new RequestSupplier.Builder(this.method, this.host, this.path).withScheme((Scheme) null)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativePort() {
    new RequestSupplier.Builder(this.method, this.host, this.path).onPort(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroPort() {
    new RequestSupplier.Builder(this.method, this.host, this.path).onPort(0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void largePort() {
    new RequestSupplier.Builder(this.method, this.host, this.path).onPort(65536).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPath() {
    new RequestSupplier.Builder(this.method, this.host, (List<Supplier<String>>) null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidPath() {
    new RequestSupplier.Builder(this.method, this.host,
        ImmutableList.of(Suppliers.of("containe\r"))).build().get();
  }

  @Test(expected = NullPointerException.class)
  public void queryParametersNullKey() {
    new RequestSupplier.Builder(this.method, this.host, this.path)
        .withQueryParameter(null, "value").build();
  }

  @Test(expected = NullPointerException.class)
  public void queryParametersNullValue() {
    new RequestSupplier.Builder(this.method, this.host, this.path).withQueryParameter("key", null)
        .build();
  }

  @Test
  public void uri() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path).build().get().getUri();

    assertThat(Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)), is(Scheme.HTTP));
    assertThat(uri.getHost(), is(this.host.get()));
    assertThat(uri.getPort(), is(-1));
    assertThat(uri.getPath(), is("/" + this.container));
    assertThat(uri.getQuery(), nullValue());
  }

  @Test
  public void scheme() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path).withScheme(Scheme.HTTPS)
            .build().get().getUri();
    assertThat(Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)), is(Scheme.HTTPS));
  }

  @Test
  public void host() {
    final URI uri =
        new RequestSupplier.Builder(this.method, Suppliers.of("10.1.1.1"), this.path).build().get()
            .getUri();
    assertThat(uri.getHost(), is("10.1.1.1"));
  }

  @Test
  public void port() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path).onPort(80).build().get()
            .getUri();
    assertThat(uri.getPort(), is(80));
  }

  @Test
  public void path() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, ImmutableList.of(
            Suppliers.of("container"), Suppliers.of("object"))).build().get().getUri();
    assertThat(uri.getPath(), is("/container/object"));
  }

  @Test
  public void pathModification() {
    final List<Supplier<String>> path = Lists.newArrayList();
    path.add(Suppliers.of("container"));
    final Supplier<Request> r =
        new RequestSupplier.Builder(this.method, this.host, this.path).build();
    path.add(Suppliers.of("object"));
    final URI uri = r.get().getUri();
    assertThat(uri.getPath(), is("/container"));
  }

  @Test
  public void trailingSlash() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path).withTrailingSlash().build()
            .get().getUri();
    assertThat(uri.getPath(), is("/container/"));
  }

  @Test
  public void queryParameters() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path)
            .withQueryParameter("key", "value").build().get().getUri();
    assertThat(uri.getQuery(), is("key=value"));
  }

  @Test
  public void queryParameters2() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path)
            .withQueryParameter("key", "value").withQueryParameter("key2", "value2").build().get()
            .getUri();
    assertThat(uri.getQuery(), is("key=value&key2=value2"));
  }

  @Test
  public void queryParameters3() {
    final URI uri =
        new RequestSupplier.Builder(this.method, this.host, this.path)
            .withQueryParameter("key2", "value2").withQueryParameter("key1", "value1").build()
            .get().getUri();
    assertThat(uri.getQuery(), is("key2=value2&key1=value1"));
  }

  @Test
  public void queryParametersModification() {
    final RequestSupplier.Builder b =
        new RequestSupplier.Builder(this.method, this.host, this.path).withQueryParameter("key",
            "value");
    final Supplier<Request> r = b.build();
    b.withQueryParameter("key2", "value2");
    final URI uri = r.get().getUri();

    assertThat(uri.getQuery(), is("key=value"));
  }

  @Test(expected = NullPointerException.class)
  public void headerNullKey() {
    new RequestSupplier.Builder(this.method, this.host, this.path).withHeader(null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void headerNullValue() {
    new RequestSupplier.Builder(this.method, this.host, this.path).withHeader("key", null);
  }

  @Test(expected = NullPointerException.class)
  public void nullBodySupplier() {
    new RequestSupplier.Builder(this.method, this.host, this.path).withBody((Supplier<Body>) null);
  }

  @Test
  public void method() {
    final Request request =
        new RequestSupplier.Builder(Method.HEAD, this.host, this.path).build().get();
    assertThat(request.getMethod(), is(Method.HEAD));
  }

  @Test
  public void headers() {
    final Request request =
        new RequestSupplier.Builder(this.method, this.host, this.path).withHeader("key2", "value2")
            .withHeader("key1", "value1").build().get();
    final Iterator<Entry<String, String>> it = request.headers().entrySet().iterator();
    // Skip Date header which is automatically added
    it.next();
    assertThat(it.hasNext(), is(true));

    Entry<String, String> e = it.next();
    assertThat(e.getKey(), is("key2"));
    assertThat(e.getValue(), is("value2"));
    assertThat(it.hasNext(), is(true));

    e = it.next();
    assertThat(e.getKey(), is("key1"));
    assertThat(e.getValue(), is("value1"));
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void headerModification() {
    final RequestSupplier.Builder b =
        new RequestSupplier.Builder(this.method, this.host, this.path).withHeader("key1", "value1");
    final RequestSupplier s = b.build();
    b.withHeader("key2", "value2");
    final Request request = s.get();

    assertThat(request.headers().get("key1"), is("value1"));
    assertThat(request.headers().get("key2"), nullValue());
  }

  @Test
  public void noBody() {
    final Request request =
        new RequestSupplier.Builder(this.method, this.host, this.path).build().get();
    assertThat(request.getBody().getData(), is(Data.NONE));
    assertThat(request.getBody().getSize(), is(0L));
  }

  @Test
  public void body() {
    final Request request =
        new RequestSupplier.Builder(this.method, this.host, this.path)
            .withBody(Suppliers.of(Bodies.zeroes(12345))).build().get();
    assertThat(request.getBody().getData(), is(Data.ZEROES));
    assertThat(request.getBody().getSize(), is(12345L));
  }

  @Test
  public void bodySupplier() {
    final Request request =
        new RequestSupplier.Builder(this.method, this.host, this.path)
            .withBody(Suppliers.of(Bodies.zeroes(12345))).build().get();
    assertThat(request.getBody().getData(), is(Data.ZEROES));
    assertThat(request.getBody().getSize(), is(12345L));
  }
}
