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

package com.cleversafe.og.producer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.util.Entities;
import com.google.common.base.Supplier;

public class RequestProducerTest
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
      new RequestProducer.Builder((Method) null, this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethodSupplier()
   {
      new RequestProducer.Builder((Supplier<Method>) null, Producers.of(this.uri)).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUri()
   {
      new RequestProducer.Builder(this.method, (URI) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUriSupplier()
   {
      new RequestProducer.Builder(Producers.of(this.method), (Supplier<URI>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKey()
   {
      new RequestProducer.Builder(this.method, this.uri).withHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValue()
   {
      new RequestProducer.Builder(this.method, this.uri).withHeader("key", null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKeySupplier()
   {
      new RequestProducer.Builder(this.method, this.uri).withHeader((Supplier<String>) null,
            Producers.of("value")).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValueSupplier()
   {
      new RequestProducer.Builder(this.method, this.uri).withHeader(Producers.of("key"),
            (Supplier<String>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntity()
   {
      new RequestProducer.Builder(this.method, this.uri).withEntity((Entity) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntitySupplier()
   {
      new RequestProducer.Builder(this.method, this.uri).withEntity((Supplier<Entity>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey()
   {
      new RequestProducer.Builder(this.method, this.uri).withMetadata((Metadata) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey2()
   {
      new RequestProducer.Builder(this.method, this.uri).withMetadata((String) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey3()
   {
      new RequestProducer.Builder(this.method, this.uri)
            .withMetadata((Supplier<String>) null, Producers.of("value")).build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue()
   {
      new RequestProducer.Builder(this.method, this.uri).withMetadata(Metadata.ABORTED, null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue2()
   {
      new RequestProducer.Builder(this.method, this.uri).withMetadata("aborted", null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue3()
   {
      new RequestProducer.Builder(this.method, this.uri)
            .withMetadata(Producers.of("aborted"), (Supplier<String>) null).build();
   }

   @Test
   public void testMethod()
   {
      final Request r = new RequestProducer.Builder(Method.HEAD, this.uri).build().get();
      Assert.assertEquals(Method.HEAD, r.getMethod());
   }

   @Test
   public void testMethodSupplier()
   {
      final Request r =
            new RequestProducer.Builder(Producers.of(Method.DELETE), Producers.of(this.uri)).build().get();
      Assert.assertEquals(Method.DELETE, r.getMethod());
   }

   @Test
   public void testUri() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final Request r = new RequestProducer.Builder(this.method, aUri).build().get();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testUriSupplier() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final Request r =
            new RequestProducer.Builder(Producers.of(this.method), Producers.of(aUri)).build().get();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testHeaders()
   {
      final RequestProducer p = new RequestProducer.Builder(this.method, this.uri)
            .withHeader("key2", "value2")
            .withHeader(Producers.of("key1"), Producers.of("value1"))
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
   public void testNoEntity()
   {
      final Request r = new RequestProducer.Builder(this.method, this.uri).build().get();
      Assert.assertEquals(EntityType.NONE, r.getEntity().getType());
      Assert.assertEquals(0, r.getEntity().getSize());
   }

   @Test
   public void testEntity()
   {
      final Request r = new RequestProducer.Builder(this.method, this.uri)
            .withEntity(Entities.zeroes(12345))
            .build()
            .get();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testEntitySupplier()
   {
      final Request r = new RequestProducer.Builder(this.method, this.uri)
            .withEntity(Producers.of(Entities.zeroes(12345)))
            .build()
            .get();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testMetadata()
   {
      final Request r = new RequestProducer.Builder(this.method, this.uri)
            .withMetadata("key3", "value3")
            .withMetadata(Metadata.ABORTED, "value2")
            .withMetadata(Producers.of("key1"), Producers.of("value1"))
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
      final RequestProducer.Builder b =
            new RequestProducer.Builder(this.method, this.uri).withHeader("key1", "value1");
      final RequestProducer rp = b.build();
      b.withHeader("key2", "value2");
      final Request r = rp.get();
      Assert.assertEquals("value1", r.getHeader("key1"));
      Assert.assertNull(r.getHeader("key2"));
   }

   @Test
   public void testMetadataModification()
   {
      final RequestProducer.Builder b =
            new RequestProducer.Builder(this.method, this.uri).withMetadata("key1", "value1");
      final RequestProducer rp = b.build();
      b.withMetadata("key2", "value2");
      final Request r = rp.get();
      Assert.assertEquals("value1", r.getMetadata("key1"));
      Assert.assertNull(r.getMetadata("key2"));
   }
}
