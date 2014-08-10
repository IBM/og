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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.util.Bodies;
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
   public void testNullMethod()
   {
      new RequestSupplier.Builder((Method) null, this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethodSupplier()
   {
      new RequestSupplier.Builder((Supplier<Method>) null, Suppliers.of(this.uri)).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUri()
   {
      new RequestSupplier.Builder(this.method, (URI) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUriSupplier()
   {
      new RequestSupplier.Builder(Suppliers.of(this.method), (Supplier<URI>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKey()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValue()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader("key", null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKeySupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader((Supplier<String>) null,
            Suppliers.of("value")).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValueSupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withHeader(Suppliers.of("key"),
            (Supplier<String>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullBody()
   {
      new RequestSupplier.Builder(this.method, this.uri).withBody((Body) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullBodySupplier()
   {
      new RequestSupplier.Builder(this.method, this.uri).withBody((Supplier<Body>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey()
   {
      new RequestSupplier.Builder(this.method, this.uri).withMetadata((Metadata) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey2()
   {
      new RequestSupplier.Builder(this.method, this.uri).withMetadata((String) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey3()
   {
      new RequestSupplier.Builder(this.method, this.uri)
            .withMetadata((Supplier<String>) null, Suppliers.of("value")).build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue()
   {
      new RequestSupplier.Builder(this.method, this.uri).withMetadata(Metadata.ABORTED, null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue2()
   {
      new RequestSupplier.Builder(this.method, this.uri).withMetadata("aborted", null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue3()
   {
      new RequestSupplier.Builder(this.method, this.uri)
            .withMetadata(Suppliers.of("aborted"), (Supplier<String>) null).build();
   }

   @Test
   public void testMethod()
   {
      final Request r = new RequestSupplier.Builder(Method.HEAD, this.uri).build().get();
      Assert.assertEquals(Method.HEAD, r.getMethod());
   }

   @Test
   public void testMethodSupplier()
   {
      final Request r =
            new RequestSupplier.Builder(Suppliers.of(Method.DELETE), Suppliers.of(this.uri)).build().get();
      Assert.assertEquals(Method.DELETE, r.getMethod());
   }

   @Test
   public void testUri() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final Request r = new RequestSupplier.Builder(this.method, aUri).build().get();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testUriSupplier() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final Request r =
            new RequestSupplier.Builder(Suppliers.of(this.method), Suppliers.of(aUri)).build().get();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testHeaders()
   {
      final RequestSupplier p = new RequestSupplier.Builder(this.method, this.uri)
            .withHeader("key2", "value2")
            .withHeader(Suppliers.of("key1"), Suppliers.of("value1"))
            .build();
      final Request r = p.get();
      final Iterator<Entry<String, String>> it = r.headers();
      // Skip Date header which is automatically added
      it.next();
      Assert.assertTrue(it.hasNext());
      Entry<String, String> e = it.next();
      Assert.assertEquals("key2", e.getKey());
      Assert.assertEquals("value2", e.getValue());

      Assert.assertTrue(it.hasNext());
      e = it.next();
      Assert.assertEquals("key1", e.getKey());
      Assert.assertEquals("value1", e.getValue());
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testNoBody()
   {
      final Request r = new RequestSupplier.Builder(this.method, this.uri).build().get();
      Assert.assertEquals(Data.NONE, r.getBody().getData());
      Assert.assertEquals(0, r.getBody().getSize());
   }

   @Test
   public void testBody()
   {
      final Request r = new RequestSupplier.Builder(this.method, this.uri)
            .withBody(Bodies.zeroes(12345))
            .build()
            .get();
      Assert.assertEquals(Data.ZEROES, r.getBody().getData());
      Assert.assertEquals(12345, r.getBody().getSize());
   }

   @Test
   public void testBodySupplier()
   {
      final Request r = new RequestSupplier.Builder(this.method, this.uri)
            .withBody(Suppliers.of(Bodies.zeroes(12345)))
            .build()
            .get();
      Assert.assertEquals(Data.ZEROES, r.getBody().getData());
      Assert.assertEquals(12345, r.getBody().getSize());
   }

   @Test
   public void testMetadata()
   {
      final Request r = new RequestSupplier.Builder(this.method, this.uri)
            .withMetadata("key3", "value3")
            .withMetadata(Metadata.ABORTED, "value2")
            .withMetadata(Suppliers.of("key1"), Suppliers.of("value1"))
            .build()
            .get();
      final Iterator<Entry<String, String>> it = r.metadata();

      Assert.assertTrue(it.hasNext());
      Entry<String, String> e = it.next();
      Assert.assertEquals("key3", e.getKey());
      Assert.assertEquals("value3", e.getValue());
      Assert.assertTrue(it.hasNext());
      e = it.next();
      Assert.assertEquals("ABORTED", e.getKey());
      Assert.assertEquals("value2", e.getValue());
      e = it.next();
      Assert.assertEquals("key1", e.getKey());
      Assert.assertEquals("value1", e.getValue());
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testHeaderModification()
   {
      final RequestSupplier.Builder b =
            new RequestSupplier.Builder(this.method, this.uri).withHeader("key1", "value1");
      final RequestSupplier rp = b.build();
      b.withHeader("key2", "value2");
      final Request r = rp.get();
      Assert.assertEquals("value1", r.getHeader("key1"));
      Assert.assertNull(r.getHeader("key2"));
   }

   @Test
   public void testMetadataModification()
   {
      final RequestSupplier.Builder b =
            new RequestSupplier.Builder(this.method, this.uri).withMetadata("key1", "value1");
      final RequestSupplier rp = b.build();
      b.withMetadata("key2", "value2");
      final Request r = rp.get();
      Assert.assertEquals("value1", r.getMetadata("key1"));
      Assert.assertNull(r.getMetadata("key2"));
   }
}
