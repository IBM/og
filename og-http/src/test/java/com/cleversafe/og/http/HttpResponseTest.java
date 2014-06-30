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

package com.cleversafe.og.http;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.util.Entities;

public class HttpResponseTest
{
   private int statusCode;

   @Before
   public void setBefore()
   {
      this.statusCode = 201;
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeRequestId()
   {
      HttpResponse.custom().withRequestId(-1).withStatusCode(this.statusCode).build();
   }

   @Test
   public void testZeroRequestId()
   {
      HttpResponse.custom().withRequestId(0).withStatusCode(this.statusCode).build();
   }

   @Test
   public void testPositiveRequestId()
   {
      HttpResponse.custom().withRequestId(1).withStatusCode(this.statusCode).build();
   }

   @Test
   public void testNoRequestId()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoStatusCode()
   {
      HttpResponse.custom().build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeStatusCode()
   {
      HttpResponse.custom().withStatusCode(-1).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroStatusCode()
   {
      HttpResponse.custom().withStatusCode(0).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSmallStatusCode()
   {
      HttpResponse.custom().withStatusCode(99).build();
   }

   @Test
   public void testMediumStatusCode()
   {
      HttpResponse.custom().withStatusCode(100).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeStatusCode()
   {
      HttpResponse.custom().withStatusCode(600).build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullKey()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withHeader(null, "value").build();
   }

   @Test(expected = NullPointerException.class)
   public void testHeaderNullValue()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withHeader("key", null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullEntity()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withEntity(null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withMetadata((Metadata) null, "value").build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullKey2()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withMetadata((String) null, "value").build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withMetadata(Metadata.ABORTED, null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testMetadataNullValue2()
   {
      HttpResponse.custom().withStatusCode(this.statusCode).withMetadata("key", null).build();
   }

   @Test
   public void testDefaultRequestId()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      Assert.assertEquals(0, r.getRequestId());
   }

   @Test
   public void testRequestId()
   {
      final HttpResponse r =
            HttpResponse.custom().withRequestId(5).withStatusCode(this.statusCode).build();
      Assert.assertEquals(5, r.getRequestId());
   }

   @Test
   public void testStatusCode()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(404).build();
      Assert.assertEquals(404, r.getStatusCode());
   }

   @Test
   public void testMissingHeader()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      Assert.assertNull(r.getHeader("key"));
   }

   @Test
   public void testHeader()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withHeader("key", "value").build();
      Assert.assertEquals("value", r.getHeader("key"));
   }

   @Test
   public void testNoHeaders()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      final Iterator<Entry<String, String>> it = r.headers();
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testHeaders()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withHeader("key", "value").build();
      final Iterator<Entry<String, String>> it = r.headers();
      Assert.assertTrue(it.hasNext());
      final Entry<String, String> e = it.next();
      Assert.assertEquals("key", e.getKey());
      Assert.assertEquals("value", e.getValue());
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testHeaders2()
   {
      final HttpResponse.Builder b = HttpResponse.custom().withStatusCode(this.statusCode);
      for (int i = 0; i < 10; i++)
      {
         // (100 - i) exposes sorted vs insertion order
         b.withHeader("key" + (100 - i), "value" + i);
      }
      final HttpResponse r = b.build();
      final Iterator<Entry<String, String>> it = r.headers();
      for (int i = 0; i < 10; i++)
      {
         Assert.assertTrue(it.hasNext());
         final Entry<String, String> e = it.next();
         Assert.assertEquals("key" + (100 - i), e.getKey());
         Assert.assertEquals("value" + i, e.getValue());
      }
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testDefaultEntity()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      Assert.assertEquals(EntityType.NONE, r.getEntity().getType());
      Assert.assertEquals(0, r.getEntity().getSize());
   }

   @Test
   public void testEntity()
   {
      final Entity e = Entities.of(EntityType.ZEROES, 12345);
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withEntity(e).build();
      Assert.assertEquals(EntityType.ZEROES, r.getEntity().getType());
      Assert.assertEquals(12345, r.getEntity().getSize());
   }

   @Test
   public void testMissingMetadata()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      Assert.assertNull(r.getMetadata(Metadata.ABORTED));
   }

   @Test
   public void testMissingMetadata2()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      Assert.assertNull(r.getMetadata("aborted"));
   }

   @Test
   public void testMetadataEntry()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withMetadata(Metadata.ABORTED,
                  "1").build();
      Assert.assertEquals("1", r.getMetadata(Metadata.ABORTED));
   }

   @Test
   public void testMetadataEntry2()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withMetadata("key", "value").build();
      Assert.assertEquals("value", r.getMetadata("key"));
   }

   @Test
   public void testMetadata()
   {
      final HttpResponse r = HttpResponse.custom().withStatusCode(this.statusCode).build();
      final Iterator<Entry<String, String>> it = r.metadata();
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testMetadata2()
   {
      final HttpResponse r =
            HttpResponse.custom().withStatusCode(this.statusCode).withMetadata("key", "value").build();
      final Iterator<Entry<String, String>> it = r.metadata();
      Assert.assertTrue(it.hasNext());
      final Entry<String, String> e = it.next();
      Assert.assertEquals("key", e.getKey());
      Assert.assertEquals("value", e.getValue());
      Assert.assertFalse(it.hasNext());
   }

   @Test
   public void testMetadata3()
   {
      final HttpResponse.Builder b = HttpResponse.custom().withStatusCode(this.statusCode);
      for (int i = 0; i < 10; i++)
      {
         b.withMetadata("key" + i, "value" + i);
      }
      b.withMetadata(Metadata.ABORTED, "1");
      final HttpResponse r = b.build();
      final Iterator<Entry<String, String>> it = r.metadata();
      Assert.assertTrue(it.hasNext());
      for (int i = 0; i < 10; i++)
      {
         final Entry<String, String> e = it.next();
         Assert.assertEquals("key" + i, e.getKey());
         Assert.assertEquals("value" + i, e.getValue());
      }
      Assert.assertTrue(it.hasNext());
      final Entry<String, String> e = it.next();
      Assert.assertEquals("ABORTED", e.getKey());
      Assert.assertEquals("1", e.getValue());
      Assert.assertFalse(it.hasNext());
   }
}
