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
// Date: Jul 9, 2014
// ---------------------

package com.cleversafe.og.s3;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Request;
import com.google.common.collect.Maps;

public class AWSAuthV2Test
{
   private Request request;
   private AWSAuthV2 auth;

   @Before
   public void before()
   {
      this.request = mock(Request.class);
      this.auth = new AWSAuthV2();
   }

   @Test
   public void testCanonicalizedAmzHeadersNoHeaders()
   {
      final Iterator<Entry<String, String>> it = Collections.emptyIterator();
      when(this.request.headers()).thenReturn(it);
      Assert.assertEquals("", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersNoAmzHeaders()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("Foo", "Bar");
      headers.put("Baz", "test");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersSingleAmzHeader()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-amz-foo", "bar");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-foo:bar\n", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersSingleAmzHeaderCapitalizedKey()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-AmZ-Foo", "bar");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-foo:bar\n", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersSingleAmzHeaderCapitalizedValue()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-amz-foo", "Bar");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-foo:Bar\n", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersSingleAmzHeaderKeySpaces()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put(" x-amz-foo ", "bar");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-foo:bar\n", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersSingleAmzHeaderValueSpaces()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-amz-foo", " bar ");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-foo:bar\n", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersXAmzDate()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-amz-date", "datexyz");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("", this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedAmzHeadersMultipleAmzHeaders()
   {
      final Map<String, String> headers = Maps.newHashMap();
      headers.put("x-amz-date", "datexyz");
      headers.put("x-AMz-foo", "foo");
      headers.put("x-amz-Baz", " baz  ");
      headers.put(" x-amz-bar  ", "bar");
      headers.put("Date", "datexyz");
      headers.put("Content-Length", "1024");
      when(this.request.headers()).thenReturn(headers.entrySet().iterator());
      Assert.assertEquals("x-amz-bar:bar\nx-amz-baz:baz\nx-amz-foo:foo\n",
            this.auth.canonicalizedAmzHeaders(this.request));
   }

   @Test
   public void testCanonicalizedResourceNoQuery() throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(new URI("http://192.168.1.1/container/object"));
      Assert.assertEquals("/container/object", this.auth.canonicalizedResource(this.request));
   }

   @Test
   public void testCanonicalizedResourceNoSubresources() throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(
            new URI("http://192.168.1.1/container/object?foo=bar&baz=test"));
      Assert.assertEquals("/container/object", this.auth.canonicalizedResource(this.request));
   }

   @Test
   public void testCanonicalizedResourceSubresource() throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(new URI("http://192.168.1.1/container/object?torrent"));
      Assert.assertEquals("/container/object?torrent",
            this.auth.canonicalizedResource(this.request));
   }

   @Test
   public void testCanonicalizedResourceSubresourceWithValue() throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(
            new URI("http://192.168.1.1/container/object?uploadId=1"));
      Assert.assertEquals("/container/object?uploadId=1",
            this.auth.canonicalizedResource(this.request));
   }

   @Test
   public void testCanonicalizedResourceMultipleSubresources() throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(
            new URI("http://192.168.1.1/container/object?uploadId=1&torrent&foo=bar&website"));
      // subresources should be sorted
      Assert.assertEquals("/container/object?torrent&uploadId=1&website",
            this.auth.canonicalizedResource(this.request));
   }

   @Test
   public void testSplitQueryParametersNullQuery()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters(null);
      Assert.assertEquals(0, queryParams.size());
   }

   @Test
   public void testSplitQueryParametersEmptyQuery()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters("");
      Assert.assertEquals(0, queryParams.size());
   }

   @Test
   public void testSplitQueryParametersSingleQueryParameterNoValue()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters("torrent");
      Assert.assertEquals(1, queryParams.size());
      Assert.assertNull(queryParams.get("torrent"));
   }

   @Test
   public void testSplitQueryParametersSingleQueryParameterEmptyValue()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters("uploadId=");
      Assert.assertEquals(1, queryParams.size());
      Assert.assertEquals("", queryParams.get("uploadId"));
   }

   @Test
   public void testSplitQueryParametersSingleQueryParameterWithValue()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters("uploadId=1");
      Assert.assertEquals(1, queryParams.size());
      Assert.assertEquals("1", queryParams.get("uploadId"));
   }

   @Test
   public void testSplitQueryParametersMultipleQueryParameters()
   {
      final Map<String, String> queryParams = this.auth.splitQueryParameters("uploadId=1&torrent");
      Assert.assertEquals(2, queryParams.size());
      Assert.assertEquals("1", queryParams.get("uploadId"));
      Assert.assertNull("1", queryParams.get("torrent"));
   }

   @Test
   public void testJoinQueryParametersNullMap()
   {
      final String query = this.auth.joinQueryParameters(null);
      Assert.assertEquals("", query);
   }

   @Test
   public void testJoinQueryParametersEmptyMap()
   {
      final Map<String, String> queryParameters = Collections.emptyMap();
      final String query = this.auth.joinQueryParameters(queryParameters);
      Assert.assertEquals("", query);
   }

   @Test
   public void testJoinQueryParametersSingleQueryParameterNoValue()
   {
      final Map<String, String> queryParameters = Maps.newHashMap();
      queryParameters.put("torrent", null);
      final String query = this.auth.joinQueryParameters(queryParameters);
      Assert.assertEquals("torrent", query);
   }

   @Test
   public void testJoinQueryParametersSingleQueryParameterWithValue()
   {
      final Map<String, String> queryParameters = Maps.newHashMap();
      queryParameters.put("uploadId", "1");
      final String query = this.auth.joinQueryParameters(queryParameters);
      Assert.assertEquals("uploadId=1", query);
   }

   @Test
   public void testJoinQueryParametersMultipleQueryParameters()
   {
      final Map<String, String> queryParameters = Maps.newTreeMap();
      queryParameters.put("uploadId", "1");
      queryParameters.put("torrent", null);
      final String query = this.auth.joinQueryParameters(queryParameters);
      Assert.assertEquals("torrent&uploadId=1", query);
   }
}
