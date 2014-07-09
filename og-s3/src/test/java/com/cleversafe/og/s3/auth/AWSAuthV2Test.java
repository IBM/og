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

package com.cleversafe.og.s3.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Request;

public class AWSAuthV2Test
{
   private Request request;
   private AWSAuthV2 auth;

   @Before
   public void setBefore()
   {
      this.request = mock(Request.class);
      this.auth = new AWSAuthV2();
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
}
