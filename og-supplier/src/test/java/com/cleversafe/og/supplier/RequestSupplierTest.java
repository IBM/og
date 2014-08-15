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
// Date: Jun 30, 2014
// ---------------------

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.google.common.base.Supplier;

public class RequestSupplierTest
{
   private Method method;
   private URI uri;

   @Before
   public void before() throws URISyntaxException
   {
      this.method = Method.PUT;
      this.uri = new URI("http://192.168.8.23/container/object");
   }

   @Test(expected = NullPointerException.class)
   public void nullMethod()
   {
      new RequestSupplier.Builder((Method) null, this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void nullMethodSupplier()
   {
      new RequestSupplier.Builder((Supplier<Method>) null, Suppliers.of(this.uri)).build();
   }

   @Test(expected = NullPointerException.class)
   public void nullUri()
   {
      new RequestSupplier.Builder(this.method, (URI) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void nullUriSupplier()
   {
      new RequestSupplier.Builder(Suppliers.of(this.method), (Supplier<URI>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void headerNullKey()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void headerNullKeySupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader((Supplier<String>) null,
            Suppliers.of("value")).build();
   }

   @Test(expected = NullPointerException.class)
   public void headerNullValue()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader("key", null);
   }

   @Test(expected = NullPointerException.class)
   public void headerNullValueSupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader(Suppliers.of("key"),
            (Supplier<String>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void nullBody()
   {
      new RequestSupplier.Builder(this.method, this.uri).withBody((Body) null);
   }

   @Test(expected = NullPointerException.class)
   public void nullBodySupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withBody((Supplier<Body>) null);
   }

   @Test
   public void method()
   {
      final Request request = new RequestSupplier.Builder(Method.HEAD, this.uri).build().get();
      assertThat(request.getMethod(), is(Method.HEAD));
   }

   @Test
   public void methodSupplier()
   {
      final Request request =
            new RequestSupplier.Builder(Suppliers.of(Method.DELETE), Suppliers.of(this.uri)).build().get();
      assertThat(request.getMethod(), is(Method.DELETE));
   }

   @Test
   public void uri()
   {
      final Request request = new RequestSupplier.Builder(this.method, this.uri).build().get();
      assertThat(request.getUri(), is(this.uri));
   }

   @Test
   public void uriSupplier()
   {
      final Request request =
            new RequestSupplier.Builder(Suppliers.of(this.method), Suppliers.of(this.uri)).build().get();
      assertThat(request.getUri(), is(this.uri));
   }

   @Test
   public void headers()
   {
      final Request request = new RequestSupplier.Builder(this.method, this.uri)
            .withHeader("key2", "value2")
            .withHeader(Suppliers.of("key1"), Suppliers.of("value1"))
            .build()
            .get();
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
   public void headerModification()
   {
      final RequestSupplier.Builder b =
            new RequestSupplier.Builder(this.method, this.uri).withHeader("key1", "value1");
      final RequestSupplier s = b.build();
      b.withHeader("key2", "value2");
      final Request request = s.get();

      assertThat(request.headers().get("key1"), is("value1"));
      assertThat(request.headers().get("key2"), nullValue());
   }

   @Test
   public void noBody()
   {
      final Request request = new RequestSupplier.Builder(this.method, this.uri).build().get();
      assertThat(request.getBody().getData(), is(Data.NONE));
      assertThat(request.getBody().getSize(), is(0L));
   }

   @Test
   public void body()
   {
      final Request request = new RequestSupplier.Builder(this.method, this.uri)
            .withBody(Bodies.zeroes(12345))
            .build()
            .get();
      assertThat(request.getBody().getData(), is(Data.ZEROES));
      assertThat(request.getBody().getSize(), is(12345L));
   }

   @Test
   public void bodySupplier()
   {
      final Request request = new RequestSupplier.Builder(this.method, this.uri)
            .withBody(Suppliers.of(Bodies.zeroes(12345)))
            .build()
            .get();
      assertThat(request.getBody().getData(), is(Data.ZEROES));
      assertThat(request.getBody().getSize(), is(12345L));
   }
}
