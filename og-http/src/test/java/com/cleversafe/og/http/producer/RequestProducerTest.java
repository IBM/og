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
import com.cleversafe.og.util.Pair;
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
      RequestProducer.custom().withMethod(this.method).withUri(this.uri).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeId()
   {
      RequestProducer.custom().withId(-1).withMethod(this.method).withUri(this.uri).build();
   }

   public void testZeroId()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).build();
   }

   public void testPositiveId()
   {
      RequestProducer.custom().withId(1).withMethod(this.method).withUri(this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullId()
   {
      RequestProducer.custom().withId((Producer<Long>) null).withMethod(this.method).withUri(
            this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethod()
   {
      RequestProducer.custom().withId(0).withMethod((Method) null).withUri(this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullMethodProducer()
   {
      RequestProducer.custom().withId(0).withMethod((Producer<Method>) null).withUri(this.uri).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUri()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri((URI) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullUriProducer()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri((Producer<URI>) null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullObject()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withObject(null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKey()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withHeader(null,
            "value");
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValue()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withHeader(
            "key", null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullPair()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withHeader(
            (Pair<String, String>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullPairProducer()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withHeader(
            (Producer<Pair<String, String>>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntity()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withEntity(
            (Entity) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntityProducer()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withEntity(
            (Producer<Entity>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withMetadata(
            (Metadata) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey2()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withMetadata(
            (String) null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withMetadata(
            Metadata.ABORTED, null);
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue2()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withMetadata(
            "aborted", null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullUsername()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withUsername(
            (String) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullUsername2()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withUsername(
            (Producer<String>) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullPassword()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withPassword(
            (String) null);
   }

   @Test(expected = NullPointerException.class)
   public void testNullPassword2()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withPassword(
            (Producer<String>) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullUsernameNoPassword()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withUsername("u").build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullNoUsernamePassword()
   {
      RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withPassword("p").build();
   }

   @Test
   public void testId()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(5).withMethod(this.method).withUri(this.uri).build();
      final Request r = p.produce();
      Assert.assertEquals(5, r.getId());
   }

   @Test
   public void testMethod()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(Method.HEAD).withUri(this.uri).build();
      final Request r = p.produce();
      Assert.assertEquals(Method.HEAD, r.getMethod());
   }

   @Test
   public void testMethodProducer()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(Producers.of(Method.DELETE)).withUri(
                  this.uri).build();
      final Request r = p.produce();
      Assert.assertEquals(Method.DELETE, r.getMethod());
   }

   @Test
   public void testUri() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(aUri).build();
      final Request r = p.produce();
      Assert.assertEquals(aUri, r.getUri());
   }

   @Test
   public void testUriProducer() throws URISyntaxException
   {
      final URI aUri = new URI("http://10.1.1.1/container/object");
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(Producers.of(aUri)).build();
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
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withObject(
                  cp).build();

      Assert.assertEquals("one", cp.produce());
      final Request r = p.produce();
      Assert.assertEquals("one", r.getMetadata(Metadata.OBJECT_NAME));
      Assert.assertEquals("two", cp.produce());
   }

   @Test
   public void testHeaders()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withHeader(
                  "key3", "value3").withHeader(new Pair<String, String>("key2", "value2")).withHeader(
                  Producers.of(new Pair<String, String>("key1", "value1"))).build();
      final Request r = p.produce();
      final Iterator<Entry<String, String>> it = r.headers();
      // Skip Date header which is automatically added
      it.next();
      for (int i = 0; i < 3; i++)
      {
         Assert.assertTrue(it.hasNext());
         final Entry<String, String> e = it.next();
         Assert.assertEquals("key" + (3 - i), e.getKey());
         Assert.assertEquals("value" + (3 - i), e.getValue());
      }
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testNoEntity()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.NONE, r.getEntity().getType());
      Assert.assertEquals(0, r.getEntity().getSize());
   }

   @Test
   public void testEntity()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withEntity(
                  Entities.of(EntityType.ZEROES, 12345)).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testEntityProducer()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withEntity(
                  Producers.of(Entities.of(EntityType.ZEROES, 12345))).build();
      final Request r = p.produce();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testMetadata()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withMetadata(
                  "key2", "value2").withMetadata(Metadata.ABORTED, "value1").build();
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
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).build();
      final Request r = p.produce();
      Assert.assertNull(r.getMetadata(Metadata.USERNAME));
      Assert.assertNull(r.getMetadata(Metadata.PASSWORD));
   }

   @Test
   public void testCredentials()
   {
      final RequestProducer p =
            RequestProducer.custom().withId(0).withMethod(this.method).withUri(this.uri).withUsername(
                  "username").withPassword("password").build();
      final Request r = p.produce();
      Assert.assertEquals("username", r.getMetadata(Metadata.USERNAME));
      Assert.assertEquals("password", r.getMetadata(Metadata.PASSWORD));
   }
}
