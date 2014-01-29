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
// Date: Jan 29, 2014
// ---------------------

package com.cleversafe.oom.operation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.statistic.Statistics;
import com.cleversafe.oom.statistic.StatisticsImpl;

public class HTTPOperationTest
{
   private HTTPOperation o;
   private Statistics stats;
   private URL url;

   @Before
   public void setBefore() throws MalformedURLException
   {
      this.stats = new StatisticsImpl(0, 5000);
      this.url = new URL("http://cleversafe.com/index.html");
      this.o = new HTTPOperation(OperationType.WRITE, this.url, HTTPMethod.PUT, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullURL()
   {
      new HTTPOperation(OperationType.WRITE, null, HTTPMethod.PUT, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullHTTPMethod()
   {
      new HTTPOperation(OperationType.WRITE, this.url, null, this.stats);
   }

   @Test
   public void testGetURL()
   {
      Assert.assertEquals(this.url.toString(), this.o.getURL().toString());
   }

   @Test(expected = NullPointerException.class)
   public void testSetURLNullURL()
   {
      this.o.setURL(null);
   }

   @Test
   public void testSetURL() throws MalformedURLException
   {
      final URL newURL = new URL("http://cleversafe.com/about.html");
      this.o.setURL(newURL);
      final URL url = this.o.getURL();
      Assert.assertEquals(newURL.toString(), url.toString());
   }

   @Test
   public void testGetMethod()
   {
      Assert.assertEquals(HTTPMethod.PUT, this.o.getMethod());
   }

   @Test(expected = NullPointerException.class)
   public void testSetMethodNullMethod()
   {
      this.o.setMethod(null);
   }

   @Test
   public void testSetMethod()
   {
      this.o.setMethod(HTTPMethod.GET);
      Assert.assertEquals(HTTPMethod.GET, this.o.getMethod());
   }

   @Test(expected = NullPointerException.class)
   public void testGetRequestHeaderNullKey()
   {
      this.o.getRequestHeader(null);
   }

   @Test
   public void testGetRequestHeaderNoMapping()
   {
      Assert.assertEquals(null, this.o.getRequestHeader("boguskey"));
   }

   @Test(expected = NullPointerException.class)
   public void testSetRequestHeaderNullKey()
   {
      this.o.setRequestHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testSetRequestHeaderNullValue()
   {
      this.o.setRequestHeader("key", null);
   }

   @Test
   public void testSetRequestHeader()
   {
      this.o.setRequestHeader("key", "value");
      Assert.assertEquals("value", this.o.getRequestHeader("key"));
   }

   @Test
   public void testRequestHeaderIteratorEmpty()
   {
      Assert.assertEquals(false, this.o.requestHeaderIterator().hasNext());
   }

   @Test
   public void testRequestHeaderIterator()
   {
      final HashMap<String, String> m = new HashMap<String, String>();
      for (int i = 0; i < 10; i++)
      {
         m.put("key" + i, "value" + i);
         this.o.setRequestHeader("key" + i, "value" + i);
      }
      final Iterator<Entry<String, String>> i = this.o.requestHeaderIterator();
      while (i.hasNext())
      {
         final Entry<String, String> e = i.next();
         Assert.assertEquals(e.getValue(), m.get(e.getKey()));
      }
   }

   @Test(expected = NullPointerException.class)
   public void testGetResponseHeaderNullKey()
   {
      this.o.getResponseHeader(null);
   }

   @Test
   public void testGetResponseHeaderNoMapping()
   {
      Assert.assertEquals(null, this.o.getResponseHeader("boguskey"));
   }

   @Test(expected = NullPointerException.class)
   public void testSetResponseHeaderNullKey()
   {
      this.o.setResponseHeader(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testSetResponseHeaderNullValue()
   {
      this.o.setResponseHeader("key", null);
   }

   @Test
   public void testSetResponseHeader()
   {
      this.o.setResponseHeader("key", "value");
      Assert.assertEquals("value", this.o.getResponseHeader("key"));
   }

   @Test
   public void testResponseHeaderIteratorEmpty()
   {
      Assert.assertEquals(false, this.o.responseHeaderIterator().hasNext());
   }

   @Test
   public void testResponseHeaderIterator()
   {
      final HashMap<String, String> m = new HashMap<String, String>();
      for (int i = 0; i < 10; i++)
      {
         m.put("key" + i, "value" + i);
         this.o.setResponseHeader("key" + i, "value" + i);
      }
      final Iterator<Entry<String, String>> i = this.o.responseHeaderIterator();
      while (i.hasNext())
      {
         final Entry<String, String> e = i.next();
         Assert.assertEquals(e.getValue(), m.get(e.getKey()));
      }
   }
}
