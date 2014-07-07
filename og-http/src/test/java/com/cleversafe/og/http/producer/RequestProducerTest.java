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

package com.cleversafe.og.http.producer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import com.cleversafe.og.util.producer.CachingProducer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;

public class RequestProducerTest
{
   private Method method;
   private URI uri;

   @Before
   public void setBefore() throws URISyntaxException
   {
      this.method = Method.PUT;
      this.uri = new URI("http://192.168.8.23/container/object");
   }

   @Test(expected = NullPointerException.class)
   public void testNoId()
   {
      new RequestProducer.Builder(this.method, this.uri).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeId()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(-1).build();
   }

   public void testZeroId()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).build();
   }

   public void testPositiveId()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(1).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullIdProducer()
   {
      new RequestProducer.Builder(this.method, this.uri).withId((Producer<Long>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethod()
   {
      new RequestProducer.Builder((Method) null, this.uri).withId(0).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethodProducer()
   {
      new RequestProducer.Builder((Producer<Method>) null, Producers.of(this.uri)).withId(0).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUri()
   {
      new RequestProducer.Builder(this.method, (URI) null).withId(0).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUriProducer()
   {
      new RequestProducer.Builder(Producers.of(this.method), (Producer<URI>) null).withId(0).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullObject()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withObject(null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKey()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValue()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withHeader("key", null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKeyProducer()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withHeader(
            (Producer<String>) null, Producers.of("value"));
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValueProducer()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withHeader(Producers.of("key"),
            (Producer<String>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntity()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withEntity((Entity) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntityProducer()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withEntity(
            (Producer<Entity>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withMetadata((Metadata) null,
            "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey2()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withMetadata((String) null,
            "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue()
   {
      new RequestProducer.Builder(this.method, this.uri).withMetadata(Metadata.ABORTED, null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue2()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withMetadata("aborted", null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullUsername()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withCredentials(null, "password");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullUsername2()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withCredentials(
            (Producer<String>) null, Producers.of("password")).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullPassword()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withCredentials("username",
            (String) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullPassword2()
   {
      new RequestProducer.Builder(this.method, this.uri).withId(0).withCredentials(
            Producers.of("username"), (Producer<String>) null).build();
   }

   @Test
   public void testId()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(5).build();
      final Request r = p.produce();
      Assert.assertEquals(5, r.getId());
   }

   @Test
   public void testIdProducer()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(Producers.of(5L)).build();
      final Request r = p.produce();
      Assert.assertEquals(5, r.getId());
   }

   @Test
   public void testMethod()
   {
      final RequestProducer p =
            new RequestProducer.Builder(Method.HEAD, this.uri).withId(0).build();
      final Request r = p.produce();
      Assert.assertEquals(Method.HEAD, r.getMethod());
   }

   @Test
   public void testMethodProducer()
   {
      final RequestProducer p =
            new RequestProducer.Builder(Producers.of(Method.DELETE), Producers.of(this.uri)).withId(
                  0).build();
      final Request r = p.produce();
      Assert.assertEquals(Method.DELETE, r.getMethod());
   }

   @Test
   public void testUri() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final RequestProducer p = new RequestProducer.Builder(this.method, aUri).withId(0).build();
      final Request r = p.produce();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testUriProducer() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final RequestProducer p =
            new RequestProducer.Builder(Producers.of(this.method), Producers.of(aUri)).withId(0).build();
      final Request r = p.produce();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testObject()
   {
      final List<String> list = new ArrayList<String>();
      list.add("one");
      list.add("two");
      list.add("three");
      final CachingProducer<String> cp = new CachingProducer<String>(Producers.cycle(list));
      final Producer<Request> p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withObject(cp).build();

      Assert.assertEquals("one", cp.produce());
      final Request r = p.produce();
      Assert.assertEquals("one", r.getMetadata(Metadata.OBJECT_NAME));
      Assert.assertEquals("two", cp.produce());
   }

   @Test
   public void testHeaders()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withHeader("key2",
                  "value2").withHeader(Producers.of("key1"), Producers.of("value1")).build();
      final Request r = p.produce();
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
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.NONE, r.getEntity().getType());
      Assert.assertEquals(0, r.getEntity().getSize());
   }

   @Test
   public void testEntity()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withEntity(
                  Entities.of(EntityType.ZEROES, 12345)).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testEntityProducer()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withEntity(
                  Producers.of(Entities.of(EntityType.ZEROES, 12345))).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testMetadata()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withMetadata("key2",
                  "value2").withMetadata(Metadata.ABORTED, "value1").build();
      final Request r = p.produce();
      final Iterator<Entry<String, String>> it = r.metadata();

      Assert.assertTrue(it.hasNext());
      Entry<String, String> e = it.next();
      Assert.assertEquals("key2", e.getKey());
      Assert.assertEquals("value2", e.getValue());
      Assert.assertTrue(it.hasNext());
      e = it.next();
      Assert.assertEquals("ABORTED", e.getKey());
      Assert.assertEquals("value1", e.getValue());
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testNoCredentials()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).build();
      final Request r = p.produce();
      Assert.assertNull(r.getMetadata(Metadata.USERNAME));
      Assert.assertNull(r.getMetadata(Metadata.PASSWORD));
   }

   @Test
   public void testCredentials()
   {
      final RequestProducer p =
            new RequestProducer.Builder(this.method, this.uri).withId(0).withCredentials(
                  "username", "password").build();
      final Request r = p.produce();
      Assert.assertEquals("username", r.getMetadata(Metadata.USERNAME));
      Assert.assertEquals("password", r.getMetadata(Metadata.PASSWORD));
   }
}
