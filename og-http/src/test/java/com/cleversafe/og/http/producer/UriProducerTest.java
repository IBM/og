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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.http.producer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.ProducerException;
import com.cleversafe.og.util.producer.Producers;

public class UriProducerTest
{
   private String host;
   private String container;
   private List<Producer<String>> path;

   @Before
   public void setBefore()
   {
      this.host = "192.168.8.1";
      this.container = "container";
      this.path = new ArrayList<Producer<String>>();
      this.path.add(Producers.of(this.container));
   }

   @Test(expected = NullPointerException.class)
   public void testNullScheme()
   {
      UriProducer.custom().withScheme((Scheme) null).toHost(this.host).atPath(this.path).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullScheme2()
   {
      UriProducer.custom().withScheme((Producer<Scheme>) null).toHost(this.host).atPath(this.path).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullHost()
   {
      UriProducer.custom().atPath(this.path).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullHost2()
   {
      UriProducer.custom().toHost((String) null).atPath(this.path).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullHost3()
   {
      UriProducer.custom().toHost((Producer<String>) null).atPath(this.path).build();
   }

   @Test
   public void testNullPort()
   {
      // can set port to null, it gets ignored when assembling url in produce
      UriProducer.custom().toHost(this.host).onPort((Producer<Integer>) null).atPath(this.path).build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativePort()
   {
      UriProducer.custom().toHost(this.host).onPort(-1).atPath(this.path).build();
   }

   @Test
   public void testZeroPort()
   {
      UriProducer.custom().toHost(this.host).onPort(0).atPath(this.path).build();
   }

   @Test
   public void testPositivePort()
   {
      UriProducer.custom().toHost(this.host).onPort(1).atPath(this.path).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullPath()
   {
      UriProducer.custom().toHost(this.host).build();
   }

   @Test(expected = NullPointerException.class)
   public void testNullPath2()
   {
      UriProducer.custom().toHost(this.host).atPath(null).build();
   }

   @Test(expected = NullPointerException.class)
   public void testQueryParamsNullKey()
   {
      UriProducer.custom().withQueryParameter(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testQueryParamsNullValue()
   {
      UriProducer.custom().withQueryParameter("key", null);
   }

   @Test
   public void testUriProducer()
   {
      final Producer<URI> p = UriProducer.custom().toHost(this.host).atPath(this.path).build();
      final URI uri = p.produce();
      Assert.assertEquals(Scheme.HTTP, Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)));
      Assert.assertEquals(this.host, uri.getHost());
      Assert.assertEquals(-1, uri.getPort());
      Assert.assertEquals("/" + this.container, uri.getPath());
      Assert.assertNull(uri.getQuery());
   }

   @Test
   public void testScheme()
   {
      final Producer<URI> p =
            UriProducer.custom().withScheme(Scheme.HTTP).toHost(this.host).atPath(this.path).build();
      final URI uri = p.produce();
      Assert.assertEquals(Scheme.HTTP, Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)));
   }

   @Test
   public void testScheme2()
   {
      final Producer<URI> p =
            UriProducer.custom().withScheme(Producers.of(Scheme.HTTPS)).toHost(this.host).atPath(
                  this.path).build();
      final URI uri = p.produce();
      Assert.assertEquals(Scheme.HTTPS, Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US)));
   }

   @Test
   public void testHost()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(Producers.of("10.1.1.1")).atPath(this.path).build();
      final URI uri = p.produce();
      Assert.assertEquals("10.1.1.1", uri.getHost());
   }

   @Test
   public void testPort()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(this.path).onPort(80).build();
      final URI uri = p.produce();
      Assert.assertEquals(80, uri.getPort());
   }

   @Test
   public void testPort2()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(this.path).onPort(Producers.of(8080)).build();
      final URI uri = p.produce();
      Assert.assertEquals(8080, uri.getPort());
   }

   @Test
   public void testPath()
   {
      final List<Producer<String>> aPath = new ArrayList<Producer<String>>();
      aPath.add(Producers.of("container"));
      aPath.add(Producers.of("object"));
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(aPath).build();
      final URI uri = p.produce();
      Assert.assertEquals("/container/object", uri.getPath());
   }

   @Test
   public void testTrailingSlash()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(this.path).withTrailingSlash().build();
      final URI uri = p.produce();
      Assert.assertEquals("/container/", uri.getPath());
   }

   @Test
   public void testQueryParameters()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(this.path).withQueryParameter("key",
                  "value").build();
      final URI uri = p.produce();
      Assert.assertEquals("key=value", uri.getQuery());
   }

   @Test
   public void testQueryParameters2()
   {
      final Producer<URI> p =
            UriProducer.custom().toHost(this.host).atPath(this.path).withQueryParameter("key",
                  "value").withQueryParameter("key2", "value2").build();
      final URI uri = p.produce();
      Assert.assertEquals("key=value&key2=value2", uri.getQuery());
   }

   @Test(expected = ProducerException.class)
   public void testBadUri()
   {
      final List<Producer<String>> badPath = new ArrayList<Producer<String>>();
      badPath.add(Producers.of("containe\r"));
      final Producer<URI> p = UriProducer.custom().toHost(this.host).atPath(badPath).build();
      p.produce();
   }
}
